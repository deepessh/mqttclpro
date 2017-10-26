package in.dc297.mqttclpro.mqtt.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Deepesh on 10/25/2017.
 */

public class MqttBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(MqttBroadcastReceiver.class.getName(),"Received a broadcast");
    }
}
