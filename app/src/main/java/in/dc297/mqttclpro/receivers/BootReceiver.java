package in.dc297.mqttclpro.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;
import in.dc297.mqttclpro.services.MyMqttService;


/**
 * Created by dc297 on 4/5/2017.
 */

public class BootReceiver extends BroadcastReceiver
{

    public void onReceive(Context context, Intent intent)
    {
        Log.i(BootReceiver.class.getName(),"Received start intent");
        //start service
        Intent svc = new Intent(context, MyMqttService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        }
        else{
            context.startService(svc);
        }
    }
}