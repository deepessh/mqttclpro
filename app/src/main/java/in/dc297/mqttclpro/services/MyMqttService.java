package in.dc297.mqttclpro.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;

public class MyMqttService extends Service {
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
        return START_STICKY;
    }

    @Override
    public void onCreate(){
        Log.i(MyMqttService.class.getName(),"Creating service");
    }
}
