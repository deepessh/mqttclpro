package in.dc297.mqttclpro.tasker.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.util.List;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.entity.BrokerEntity;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import tasker.TaskerPlugin;

import static in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BUNDLE;

public class ActionEditActivity extends AbstractPluginActivity {

    private ReactiveEntityStore<Persistable> data = null;
    private List<BrokerEntity> brokerEntityList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bundle taskerBundle = getIntent().getBundleExtra(EXTRA_BUNDLE);

        final String topic = taskerBundle!=null?taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC):"";
        final String message = taskerBundle!=null ? taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE) : "";
        final boolean retained = taskerBundle!=null?taskerBundle.getBoolean(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_RETAINED,false):false;
        final String qos = taskerBundle!=null?taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_QOS):"";
        final Long brokerId = taskerBundle!=null?taskerBundle.getLong(Intent.EXTRA_BROKER_ID):0;


        final Spinner qosSpinner = (Spinner) findViewById(R.id.qos_spinner);
        Switch retainedSwitch = (Switch) findViewById(R.id.message_retained);

        ArrayAdapter qosAdapter = ArrayAdapter.createFromResource(this, R.array.qos_array, R.layout.simple_spinner_item_black);
        qosAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_black);

        qosSpinner.setAdapter(qosAdapter);


        data = ((MQTTClientApplication) getApplication()).getData();
        brokerEntityList = data.select(BrokerEntity.class).where(BrokerEntity.ENABLED.eq(true)).get().toList();
        if(brokerEntityList.size()<=0){
            Toast.makeText(getApplicationContext(),"Please add at least 1 broker before configuring tasker event.",Toast.LENGTH_SHORT).show();
            mIsCancelled = true;
            finish();
            return;
        }
        Spinner brokerSpinner = (Spinner) findViewById(R.id.brokerSpinner);

        String[] brokers = new String[brokerEntityList.size()];
        int i_b = 0;
        int selIndex_b = 0;
        for(BrokerEntity brokerEntity:brokerEntityList){
            brokers[i_b] = brokerEntity.getNickName() + " - " + brokerEntity.getHost();
            if(brokerId == brokerEntity.getId()) selIndex_b = i_b;
            i_b++;
        }

        ArrayAdapter dataAdapter_b = new ArrayAdapter(this,
                R.layout.simple_spinner_item_black, brokers);
        dataAdapter_b.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_black);
        brokerSpinner.setAdapter(dataAdapter_b);


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
            if(brokerId!=0){
                brokerSpinner.setSelection(selIndex_b);
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
            final int brokerPosition = ((Spinner)findViewById(R.id.brokerSpinner)).getSelectedItemPosition();
            final Long brokerId = brokerEntityList.size()>=brokerPosition?brokerEntityList.get(brokerPosition).getId():0;
            final String brokerString = ((Spinner)findViewById(R.id.brokerSpinner)).getSelectedItem().toString();

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

            if (topic.length() > 0 && brokerId>0)
            {
                final android.content.Intent resultIntent = new android.content.Intent();
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                final String blurb = generateBlurb(getApplicationContext(),brokerString + " : " +  topic+" : "+message+" : "+qos + " : "+retained);
                resultIntent.putExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_STRING_BLURB, blurb);

                Bundle taskerBundle = new Bundle();
                taskerBundle.putString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC, topic);
                taskerBundle.putString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE, message);
                taskerBundle.putBoolean(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_RETAINED,retained);
                taskerBundle.putString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_QOS,qos);
                taskerBundle.putLong(Intent.EXTRA_BROKER_ID, brokerId);
                taskerBundle.putString(Intent.ACTION_OPERATION,Intent.MQTT_PUBLISH_ACTION);

                //replace tasker variables
                resultIntent.putExtra(TaskerPlugin.Setting.BUNDLE_KEY_VARIABLE_REPLACE_STRINGS,in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC+" "+in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE);

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
