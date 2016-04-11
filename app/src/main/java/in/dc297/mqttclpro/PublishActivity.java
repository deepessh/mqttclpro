package in.dc297.mqttclpro;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttTopic;

public class PublishActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DBHelper db = new DBHelper(this);
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Spinner qosSpinner = (Spinner) findViewById(R.id.qos_spinner);

        ArrayAdapter qosAdapter = ArrayAdapter.createFromResource(this, R.array.qos_array, android.R.layout.simple_spinner_item);
        qosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        qosSpinner.setAdapter(qosAdapter);

        final ListView pubTopsLV = (ListView) findViewById(R.id.listView);
        String[] from = new String[] { "topic", "count","message","timest"};
        int[] to = new int[]{R.id.topic_tv, R.id.message_count,R.id.message_tv,R.id.timestamp_tv };

        final TopicsListAdapter pubtopsAdapter = new TopicsListAdapter(getApplicationContext(),R.layout.subscribed_topics_list_item,db.getTopics(1),from,to,0);
        pubTopsLV.setAdapter(pubtopsAdapter);

        pubTopsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String topic = ((TextView) view.findViewById(R.id.topic_tv)).getText().toString();

                if(topic!=null & !topic.equals("")){
                    Intent publishedTopicIntent = new Intent(getApplicationContext(),PublishedTopicActivity.class);
                    publishedTopicIntent.putExtra("topic",topic);
                    startActivity(publishedTopicIntent);
                }
            }
        });

        final EditText topicEditText = (EditText) findViewById(R.id.topic_edittext);
        final EditText messageEditText = (EditText) findViewById(R.id.message_edittext);

        Button publishButton = (Button) findViewById(R.id.publish_button);
        if (publishButton != null) {
            publishButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String topic = topicEditText.getText().toString();
                    final String message = messageEditText.getText().toString();
                    final String qos = qosSpinner.getSelectedItem().toString();

                    if(topic==null || topic.equals("")){
                        Toast.makeText(getApplicationContext(),"Invalid topic value",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(message==null || message.equals("")){
                        Toast.makeText(getApplicationContext(),"Invalid message value",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(qos==null || qos.equals("")){
                        Toast.makeText(getApplicationContext(),"Invalid QOS value",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try{
                        MqttTopic.validate(topic,false);
                    }
                    catch(IllegalArgumentException ila){
                        Toast.makeText(getApplicationContext(),"Invalid topic",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    catch(IllegalStateException ise){
                        Toast.makeText(getApplicationContext(),"Invalid topic",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bindService(new Intent(getApplicationContext(), MQTTService.class), new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            MQTTService mqttService = ((MQTTService.LocalBinder<MQTTService>) service).getService();
                            mqttService.publishMessage(topic, message, qos);
                            unbindService(this);
                            pubtopsAdapter.swapCursor(db.getTopics(1));
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                        }
                    },0);
                }
            });
        }
    }

}
