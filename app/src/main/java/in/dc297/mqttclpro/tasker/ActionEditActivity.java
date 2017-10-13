package in.dc297.mqttclpro.tasker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import net.dinglisch.android.tasker.TaskerPlugin;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import in.dc297.mqttclpro.R;

import static in.dc297.mqttclpro.tasker.Constants.LOG_TAG;
import static in.dc297.mqttclpro.tasker.Intent.EXTRA_BUNDLE;

public class ActionEditActivity extends AbstractPluginActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle taskerBundle = getIntent().getBundleExtra(EXTRA_BUNDLE);

        final String topic = taskerBundle!=null?taskerBundle.getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC):"";
        final String message = taskerBundle!=null ? taskerBundle.getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE) : "";
        final boolean retained = taskerBundle!=null?taskerBundle.getBoolean(in.dc297.mqttclpro.tasker.Intent.EXTRA_RETAINED,false):false;
        final String qos = taskerBundle!=null?taskerBundle.getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_QOS):"";

        final Spinner qosSpinner = (Spinner) findViewById(R.id.qos_spinner);
        Switch retainedSwitch = (Switch) findViewById(R.id.message_retained);

        ArrayAdapter qosAdapter = ArrayAdapter.createFromResource(this, R.array.qos_array, android.R.layout.simple_spinner_item);
        qosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        qosSpinner.setAdapter(qosAdapter);

        if (null == savedInstanceState)
        {
            if(message!=null && message!="") {
                ((EditText) findViewById(R.id.editText2)).setText(message);
            }
            if(topic!=null && topic!="") {
                ((EditText) findViewById(R.id.editText)).setText(topic);
            }
            retainedSwitch.setChecked(retained);
            if("0".equals(qos) || "1".equals(qos) || "2".equals(qos)) {
                qosSpinner.setSelection(Integer.parseInt(qos), true);
            }
        }
    }

    @Override
    public void finish()
    {
        if (!isCanceled())
        {
            final Switch retainedSwitch = (Switch) findViewById(R.id.message_retained);
            final boolean retained = retainedSwitch.isChecked();
            String qos = ((Spinner) findViewById(R.id.qos_spinner)).getSelectedItem().toString();
            final String topic = ((EditText) findViewById(R.id.editText)).getText().toString();
            final String message = ((EditText) findViewById(R.id.editText2)).getText().toString();

            try{
                MqttTopic.validate(topic,false);
            }
            catch(IllegalArgumentException iae){
                Toast.makeText(getApplicationContext(),"Invalid Topic",Toast.LENGTH_SHORT).show();
                return;
            }
            catch (IllegalStateException ise){
                Toast.makeText(getApplicationContext(),"Invalid Topic",Toast.LENGTH_SHORT).show();
                return;
            }
            try{
                MqttMessage.validateQos(Integer.parseInt(qos));
            }
            catch(IllegalArgumentException iae){
                Toast.makeText(getApplicationContext(),"Invalid QOS",Toast.LENGTH_SHORT).show();
                return;
            }

            if (topic.length() > 0)
            {
                final Intent resultIntent = new Intent();
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                final String blurb = generateBlurb(getApplicationContext(), topic+" : "+message+" : "+qos + " : "+retained);
                resultIntent.putExtra(in.dc297.mqttclpro.tasker.Intent.EXTRA_STRING_BLURB, blurb);

                Bundle taskerBundle = new Bundle();
                taskerBundle.putString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC, topic);
                taskerBundle.putString(in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE, message);
                taskerBundle.putBoolean(in.dc297.mqttclpro.tasker.Intent.EXTRA_RETAINED,retained);
                taskerBundle.putString(in.dc297.mqttclpro.tasker.Intent.EXTRA_QOS,qos);

                //replace tasker variables
                resultIntent.putExtra(TaskerPlugin.Setting.BUNDLE_KEY_VARIABLE_REPLACE_STRINGS,in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC+" "+in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE);

                resultIntent.putExtra(EXTRA_BUNDLE,taskerBundle);

                setResult(RESULT_OK, resultIntent);
            }
        }

        super.finish();
    }

    static String generateBlurb(final Context context, final String message)
    {
        final int maxBlurbLength =
                context.getResources().getInteger(R.integer.max_blurb_length);

        if (message.length() > maxBlurbLength)
        {
            return message.substring(0, maxBlurbLength);
        }

        return message;
    }

}
