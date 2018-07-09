package in.dc297.mqttclpro.mqtt.internal;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.sql.Timestamp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.net.SocketFactory;

import in.dc297.mqttclpro.SSL.SSLUtil;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.entity.Message;
import in.dc297.mqttclpro.entity.MessageEntity;
import in.dc297.mqttclpro.entity.TopicEntity;
import in.dc297.mqttclpro.tasker.PluginBundleManager;
import in.dc297.mqttclpro.tasker.activity.ConfigureTaskerEventActivity;
import in.dc297.mqttclpro.tasker.activity.ConnectionLostConfigActivity;
import in.dc297.mqttclpro.tasker.activity.ReconnectConfigActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.sql.RowCountException;
import tasker.TaskerPlugin;

/**
 * Created by Deepesh on 10/25/2017.
 */

public class MQTTClients {

    /**
     * Singleton instance of <code>MQTTClients</code>
     */
    private static MQTTClients instance = null;

    protected static final Intent INTENT_REQUEST_REQUERY =
            new Intent(in.dc297.mqttclpro.tasker.activity.Intent.ACTION_REQUEST_QUERY).putExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_ACTIVITY,
                    ConfigureTaskerEventActivity.class.getName());

    protected static final Intent INTENT_REQUEST_REQUERY_CONN_LOST =
            new Intent(in.dc297.mqttclpro.tasker.activity.Intent.ACTION_REQUEST_QUERY).putExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_ACTIVITY,
                    ConnectionLostConfigActivity.class.getName());

    protected static final Intent INTENT_REQUEST_REQUERY_RECONNECTED =
            new Intent(in.dc297.mqttclpro.tasker.activity.Intent.ACTION_REQUEST_QUERY).putExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_ACTIVITY,
                    ReconnectConfigActivity.class.getName());

    private Handler handler;
    private HandlerThread handlerThread;

    /**
     * List of clients
     */
    private HashMap<Long, MqttAndroidClient> clients = null;

    /**Requery datastore object to save or delete or restore connections**/
    private ReactiveEntityStore<Persistable> data;

    private MQTTClientApplication application = null;


    private int maxMessages = 0;

    private static final String MAX_MESSAGES_KEY = "max_messages";


    /**
     * Create a clients object
     */
    private MQTTClients(MQTTClientApplication mqttClientApplication){
        application = mqttClientApplication;
        clients = new HashMap<Long, MqttAndroidClient>();
        data = application.getData();
        List<BrokerEntity> brokerEntities = data.select(BrokerEntity.class).where(BrokerEntity.ENABLED.eq(true)).get().toList();
        for(BrokerEntity brokerEntity : brokerEntities){
            clients.put(brokerEntity.getId(),fromEntity(brokerEntity));
        }
        handlerThread = new HandlerThread("messagearrived");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
        maxMessages = Integer.parseInt(mSharedPreferences.getString(MAX_MESSAGES_KEY,"0"));
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    public synchronized static MQTTClients getInstance(MQTTClientApplication mqttClientApplication){
        if(instance == null){
            Log.i(MQTTClients.class.getName(),"creating new instance");
            instance = new MQTTClients(mqttClientApplication);
        }
        return instance;
    }


    private MqttAndroidClient fromEntity(final BrokerEntity brokerEntity) {
        String hostName = brokerEntity.getHost();
        int portNum = Constants.DEFAULT_PORT_NUM;
        try{
            portNum = Integer.parseInt(brokerEntity.getPort());
        }
        catch(NumberFormatException nfe){
            nfe.printStackTrace();
        }

        String userName = brokerEntity.getUsername();
        String password = brokerEntity.getPassword();
        boolean ssl = brokerEntity.getSSLEnabled();
        boolean ws = brokerEntity.getWSEnabled();
        int keepAliveSeconds = Constants.DEFAULT_KEPPALIVE_INTERVAL;
        try{
            keepAliveSeconds = Short.parseShort(brokerEntity.getKeepAlive());
        }
        catch(NumberFormatException nfe){
            nfe.printStackTrace();
        }

        boolean cleanSession = brokerEntity.getCleanSession();
        String clientId = brokerEntity.getClientId();

        String lastWillTopic = brokerEntity.getLastWillTopic();
        String lastWillMessage = brokerEntity.getLastWillMessage();
        int lastWillQOS = Constants.DEFAULT_QOS;

        try{
            lastWillQOS = Integer.parseInt(brokerEntity.getLastWillQOS());
            MqttMessage.validateQos(lastWillQOS);
        }
        catch (NumberFormatException nfe){
            nfe.printStackTrace();
        }
        catch(IllegalArgumentException iae){
            iae.printStackTrace();
        }

        boolean lastWillRetained = brokerEntity.getLastWillRetained();

        String caCrt = brokerEntity.getCACrt();
        String clientCrt = brokerEntity.getClientCrt();
        String clientKey = brokerEntity.getClientKey();
        String clientKeyPassword = brokerEntity.getClientKeyPwd();
        String clientP12 = brokerEntity.getClientP12Crt();
        String protocol = "tcp";
        if(ws) protocol = "ws";
        if(ssl){
            if(ws) {
                protocol = "wss";
            }
            else{
                protocol = "ssl";
            }
        }
        final String uri = protocol+"://" + hostName + ":" + portNum;


        final MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(application.getApplicationContext(),uri,clientId, new MemoryPersistence());
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                setBrokerStatus(brokerEntity,(reconnect?"Rec":"C")+"onnected to " + uri);
                subscribeToTopics(brokerEntity,mqttAndroidClient);
                if(reconnect) {
                    int taskerPassthroughMessageId = TaskerPlugin.Event.addPassThroughMessageID(INTENT_REQUEST_REQUERY_RECONNECTED);
                    TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY_RECONNECTED, PluginBundleManager.generateBundle(application.getApplicationContext(), "", "", taskerPassthroughMessageId));


                    brokerEntity.setTaskerPassThroughId(taskerPassthroughMessageId);
                    try {
                        data.update(brokerEntity).blockingGet();
                        application.sendBroadcast(INTENT_REQUEST_REQUERY_RECONNECTED);
                        Log.i(MQTTClients.class.getName(), "broadcasting reconnected with tasker id: " + taskerPassthroughMessageId);
                    } catch (RowCountException e) {
                        //seems like we have already deleted broker!
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void startingConnect(boolean reconnect) {

                setBrokerStatus(brokerEntity,(reconnect?"Rec":"C")+"onnecting to " + uri);
            }


            @Override
            public void connectionLost(Throwable cause) {
                if(cause!=null) cause.printStackTrace();
                int taskerPassthroughMessageId = TaskerPlugin.Event.addPassThroughMessageID(INTENT_REQUEST_REQUERY_CONN_LOST);
                TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY_CONN_LOST,PluginBundleManager.generateBundle(application.getApplicationContext(), "", "",taskerPassthroughMessageId));

                if(!brokerEntity.getEnabled()){
                    setBrokerStatus(brokerEntity, "Disabled");
                }
                else {
                    brokerEntity.setTaskerPassThroughId(taskerPassthroughMessageId);
                    try {
                        data.update(brokerEntity).blockingGet();
                        setBrokerStatus(brokerEntity, "Connection lost from " + uri);
                        application.sendBroadcast(INTENT_REQUEST_REQUERY_CONN_LOST);
                        Log.i(MQTTClients.class.getName(), "broadcasting connection lost with tasker id: " + taskerPassthroughMessageId);
                    } catch (RowCountException e) {
                        //seems like we have already deleted broker!
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void messageArrived(String topic, final MqttMessage message) throws Exception {
                final String receivedTopic = topic;
                final MqttMessage receivedMessage = message;
                if(handler!=null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            data.select(TopicEntity.class).where(TopicEntity.BROKER.eq(brokerEntity).and(TopicEntity.TYPE.eq(0)))
                            .get().observable()
                            .subscribe(new Consumer<TopicEntity>() {
                                @Override
                                public void accept(TopicEntity topicEntity) throws Exception {
                                    if (Util.mosquitto_topic_matches_sub(topicEntity.getName(), receivedTopic)) {
                                        MessageEntity messageEntity = new MessageEntity();
                                        messageEntity.setPayload(new String(receivedMessage.getPayload()));
                                        messageEntity.setTopic(topicEntity);
                                        messageEntity.setQOS(receivedMessage.getQos());
                                        messageEntity.setTimeStamp(new Timestamp(System.currentTimeMillis()));
                                        messageEntity.setDisplayTopic(receivedTopic);
                                        messageEntity.setRetained(receivedMessage.isRetained());

                                        final int taskerMessageId = TaskerPlugin.Event.addPassThroughMessageID(INTENT_REQUEST_REQUERY);
                                        messageEntity.setTaskerId(taskerMessageId);

                                        data.insert(messageEntity).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                                                new Consumer<MessageEntity>() {
                                                    @Override
                                                    public void accept(MessageEntity messageEntity) throws Exception {

                                                        Bundle publishedBundle = PluginBundleManager.generateBundle(application.getApplicationContext(), messageEntity.getPayload(), messageEntity.getDisplayTopic(),messageEntity.getId());

                                                        TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, publishedBundle);
                                                        Log.i(MQTTClients.class.getName(),"broadcasting message arrived with tasker id " + taskerMessageId);

                                                        Intent broadcastIntent = new Intent();
                                                        broadcastIntent.setAction(in.dc297.mqttclpro.mqtt.Constants.INTENT_FILTER_SUBSCRIBE + brokerEntity.getId());
                                                        application.sendBroadcast(broadcastIntent);
                                                        application.sendBroadcast(INTENT_REQUEST_REQUERY);
                                                        //tasker stuff starts
                                                    }
                                                }
                                        );

                                        if(maxMessages>0){
                                            data.count(MessageEntity.class).get().single()
                                                    .subscribe(new Consumer<Integer>() {
                                                        @Override
                                                        public void accept(final Integer integer) {
                                                            if (integer > maxMessages) {
                                                                data.select(MessageEntity.class).orderBy(MessageEntity.TIME_STAMP.desc()).limit(1).offset(maxMessages).get().observable()
                                                                        .subscribe(new Consumer<MessageEntity>() {
                                                                            @Override
                                                                            public void accept(MessageEntity messageEntity) throws Exception {
                                                                                Log.i(MQTTClients.class.getName(),"Deleting messages before " + messageEntity.getTimeStamp().toString());
                                                                                data.delete(MessageEntity.class).where(MessageEntity.TIME_STAMP.lessThan(messageEntity.getTimeStamp())).get().single().blockingGet();
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    final MqttMessage message = token.getMessage();
                    int messageId = message.getId();

                    if(messageId>0){
                        data.update(MessageEntity.class)
                            .set(MessageEntity.READ,1)
                            .where(MessageEntity.ID.eq(messageId))
                            .get()
                            .single()
                            .subscribeOn(Schedulers.single())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe();
                    }

                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setCleanSession(cleanSession);
        connectOptions.setKeepAliveInterval(keepAliveSeconds);

        if(!TextUtils.isEmpty(userName)){
            connectOptions.setUserName(userName);
        }
        if(!TextUtils.isEmpty(password)){
            connectOptions.setPassword(password.toCharArray());
        }
        try{
            MqttTopic.validate(lastWillTopic,false);
            connectOptions.setWill(lastWillTopic, lastWillMessage.getBytes(), lastWillQOS, lastWillRetained);
        }
        catch(Exception e){}

        if(ssl){
            SocketFactory mSocketFactory = SSLUtil.getSocketFactory(caCrt,clientCrt,clientKey,clientKeyPassword,clientP12);
            if(mSocketFactory!=null)connectOptions.setSocketFactory(mSocketFactory);
        }
        boolean v31 = brokerEntity.getv31();
        if(!v31){
            connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        }
        else{
            connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        }
        try {
            mqttAndroidClient.connect(connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(true);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    setBrokerStatus(brokerEntity,"Connected to " + uri);
                }


                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                   if(exception!=null) exception.printStackTrace();
                    if(exception instanceof MqttException){
                        if(((MqttException)exception).getReasonCode() == 32100){
                            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                            disconnectedBufferOptions.setBufferEnabled(true);
                            disconnectedBufferOptions.setBufferSize(100);
                            disconnectedBufferOptions.setPersistBuffer(false);
                            disconnectedBufferOptions.setDeleteOldestMessages(false);
                            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                            setBrokerStatus(brokerEntity,"Connected to " + uri);
                            //subscribeToTopics(brokerEntity,mqttAndroidClient);
                            return;
                        }
                        else{
                            /*TaskerPlugin.Event.addPassThroughMessageID(INTENT_REQUEST_REQUERY_CONN_LOST);
                            int taskerPassthroughMessageId = TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY_CONN_LOST, PluginBundleManager.generateBundle(application.getApplicationContext(), "", ""));
                            brokerEntity.setTaskerPassThroughId(taskerPassthroughMessageId);
                            try{
                                data.update(brokerEntity).blockingGet();
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }*/
                            Calendar wakeUpTime = Calendar.getInstance();
                            wakeUpTime.add(Calendar.MILLISECOND, mqttAndroidClient.getReconnectDelay());
                            setBrokerStatus(brokerEntity, "Failed to connect to " + uri + ". Next Connect scheduled at " + wakeUpTime.getTime());
                            application.sendBroadcast(INTENT_REQUEST_REQUERY_CONN_LOST);
                            //Log.i(MQTTClients.class.getName(), "broadcasting connection lost with tasker id: " + taskerPassthroughMessageId);
                        }
                    }

                }

                @Override
                public void onIntermediate(IMqttToken asyncActionToken) {
                    setBrokerStatus(brokerEntity, "Connecting to " + uri);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return mqttAndroidClient;
    }

    private void subscribeToTopics(final BrokerEntity brokerEntity, MqttAndroidClient mqttAndroidClient){
        if(mqttAndroidClient.isConnected()){
            Result<TopicEntity> topicEntities = data.select(TopicEntity.class).where(TopicEntity.BROKER.eq(brokerEntity).and(TopicEntity.TYPE.eq(0))).get();
            List<TopicEntity> topicEntityArrayList = topicEntities.toList();
            if(topicEntityArrayList.size()<=0){
                return;
            }
            String[] topics = new String[topicEntityArrayList.size()];
            int[] qoss = new int[topicEntityArrayList.size()];
            int i = 0;
            for(TopicEntity topicEntity : topicEntityArrayList){
                topics[i] = topicEntity.getName();
                qoss[i] = topicEntity.getQOS();
                i++;
            }
            try {
                mqttAndroidClient.subscribe(topics, qoss).setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        if(exception!=null) exception.printStackTrace();
                    }

                    @Override
                    public void onIntermediate(IMqttToken asyncActionToken) {

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribeToTopic(final BrokerEntity brokerEntity, String topic, int qos){
        MqttAndroidClient mqttAndroidClient = clients.get(brokerEntity.getId());
        if(mqttAndroidClient!=null){
            try {
                mqttAndroidClient.subscribe(topic, qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void publishMessage(final BrokerEntity brokerEntity, String topic, String message, int qos, boolean retained, int messageId){
        if(TextUtils.isEmpty(topic) || brokerEntity==null){
            return;
        }
        if(message==null) message="";

        MqttAndroidClient mqttAndroidClient = clients.get(brokerEntity.getId());
        if(mqttAndroidClient!=null && !TextUtils.isEmpty(mqttAndroidClient.getClientId()) && mqttAndroidClient.isConnected()){
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setId(messageId);
            mqttMessage.setRetained(retained);
            mqttMessage.setPayload(message.getBytes());
            mqttMessage.setQos(qos);
            try {
                mqttAndroidClient.publish(topic, mqttMessage, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(MQTTClients.class.getName(),"Successfully Published");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        if(exception!=null) exception.printStackTrace();
                    }

                    @Override
                    public void onIntermediate(IMqttToken asyncActionToken) {

                    }
                });
            } catch (MqttException e) {
                Toast.makeText(application.getApplicationContext(),"Failed to publish!",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    public void addBroker(final BrokerEntity brokerEntity){
        MqttAndroidClient mqttAndroidClient = clients.get(brokerEntity.getId());
        if(mqttAndroidClient!=null){
            //means we modified a broker
            try {
                IMqttToken mqttToken = mqttAndroidClient.disconnect();

                setBrokerStatus(brokerEntity,"Disconnecting");
                mqttToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        clients.remove(brokerEntity.getId());
                        if (brokerEntity.getEnabled()) {
                            clients.put(brokerEntity.getId(), fromEntity(brokerEntity));
                        } else {
                            setBrokerStatus(brokerEntity, "Disabled");
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        //something went wrong with disconnection!
                        clients.remove(brokerEntity.getId());
                        exception.printStackTrace();
                        if (brokerEntity.getEnabled()) {
                            clients.put(brokerEntity.getId(), fromEntity(brokerEntity));
                        } else {
                            setBrokerStatus(brokerEntity, "Disabled");
                        }
                    }

                    @Override
                    public void onIntermediate(IMqttToken asyncActionToken) {
                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
            catch(IllegalArgumentException ee){
                ee.printStackTrace();
            }
        }
        else {
            if (brokerEntity.getEnabled()) {
                clients.put(brokerEntity.getId(), fromEntity(brokerEntity));
            }
            else{
                setBrokerStatus(brokerEntity, "Disabled");
            }
        }
    }

    public void removeBroker(BrokerEntity brokerEntity){
        MqttAndroidClient mqttAndroidClient = clients.get(brokerEntity.getId());
        if(mqttAndroidClient!=null){
            mqttAndroidClient.close();
            clients.remove(brokerEntity.getId());
        }
    }

    private void setBrokerStatus(BrokerEntity brokerEntity, String status){
        brokerEntity.setStatus(status);
        try{
            data.update(brokerEntity).blockingGet();
        }
        catch(RowCountException e){
            e.printStackTrace();
        }
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(in.dc297.mqttclpro.mqtt.Constants.INTENT_FILTER_STATUS+brokerEntity.getId());
        broadcastIntent.putExtra(in.dc297.mqttclpro.mqtt.Constants.INTENT_FILTER_STATUS_KEY, status);
        application.sendBroadcast(broadcastIntent);
    }

    public void unSubscribe(BrokerEntity brokerEntity, String topic){
        MqttAndroidClient mqttAndroidClient = clients.get(brokerEntity.getId());
        if(mqttAndroidClient!=null){
            try {
                mqttAndroidClient.unsubscribe(topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void doConnect(BrokerEntity brokerEntity){
        if(clients.get(brokerEntity.getId())!=null){
            MqttAndroidClient mqttAndroidClient = clients.get(brokerEntity.getId());
            mqttAndroidClient.close();
            clients.remove(brokerEntity.getId());
        }
        clients.put(brokerEntity.getId(),fromEntity(brokerEntity));

    }

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(MAX_MESSAGES_KEY)){
                maxMessages = Integer.parseInt(sharedPreferences.getString(key,"0"));
            }
        }
    };
}
