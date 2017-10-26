package in.dc297.mqttclpro.mqtt.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;

/**
 * Created by Deepesh on 10/25/2017.
 */

public class MQTTService extends Service {

    private MQTTClients mqttClients = null;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mqttClients = MQTTClients.getInstance((MQTTClientApplication)getApplication());
        registerBroadcastReceivers();
    }

    @SuppressWarnings("deprecation")
    protected void registerBroadcastReceivers() {

    }

    protected void unregisterBroadcastReceivers(){

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterBroadcastReceivers();
    }
}
