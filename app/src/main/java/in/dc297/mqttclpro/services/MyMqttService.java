package in.dc297.mqttclpro.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import in.dc297.mqttclpro.BuildConfig;
import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.BrokersListActivity;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;

public class MyMqttService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String MY_NOTIFICATION_KEY = "notifications_new_message";
    public MyMqttService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MyMqttService.class.getName(),"Starting service");
        MQTTClients mqttClients = MQTTClients.getInstance((MQTTClientApplication)getApplication());
        if(shouldShowNotification()) showNotification();
        registerReceivers();
        return START_STICKY;
    }

    private void showNotification() {
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        int resourceId = R.drawable.ic_notifications_black_24dp;

        if(android.os.Build.VERSION.SDK_INT<=19) resourceId = R.mipmap.ic_launcher;//fix for kitkat

        builder.setSmallIcon(resourceId)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Running in background")
                .setAutoCancel(false);
        Notification notification = builder.getNotification();

        notification.flags |= Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT;
        Intent resultIntent = new Intent(this, BrokersListActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(BrokersListActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.contentIntent = resultPendingIntent;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Log.i(MyMqttService.class.getName(),"Adding notification");
        notificationManager.notify(1, notification);
    }

    private boolean shouldShowNotification(){
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(mSharedPreferences.getBoolean(MY_NOTIFICATION_KEY,true)){
            return true;
        }
        return false;
    }

    private void removeNotification(){
        Log.i(MyMqttService.class.getName(),"Removing notification");
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    public void onCreate(){
        Log.i(MyMqttService.class.getName(),"Creating service");
    }

    private void registerReceivers(){
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void unregisterReceivers(){
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(mSharedPreferences!=null) mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onDestroy(){
        unregisterReceivers();
        removeNotification();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(MyMqttService.class.getName(),"Preference changed");
        if(key.equals(MY_NOTIFICATION_KEY)){
            if(sharedPreferences.getBoolean(MY_NOTIFICATION_KEY,true)){
                showNotification();
            }
            else{
                removeNotification();
            }
        }
    }
}
