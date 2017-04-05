package in.dc297.mqttclpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Created by dc297 on 4/5/2017.
 */

public class BootReceiver extends BroadcastReceiver
{

    public void onReceive(Context context, Intent intent)
    {
        Intent svc = new Intent(context, MQTTService.class);
        context.startService(svc);
    }
}