package in.dc297.mqttclpro;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.jar.Manifest;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.dinglisch.android.tasker.TaskerPlugin;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.SSLSocketFactory;

import in.dc297.mqttclpro.SSL.SSLUtil;
import in.dc297.mqttclpro.tasker.Constants;
import in.dc297.mqttclpro.tasker.PluginBundleManager;
import in.dc297.mqttclpro.tasker.PublishTaskerActivity;

import static in.dc297.mqttclpro.tasker.Constants.LOG_TAG;

/*
 * An example of how to implement an MQTT client in Android, able to receive
 *  push notifications from an MQTT message broker server.
 *
 *  Dale Lane (dale.lane@gmail.com)
 *    28 Jan 2011
 */
public class MQTTService extends Service implements MqttCallback
{
    /************************************************************************/
    /*    CONSTANTS                                                         */
    /************************************************************************/

    // something unique to identify your app - used for stuff like accessing
    //   application preferences
    public static final String APP_ID = "in.dc297.mqttclpro";

    // constants used to notify the Activity UI of received messages
    public static final String MQTT_MSG_RECEIVED_INTENT = "in.dc297.mqttclpro.MSGRECVD";
    public static final String MQTT_MSG_RECEIVED_TOPIC  = "in.dc297.mqttclpro.MSGRECVD_TOPIC";
    public static final String MQTT_MSG_RECEIVED_MSG    = "in.dc297.mqttclpro.MSGRECVD_MSGBODY";

    // constants used to tell the Activity UI the connection status
    public static final String MQTT_STATUS_INTENT = "in.dc297.mqttclpro.STATUS";
    public static final String MQTT_STATUS_MSG    = "in.dc297.mqttclpro.STATUS_MSG";

    // constant used internally to schedule the next ping event
    public static final String MQTT_PING_ACTION = "in.dc297.mqttclpro.PING";
    public static final String MQTT_RECONNECT_ACTION = "in.dc297.mqttclpro.RECONNECT";

    public static final String MQTT_PUBLISH = "in.dc297.mqttclpro.PUBLISH";

    // constants used by status bar notifications
    public static final int MQTT_NOTIFICATION_ONGOING = 1;
    public static final int MQTT_NOTIFICATION_UPDATE  = 2;

    private ConnectAsyncTask connectTask = null;
    private SubscribeAsyncTask subTask = null;

    protected static final Intent INTENT_REQUEST_REQUERY =
            new Intent(in.dc297.mqttclpro.tasker.Intent.ACTION_REQUEST_QUERY).putExtra(in.dc297.mqttclpro.tasker.Intent.EXTRA_ACTIVITY,
                    PublishTaskerActivity.class.getName());

    @Override
    public void connectionLost(Throwable cause) {
        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqttserv-connlost");
        wl.acquire();


        //
        // have we lost our data connection?
        //

        if (isOnline() == false)
        {
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;

            // inform the app that we are not connected any more
            broadcastServiceStatus("Connection lost - no network connection");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we are no longer able to receive messages
            notifyUser("Connection lost - no network connection",
                    LOG_TAG, "Connection lost - no network connection");

            //
            // wait until the phone has a network connection again, when we
            //  the network connection receiver will fire, and attempt another
            //  connection to the broker
        }
        else
        {
            //
            // we are still online
            //   the most likely reason for this connectionLost is that we've
            //   switched from wifi to cell, or vice versa
            //   so we try to reconnect immediately
            //

            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

            // inform the app that we are not connected any more, and are
            //   attempting to reconnect
            broadcastServiceStatus("Connection lost - reconnecting...");

            // try to reconnect
            handleStart();
        }

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqttserv-messagearrived");
        wl.acquire();

        //
        //  I'm assuming that all messages I receive are being sent as strings
        //   this is not an MQTT thing - just me making as assumption about what
        //   data I will be receiving - your app doesn't have to send/receive
        //   strings - anything that can be sent as bytes is valid
        String messageBody = new String(message.getPayload());

        //
        //  for times when the app's Activity UI is not running, the Service
        //   will need to safely store the data that it receives
        if (addReceivedMessageToStore(topic, messageBody,message.getQos()))
        {
            // this is a new message - a value we haven't seen before

            //
            // inform the app (for times when the Activity UI is running) of the
            //   received message so the app UI can be updated with the new data
            broadcastReceivedMessage(topic, messageBody);

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that there is new data available
            notifyUser("New data received", topic, messageBody);

            //tasker stuff starts
            Bundle publishedBundle = PluginBundleManager.generateBundle(getApplicationContext(),messageBody,topic);
            TaskerPlugin.Event.addPassThroughMessageID( INTENT_REQUEST_REQUERY );
            TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY,publishedBundle);
            sendBroadcast(INTENT_REQUEST_REQUERY);
            //tasker stuff ends
        }

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    // constants used to define MQTT connection status
    public enum MQTTConnectionStatus
    {
        INITIAL,                            // initial status
        CONNECTING,                         // attempting to connect
        CONNECTED,                          // connected
        NOTCONNECTED_WAITINGFORINTERNET,    // can't connect because the phone
        //     does not have Internet access
        NOTCONNECTED_USERDISCONNECT,        // user has explicitly requested
        //     disconnection
        NOTCONNECTED_DATADISABLED,          // can't connect because the user
        //     has disabled data access
        NOTCONNECTED_UNKNOWNREASON,          // failed to connect for some reason
        FIRST_RUN
    }

    // MQTT constants
    public static final int MAX_MQTT_CLIENTID_LENGTH = 22;

    /************************************************************************/
    /*    VARIABLES used to maintain state                                  */
    /************************************************************************/

    // status of MQTT client connection
    private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;


    /************************************************************************/
    /*    VARIABLES used to configure MQTT connection                       */
    /************************************************************************/

    // taken from preferences
    //    host name of the server we're receiving push notifications from
    private String          brokerHostName       = "";


    // defaults - this sample uses very basic defaults for it's interactions
    //   with message brokers
    private int             brokerPortNumber     = 1883;
    private String          userName             = "";
    private String          password             = "";
    private boolean         ssl                  = false;
    private boolean         cleanSession         = false;
    private MqttClientPersistence usePersistence       = null;

    private String  lastwill_topic = "",
                    lastwill_message = "";
    private int     lastwill_qos = 0;
    private boolean lastwill_retained = false;


    private DBHelper db = null;

    private SharedPreferences settings = null;
    //  how often should the app ping the server to keep the connection alive?
    //
    //   too frequently - and you waste battery life
    //   too infrequently - and you wont notice if you lose your connection
    //                       until the next unsuccessfull attempt to ping
    //
    //   it's a trade-off between how time-sensitive the data is that your
    //      app is handling, vs the acceptable impact on battery life
    //
    //   it is perhaps also worth bearing in mind the network's support for
    //     long running, idle connections. Ideally, to keep a connection open
    //     you want to use a keep alive value that is less than the period of
    //     time after which a network operator will kill an idle connection
    private short           keepAliveSeconds     = 20 * 60;


    // This is how the Android client app will identify itself to the
    //  message broker.
    // It has to be unique to the broker - two clients are not permitted to
    //  connect to the same broker using the same client ID.
    private String          mqttClientId = null;

    private Reconnector reconnector;



    /************************************************************************/
    /*    VARIABLES  - other local variables                                */
    /************************************************************************/
    // connection to the message broker
    private MqttClient mqttClient = null;

    // receiver that notifies the Service when the phone gets data connection
    private NetworkConnectionIntentReceiver netConnReceiver;

    // receiver that notifies the Service when the user changes data use preferences
    //private BackgroundDataChangeIntentReceiver dataEnabledReceiver;
    private Pinger pinger;
    private FireTaskerReceiver taskerFireReceiver;

    private ArrayList<String> prefs_key = new ArrayList<>(Arrays.asList("url","port","keepalive","user",
            "password","cleansession","ssl_switch", "lastwill_topic", "lastwill_message", "lastwill_qos", "lastwill_retained","clientid"));
    //listener for shared preferences to reconnect if user changes server settings
    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            // Implementation
            //if(prefs.)
            if(prefs_key.contains(key)) {
                if (mqttClient != null) {
                    if (mqttClient.isConnected()) {
                        try {
                            mqttClient.disconnect();
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    mqttClient = null;
                }
                if(connectTask!=null){
                    connectTask.cancel(true);
                    connectTask=null;
                }
                defineConnectionToBroker();
                handleStart();
            }
        }
    };
    /************************************************************************/
    /*    METHODS - core Service lifecycle methods                          */
    /************************************************************************/

    // see http://developer.android.com/guide/topics/fundamentals.html#lcycles
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(LOG_TAG,"starting service");
        // reset status variable to initial state
        connectionStatus = MQTTConnectionStatus.INITIAL;
        db = new DBHelper(getApplicationContext());
        // create a binder that will let the Activity UI send
        //   commands to the Service
        mBinder = new LocalBinder<MQTTService>(this);


        // get the broker settings out of app preferences
        //   this is not the only way to do this - for example, you could use
        //   the Intent that starts the Service to pass on configuration values
        // register to be notified whenever the user changes their preferences
        //  relating to background data use - so that we can respect the current
        //  preference
  //      dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
        //registerReceiver(dataEnabledReceiver,
//                new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));

        reconnector = new Reconnector();
        registerReceiver(reconnector,new IntentFilter(MQTT_RECONNECT_ACTION));

        taskerFireReceiver = new FireTaskerReceiver();

        registerReceiver(taskerFireReceiver,new IntentFilter(MQTT_PUBLISH));

        pinger = new Pinger();
        registerReceiver(pinger,new IntentFilter(MQTT_PING_ACTION));

        // define the connection to the broker
        defineConnectionToBroker();
    }


    @Override
    public void onStart(final Intent intent, final int startId)
    {
        // This is the old onStart method that will be called on the pre-2.0
        // platform.  On 2.0 or later we override onStartCommand() so this
        // method will not be called.

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart();
            }
        }, "MQTTservice").start();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart();
            }
        }, "MQTTservice").start();

        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        return START_STICKY;
    }

    synchronized void handleStart()
    {
        // before we start - check for a couple of reasons why we should stop

        if (mqttClient == null)
        {
            // we were unable to define the MQTT client connection, so we stop
            //  immediately - there is nothing that we can do
            stopSelf();
            return;
        }

        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getBackgroundDataSetting()) // respect the user's request not to use data!
        {
            // user has disabled background data
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;

            // update the app to show that the connection has been disabled
            broadcastServiceStatus("Not connected - background data disabled");
            //scheduleNextConnect(); we wont schedule next connect if bg data is disabled
            // we have a listener running that will notify us when this
            //   preference changes, and will call handleStart again when it
            //   is - letting us pick up where we leave off now
            return;
        }

        // the Activity UI has started the MQTT service - this may be starting
        //  the Service new for the first time, or after the Service has been
        //  running for some time (multiple calls to startService don't start
        //  multiple Services, but it does call this method multiple times)
        // if we have been running already, we re-send any stored data
        rebroadcastStatus();
        rebroadcastReceivedMessages();

        // if the Service was already running and we're already connected - we
        //   don't need to do anything
        if (isAlreadyConnected() == false)
        {
            // set the status to show we're trying to connect
            connectionStatus = MQTTConnectionStatus.CONNECTING;

            // we are creating a background service that will run forever until
            //  the user explicity stops it. so - in case they start needing
            //  to save battery life - we should ensure that they don't forget
            //  we're running, by leaving an ongoing notification in the status
            //  bar while we are running
            /*NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.icon,
                    LOG_TAG,
                    System.currentTimeMillis());
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            Intent notificationIntent = new Intent(this, MQTTNotifier.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setLatestEventInfo(this, LOG_TAG, "MQTT Service is running", contentIntent);
            nm.notify(MQTT_NOTIFICATION_ONGOING, notification);
            */


            // before we attempt to connect - we check if the phone has a
            //  working data connection
            if (isOnline())
            {
                // we think we have an Internet connection, so try to connect
                //  to the message broker
                connectToBroker();
            }
            else
            {
                // we can't do anything now because we don't have a working
                //  data connection
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;

                // inform the app that we are not connected
                broadcastServiceStatus("Waiting for network connection");
                //scheduleNextConnect(); we wont schedule next connect if we are not connected to internet
            }
        }

        // changes to the phone's network - such as bouncing between WiFi
        //  and mobile data networks - can break the MQTT connection
        // the MQTT connectionLost can be a bit slow to notice, so we use
        //  Android's inbuilt notification system to be informed of
        //  network changes - so we can reconnect immediately, without
        //  haing to wait for the MQTT timeout
        if (netConnReceiver == null)
        {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            getApplicationContext().registerReceiver(netConnReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        db.close();
        // disconnect immediately
        disconnectFromBroker();

        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");

        // try not to leak the listener
        //if (dataEnabledReceiver != null)
        //{
        //    unregisterReceiver(dataEnabledReceiver);
        //    dataEnabledReceiver = null;
        //}

        if(reconnector!=null){
            unregisterReceiver(reconnector);
            reconnector = null;
        }

        if(taskerFireReceiver!=null){
            unregisterReceiver(taskerFireReceiver);
            taskerFireReceiver = null;
        }

        if(pinger!=null){
            unregisterReceiver(pinger);
            pinger = null;
        }

        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }
        if(settings!=null) {
            settings.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }


    /************************************************************************/
    /*    METHODS - broadcasts and notifications                            */
    /************************************************************************/

    // methods used to notify the Activity UI of something that has happened
    //  so that it can be updated to reflect status and the data received
    //  from the server

    private void broadcastServiceStatus(String statusDescription)
    {
        if(statusDescription.equals("Connected")){
            statusDescription = statusDescription+(mqttClient!=null ? " to "+mqttClient.getServerURI():"");
        }
        // inform the app (for times when the Activity UI is running /
        //   active) of the current MQTT connection status so that it
        //   can update the UI accordingly

        if(settings!=null){
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putString("servicestatus",statusDescription);
            settingsEditor.apply();
        }
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_STATUS_INTENT);
        broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastReceivedMessage(String topic, String message)
    {
        // pass a message received from the MQTT server on to the Activity UI
        //   (for times when it is running / active) so that it can be displayed
        //   in the app GUI
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG,   message);
        sendBroadcast(broadcastIntent);
    }

    // methods used to notify the user of what has happened for times when
    //  the app Activity UI isn't running
    private void notifyUser(String alert, String title, String body)
    {/*
        try {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .setContentTitle(alert+" "+title)
                        .setContentText(body)
                        .setAutoCancel(true);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, SubscribeActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(SubscribeActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        mNotificationManager.notify(1, mBuilder.build());
        }
        catch(Exception e){
            e.printStackTrace();
            Log.e(LOG_TAG,"Error during notify",e);
        }*/
    }


    /************************************************************************/
    /*    METHODS - binding that allows access from the Actitivy            */
    /************************************************************************/

    // trying to do local binding while minimizing leaks - code thanks to
    //   Geoff Bruckner - which I found at
    //   http://groups.google.com/group/cw-android/browse_thread/thread/d026cfa71e48039b/c3b41c728fedd0e7?show_docid=c3b41c728fedd0e7

    private LocalBinder<MQTTService> mBinder;

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
    public class LocalBinder<S> extends Binder
    {
        private WeakReference<S> mService;

        public LocalBinder(S service)
        {
            mService = new WeakReference<S>(service);
        }
        public S getService()
        {
            return mService.get();
        }
        public void close()
        {
            mService = null;
        }
    }

    //
    // public methods that can be used by Activities that bind to the Service
    //

    public MQTTConnectionStatus getConnectionStatus()
    {
        return connectionStatus;
    }

    public void rebroadcastStatus()
    {
        String status = "";

        switch (connectionStatus)
        {
            case INITIAL:
                status = "Please wait";
                break;
            case CONNECTING:
                status = "Connecting...";
                break;
            case CONNECTED:
                status = "Connected";
                break;
            case NOTCONNECTED_UNKNOWNREASON:
                status = "Not connected - waiting for network connection";
                break;
            case NOTCONNECTED_USERDISCONNECT:
                status = "Disconnected";
                break;
            case NOTCONNECTED_DATADISABLED:
                status = "Not connected - background data disabled";
                break;
            case NOTCONNECTED_WAITINGFORINTERNET:
                status = "Unable to connect";
                break;
            case FIRST_RUN:
                status = "Please define broker details";
                break;
        }

        //
        // inform the app that the Service has successfully connected
        broadcastServiceStatus(status);
    }

    public void disconnect()
    {
        disconnectFromBroker();

        // set status
        connectionStatus = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;

        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");
    }

    public void publishMessage(String topic,String message,String qos,long message_id,boolean retained){
        PublishAsyncTask pubTask = new PublishAsyncTask();
        pubTask.execute(topic,qos,message,message_id,retained);
    }


    /************************************************************************/
    /*    METHODS - wrappers for some of the MQTT methods that we use       */
    /************************************************************************/

    /*
     * Create a client connection object that defines our connection to a
     *   message broker server
     */
    private void defineConnectionToBroker()
    {
        settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settings.registerOnSharedPreferenceChangeListener(listener);
        brokerHostName = settings.getString("url", "");
        brokerPortNumber = Integer.parseInt(settings.getString("port", "1883"));
        userName = settings.getString("user", "");
        password = settings.getString("password","");
        ssl = settings.getBoolean("ssl_switch",false);
        keepAliveSeconds = Short.parseShort(settings.getString("keepalive","1200"));
        cleanSession = settings.getBoolean("cleansession",false);
        String genClientId = generateClientId();
        mqttClientId = settings.getString("clientid",genClientId);
        if("".equals(mqttClientId)) {
            mqttClientId = genClientId;
        }
        Log.i("mqttserv", "Client ID: " + mqttClientId);
        lastwill_topic = settings.getString("lastwill_topic","");
        lastwill_message = settings.getString("lastwill_message","");
        try{
            lastwill_qos = Integer.parseInt(settings.getString("lastwill_qos","0"));
        }
        catch(Exception e){
            lastwill_qos = 0;
        }
        lastwill_retained = settings.getBoolean("lastwill_retained",false);

        String protocol = "tcp";
        if(ssl){
            protocol = "ssl";
        }
        String mqttConnSpec = protocol+"://" + brokerHostName + ":" + brokerPortNumber;

        try
        {
            usePersistence = new MemoryPersistence();
            // define the connection to the broker
            mqttClient = new MqttClient(mqttConnSpec,mqttClientId,usePersistence);
            mqttClient.setCallback(this);
            // register this client app has being able to receive messages
            //mqttClient.registerSimpleHandler(this);
        }
        catch (MqttException e)
        {
            // something went wrong!
            mqttClient = null;
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

            //
            // inform the app that we failed to connect so that it can update
            //  the UI accordingly
            broadcastServiceStatus("Invalid connection parameters");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we failed to connect
            notifyUser("Unable to connect", LOG_TAG, "Unable to connect");
            scheduleNextConnect();
        }
    }
    private void scheduleNextConnect()
    {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_RECONNECT_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // in case it takes us a little while to do this, we try and do it
        //  shortly before the keep alive period expires
        // it means we're pinging slightly more frequently than necessary
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);
        broadcastServiceStatus("Failed to connect. Next connect scheduled at "+wakeUpTime.getTime());
        Log.i(LOG_TAG,"Scheduling next connect at "+wakeUpTime.getTime());

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);
    }

    private void scheduleNextPing()
    {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // in case it takes us a little while to do this, we try and do it
        //  shortly before the keep alive period expires
        // it means we're pinging slightly more frequently than necessary
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);
        Log.i(LOG_TAG,"scheduling next ping for "+wakeUpTime.getTime());
        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);
    }

    /*
     * (Re-)connect to the message broker
     */
    private void connectToBroker()
    {
        if(connectTask==null){
            connectTask = new ConnectAsyncTask();
            connectTask.execute();
        }
        else if(connectTask.getStatus()== AsyncTask.Status.FINISHED){
            connectTask = new ConnectAsyncTask();
            connectTask.execute();
        }
    }

    /*
     * Send a request to the message broker to be sent messages published with
     *  the specified topic name. Wildcards are allowed.
     */
    private void subscribeToTopics()
    {
        if(subTask==null || subTask.getStatus()== AsyncTask.Status.FINISHED) {
            subTask = new SubscribeAsyncTask();
            subTask.execute();
        }
    }

    public void subscribeToTopic(String topic,int qos){
        if(subTask==null || subTask.getStatus()== AsyncTask.Status.FINISHED) {
            subTask = new SubscribeAsyncTask();
            subTask.execute(topic,qos);
        }
    }

    public void unsubscribeFromTopic(String topic){
        UnsubscribeAsyncTask unsubTask = new UnsubscribeAsyncTask();
        unsubTask.execute(topic);
    }

    /*
     * Terminates a connection to the message broker.
     */
    private void disconnectFromBroker()
    {
        Log.i(LOG_TAG,"Disconnecting from broker");
        // if we've been waiting for an Internet connection, this can be
        //  cancelled - we don't need to be told when we're connected now
        try
        {
            if(reconnector!=null){
                unregisterReceiver(reconnector);
                reconnector = null;
            }
            if(taskerFireReceiver!=null){
                unregisterReceiver(taskerFireReceiver);
                taskerFireReceiver = null;
            }
            if(pinger!=null){
                unregisterReceiver(pinger);
                pinger = null;
            }
        }
        catch (Exception eee)
        {
            // probably because we hadn't registered it
            Log.e(LOG_TAG, "unregister failed", eee);
        }

        try
        {
            if (mqttClient != null)
            {
                mqttClient.disconnect();
            }
        }
        catch (MqttPersistenceException e)
        {
            Log.e(LOG_TAG, "disconnect failed - persistence exception", e);
        } catch (MqttException e) {
            Log.e(LOG_TAG, "disconnect failed - mqtt exception", e);
            //e.printStackTrace();
        } finally
        {
            mqttClient = null;
        }

        // we can now remove the ongoing notification that warns users that
        //  there was a long-running ongoing service running
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    /*
     * Checks if the MQTT client thinks it has an active connection
     */
    private boolean isAlreadyConnected()
    {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }

    /*private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context ctx, Intent intent)
        {
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqttserv-bgdatachanged");
            wl.acquire();

            ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if (cm.getBackgroundDataSetting())
            {
                // user has allowed background data - we start again - picking
                //  up where we left off in handleStart before
                handleStart();
            }
            else
            {
                // user has disabled background data
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;

                // update the app to show that the connection has been disabled
                broadcastServiceStatus("Not connected - background data disabled");

                // disconnect from the broker
                disconnectFromBroker();
            }

            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }*/


   /************************************************************************/
    /*   APP SPECIFIC - stuff that would vary for different uses of MQTT    */
    /************************************************************************/

    //  apps that handle very small amounts of data - e.g. updates and
    //   notifications that don't need to be persisted if the app / phone
    //   is restarted etc. may find it acceptable to store this data in a
    //   variable in the Service
    //  that's what I'm doing in this sample: storing it in a local hashtable
    //  if you are handling larger amounts of data, and/or need the data to
    //   be persisted even if the app and/or phone is restarted, then
    //   you need to store the data somewhere safely
    //  see http://developer.android.com/guide/topics/data/data-storage.html
    //   for your storage options - the best choice depends on your needs

    // stored internally

    private Hashtable<String, String> dataCache = new Hashtable<String, String>();

    private boolean addReceivedMessageToStore(String key, String value,int qos)
    {
        Log.i(LOG_TAG,"adding to db");
        if(db.addMessage(key,value,0,qos)!=0){
            return true;
        }
        return false;

        /*String previousValue = null;

        if (value.length() == 0)
        {
            previousValue = dataCache.remove(key);
        }
        else
        {
            previousValue = dataCache.put(key, value);
        }

        // is this a new value? or am I receiving something I already knew?
        //  we return true if this is something new
        return ((previousValue == null) ||
                (previousValue.equals(value) == false));*/
    }

    // provide a public interface, so Activities that bind to the Service can
    //  request access to previously received messages

    public void rebroadcastReceivedMessages()
    {
        Enumeration<String> e = dataCache.keys();
        while(e.hasMoreElements())
        {
            String nextKey = e.nextElement();
            String nextValue = dataCache.get(nextKey);

            broadcastReceivedMessage(nextKey, nextValue);
        }
    }


    /************************************************************************/
    /*    METHODS - internal utility methods                                */
    /************************************************************************/

    private String generateClientId()
    {
        // generate a unique client id if we haven't done so before, otherwise
        //   re-use the one we already have

        if (mqttClientId == null)
        {
            // generate a unique client ID - I'm basing this on a combination of
            //  the phone device id and the current timestamp
            String timestamp = "" + (new Date()).getTime();
            String android_id = Settings.System.getString(getContentResolver(),
                    Secure.ANDROID_ID);
            mqttClientId = timestamp + android_id;

            // truncate - MQTT spec doesn't allow client ids longer than 23 chars
            if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
                mqttClientId = mqttClientId.substring(0, MAX_MQTT_CLIENTID_LENGTH);
            }
        }

        return mqttClientId;
    }

    private boolean isOnline()
    {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if(cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected())
        {
            return true;
        }

        return false;
    }

    private class ConnectAsyncTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            try
            {
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(cleanSession);
                connOpts.setKeepAliveInterval(keepAliveSeconds);
                if(userName!=null & !userName.equals("")){
                    connOpts.setUserName(userName);
                    connOpts.setPassword(password != null ? password.toCharArray() : "".toCharArray());
                }
                try{
                    if(lastwill_topic!=null) {
                        MqttTopic.validate(lastwill_topic, false);
                        connOpts.setWill(lastwill_topic,lastwill_message.getBytes(),lastwill_qos,lastwill_retained);
                    }
                }
                catch(Exception e){

                }
                // try to connect
                if(mqttClient==null){
                    defineConnectionToBroker();
                }
                if("".equals(brokerHostName)){
                    broadcastServiceStatus("Please define broker details");
                    connectionStatus = MQTTConnectionStatus.FIRST_RUN;
                    return true;
                }
                broadcastServiceStatus("Connecting...");
                if(ssl){
                    connOpts.setSocketFactory(SSLUtil.getSocketFactory("/mnt/sdcard/Mosquitto/ca.crt",null,null,null));
                }
                mqttClient.connect(connOpts);
                scheduleNextPing();
                //
                // inform the app that the app has successfully connected
                broadcastServiceStatus("Connected");

                // we are connected
                connectionStatus = MQTTConnectionStatus.CONNECTED;
                Log.i(LOG_TAG,"Successfully connected");

                // we need to wake up the phone's CPU frequently enough so that the
                //  keep alive messages can be sent
                // we schedule the first one of these now
                if(mqttClient!=null) {
                    if(mqttClient.isConnected()){
                        subscribeToTopics();
                    }
                }
                return true;
            }
            catch (MqttException e)
            {
                // something went wrong!

                if(e.getReasonCode()==32100){
                    connectionStatus = MQTTConnectionStatus.CONNECTED;
                    //try {
                    //mqttClient.disconnect();
                    //connectToBroker();
                    //} catch (MqttException e1) {
                    //  e1.printStackTrace();
                    //}
                }else {
                    connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

                    Log.e(LOG_TAG, "unable to connect" + brokerHostName + ":" + brokerPortNumber + " exception is" + e.getReasonCode(), e);
                    //
                    // inform the app that we failed to connect so that it can update
                    //  the UI accordingly

                    broadcastServiceStatus("Unable to connect");

                    scheduleNextConnect();
                    //
                    // inform the user (for times when the Activity UI isn't running)
                    //   that we failed to connect
                    notifyUser("Unable to connect", LOG_TAG, "Unable to connect - will retry later");

                    // if something has failed, we wait for one keep-alive period before
                    //   trying again
                    // in a real implementation, you would probably want to keep count
                    //  of how many times you attempt this, and stop trying after a
                    //  certain number, or length of time - rather than keep trying
                    //  forever.
                    // a failure is often an intermittent network issue, however, so
                    //  some limited retry is a good idea
                }
                return false;
            }
        }
    }

    private class SubscribeAsyncTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            if(params.length==2){
                //we passed topic name to subscribe to
                Log.i(LOG_TAG,"Subscribing to "+(String)params[0]);
                if(mqttClient!=null){
                    if(mqttClient.isConnected()){
                        try {
                            mqttClient.subscribe((String)params[0],(int)params[1]);
                            scheduleNextPing();
                        } catch (MqttException e) {
                            //broadcastServiceStatus("Unable to Subscribe");
                            e.printStackTrace();
                        }
                    }
                }
            }else {
                Log.i(LOG_TAG,"Subscribing to all topics");
                //subscribe to all topics
                String[] topics;
                int[] qos;
                boolean subscribed = false;
                if (isAlreadyConnected() == false) {
                    // quick sanity check - don't try and subscribe if we
                    //  don't have a connection
                } else {
                    try {
                        Cursor topicCursor = db.getTopics(0);
                        topicCursor.moveToFirst();
                        topics = new String[topicCursor.getCount()];
                        qos = new int[topicCursor.getCount()];
                        int i = 0;
                        while (!topicCursor.isAfterLast()) {
                            topics[i] = topicCursor.getString(topicCursor.getColumnIndexOrThrow("topic"));
                            qos[i] = topicCursor.getInt(topicCursor.getColumnIndexOrThrow("qos"));
                            topicCursor.moveToNext();
                            i++;
                        }
                        Log.i(LOG_TAG, topics.toString());
                        if (topics.length != 0) {
                            mqttClient.subscribe(topics, qos);
                            scheduleNextPing();
                            subscribed = true;
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(LOG_TAG, "subscribe failed - illegal argument", e);
                    } catch (MqttException e) {
                        Log.e(LOG_TAG, "subscribe failed - MQTT exception", e);
                    }
                }
            }
            return false;
        }
    }

    private class PublishAsyncTask extends AsyncTask{

        @Override
        protected void onPostExecute(Object result){
            if((boolean)result){
                Toast.makeText(getApplicationContext(),"Unable to publish as we are not connected",Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Object doInBackground(Object[] params) {
            String topic = (String)params[0];
            String qos = (String)params[1];
            String message = (String) params[2];
            long message_id = (long)params[3];
            boolean retained = (boolean)params[4];
            Log.i(LOG_TAG,topic+" : "+message + " : "+qos + " : "+retained);
            if(mqttClient!=null) {
                if (mqttClient.isConnected()) {
                    try {
                        MqttMessage mqmessage = new MqttMessage(message.getBytes());
                        mqmessage.setQos(Integer.parseInt(qos));
                        mqmessage.setRetained(retained);
                        mqttClient.publish(topic, mqmessage);
                        scheduleNextPing();
                        db.setMessagePublished(message_id);
                    } catch (MqttException e) {
                        e.printStackTrace();
                        return true;
                    }
                }
                else{
                    return true;
                }
            }
            else{
                return true;
            }
            return false;
        }
    }

    private class UnsubscribeAsyncTask extends AsyncTask{
        @Override
        protected Object doInBackground(Object[] params) {
            String topic = (String)params[0];
            if(mqttClient!=null){
                if(mqttClient.isConnected()){
                    try {
                        mqttClient.unsubscribe(topic);
                        scheduleNextPing();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }
    }

    public class Pinger extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Note that we don't need a wake lock for this method (even though
            //  it's important that the phone doesn't switch off while we're
            //  doing this).
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            //  long as the alarm receiver's onReceive() method is executing.
            //  This guarantees that the phone will not sleep until you have
            //  finished handling the broadcast."
            // This is good enough for our needs.

            try
            {
                if(mqttClient!=null){
                    if(mqttClient.isConnected()){
                        PublishAsyncTask publishAsyncTask = new PublishAsyncTask();
                        publishAsyncTask.execute("ping","2","",0l,false);
                    }
                }
            }
            catch (Exception e)
            {
                broadcastServiceStatus("Unable to connect");
                e.printStackTrace();
                Log.e(LOG_TAG,"Unable to reconnect",e);
                scheduleNextConnect();
            }
        }
    }

    public class Reconnector extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Note that we don't need a wake lock for this method (even though
            //  it's important that the phone doesn't switch off while we're
            //  doing this).
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            //  long as the alarm receiver's onReceive() method is executing.
            //  This guarantees that the phone will not sleep until you have
            //  finished handling the broadcast."
            // This is good enough for our needs.

            try
            {
                if(mqttClient==null){
                    defineConnectionToBroker();
                }
                if(!mqttClient.isConnected()){
                    handleStart();
                }
            }
            catch (Exception e)
            {
                broadcastServiceStatus("Unable to connect");
                e.printStackTrace();
                Log.e(LOG_TAG,"Unable to reconnect",e);
                scheduleNextConnect();
            }
        }
    }
    public final class FireTaskerReceiver extends BroadcastReceiver
    {
        public FireTaskerReceiver() {
        }

        @Override
        public void onReceive(final Context context, final Intent intent)
        {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

            if (!MQTT_PUBLISH.equals(intent.getAction()))
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.e(Constants.LOG_TAG,
                            String.format(Locale.US, "Received unexpected Intent action %s %s", intent.getAction(),MQTT_PUBLISH)); //$NON-NLS-1$
                }
                return;
            }
            //BundleScrubber.scrub(intent);

            //final Bundle bundle = intent.getBundleExtra(in.dc297.mqttclpro.tasker.Intent.EXTRA_BUNDLE);
            //BundleScrubber.scrub(bundle);

        /*if (PluginBundleManager.isBundleValid(bundle))
        {
            final String message = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }*/
        }
    }
    /*
    * Called in response to a change in network connection - after losing a
    *  connection to the server, this allows us to wait until we have a usable
    *  data connection again
    */
    public class NetworkConnectionIntentReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context ctx, Intent intent)
        {
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            Log.i(LOG_TAG,"change in network");
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqttserv-networkconnchange");
            wl.acquire();
            ConnectivityManager cm =
                    (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork!=null) Log.i(LOG_TAG,activeNetwork.toString());
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            // we have an internet connection - have another try at connecting
            if(isConnected) {
                defineConnectionToBroker();
                handleStart();
                Log.i(LOG_TAG,String.valueOf(isConnected));
            }
            else{
                disconnectFromBroker();
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
                broadcastServiceStatus("Waiting for Connection");
            }


            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }
}