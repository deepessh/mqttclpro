package in.dc297.mqttclpro;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService {

    public MyIntentService() {
        super("MyIntentService");
    }
    private DBHelper db = null;


    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public void startActionPublish(final Intent intent) {
        bindService(new Intent(getApplicationContext(), MQTTService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                db = new DBHelper(getApplicationContext());
                MQTTService mqttService = ((MQTTService.LocalBinder<MQTTService>) iBinder).getService();

                String topic = intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC);
                String message = intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE);
                String qos = intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_QOS);
                boolean retained = intent.getExtras().getBoolean(in.dc297.mqttclpro.tasker.Intent.EXTRA_RETAINED);

                try{
                    MqttTopic.validate(topic,false);
                }
                catch (IllegalStateException ise){
                    ise.printStackTrace();
                }
                catch(IllegalArgumentException iae){
                    iae.printStackTrace();
                }

                try{
                    MqttMessage.validateQos(Integer.parseInt(qos));
                }
                catch(IllegalArgumentException iae){
                    iae.printStackTrace();
                }



                Log.i("mqttsrv","Received a fire :D"+intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC));

                db.addTopic(topic,1,Integer.parseInt(qos));
                long mid = db.addMessage(topic,message,1,Integer.parseInt(qos));
                mqttService.publishMessage(topic,message,qos,mid,retained);
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                db.close();

            }
        },0);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (MQTTService.MQTT_PUBLISH.equals(action)) {
                startActionPublish(intent);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
