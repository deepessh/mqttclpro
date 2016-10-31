package in.dc297.mqttclpro;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SubscribeActivity extends AppCompatActivity {

    private DBHelper db = null;
    TextView statusTv;
    private ArrayList<TopicsListViewModel> tlvmal = null;
    ListView topicsLv = null;
    TopicsListAdapter topicsLVAdapter = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        //start service
        Intent svc = new Intent(this, MQTTService.class);
        startService(svc);
        db = new DBHelper(getApplicationContext());
        topicsLv = (ListView) findViewById(R.id.subsctibeTopicListView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Spinner qosSpinner = (Spinner) findViewById(R.id.qos_spinner);

        ArrayAdapter qosAdapter = ArrayAdapter.createFromResource(this, R.array.qos_array, android.R.layout.simple_spinner_item);
        qosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        qosSpinner.setAdapter(qosAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pubActivityIntent = new Intent(getApplicationContext(),PublishActivity.class);
                startActivity(pubActivityIntent);
            }
        });

        String[] from = new String[] { "topic", "count","message","timest"};
        int[] to = new int[]{R.id.topic_tv, R.id.message_count,R.id.message_tv,R.id.timestamp_tv };
        topicsLVAdapter = new TopicsListAdapter(this,R.layout.subscribed_topics_list_item,db.getTopics(0),from,to,0);
        topicsLv.setAdapter(topicsLVAdapter);

        final EditText topicEdit = (EditText) findViewById(R.id.subscribeTopicEditText);
        if (topicEdit != null) {
            topicEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    final String topic = topicEdit.getText().toString();
                    final String qos = qosSpinner.getSelectedItem().toString();
                    if (topic != null && !topic.equals("") && qos!=null && !qos.equals("")) {
                        int atret = db.addTopic(topic, 0,Integer.parseInt(qos));
                        switch (atret) {
                            case 0:
                                topicsLVAdapter.swapCursor(db.getTopics(0));
                                topicEdit.setText("");
                                bindService(new Intent(getApplicationContext(), MQTTService.class),
                                        new ServiceConnection() {
                                            @SuppressWarnings("unchecked")
                                            @Override
                                            public void onServiceConnected(ComponentName className, final IBinder service) {
                                                MQTTService mqttService = ((MQTTService.LocalBinder<MQTTService>) service).getService();
                                                mqttService.subscribeToTopic(topic);
                                                unbindService(this);
                                            }

                                            @Override
                                            public void onServiceDisconnected(ComponentName name) {
                                            }
                                        },
                                        0);
                                break;
                            case 1:
                                Snackbar.make(v, "Topic already added.", Snackbar.LENGTH_LONG)
                                        .setAction("Error", null).show();
                                return true;
                            case 3:
                                Snackbar.make(v,"Invalid MQTT Topic",Snackbar.LENGTH_SHORT)
                                        .setAction("Error",null).show();
                                return true;
                            default:
                                Snackbar.make(v, "Unknown error Occurred.", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                return true;
                        }
                    }
                    else{
                        Snackbar.make(v,"Please make sure that you entered valid topic and selected correct QOS.",Snackbar.LENGTH_SHORT)
                                .setAction("Error",null).show();
                    }
                    return false;
                }
            });
            //tlvmal = db.getTopics(0);

            //topicsLv.setAdapter();
            topicsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView topicTV = (TextView) view.findViewById(R.id.topic_tv);
                    TextView timestampTV = (TextView) view.findViewById(R.id.timestamp_tv);
                    if(topicTV!=null) {
                        String topic = (String) topicTV.getText();
                        String timestamp = (String) timestampTV.getText();
                        if(timestamp==null || timestamp.equals("")){
                            Toast.makeText(getApplicationContext(),"No messages received.",Toast.LENGTH_SHORT).show();
                            topicsLVAdapter.swapCursor(db.getTopics(0));
                            return;
                        }
                        if(topic!=null & !topic.equals("")){
                            Intent messageActivityIntent = new Intent(getApplicationContext(),MessageActivity.class);
                            messageActivityIntent.putExtra("in.dc297.mqttclpro.topic",topic);
                            startActivity(messageActivityIntent);
                            return;
                        }
                    }
                    Toast.makeText(getApplicationContext(),"Unknown command",Toast.LENGTH_SHORT).show();
                }
            });
            registerForContextMenu(topicsLv);
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        statusTv = (TextView) findViewById(R.id.statusTV);
        if (statusTv != null) {
            statusTv.setText(settings.getString("servicestatus","Waiting for connection"));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.subsctibeTopicListView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_subscribe_context, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.delete:
                final String topic = ((TextView)info.targetView.findViewById(R.id.topic_tv)).getText().toString();
                if(db.deleteTopic(topic,0)==1){
                    Toast.makeText(getApplicationContext(),"Successfully deleted topic",Toast.LENGTH_LONG).show();
                    bindService(new Intent(getApplicationContext(), MQTTService.class),
                            new ServiceConnection() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public void onServiceConnected(ComponentName className, final IBinder service) {
                                    MQTTService mqttService = ((MQTTService.LocalBinder<MQTTService>) service).getService();
                                    mqttService.unsubscribeFromTopic(topic);
                                    unbindService(this);
                                }

                                @Override
                                public void onServiceDisconnected(ComponentName name) {
                                }
                            },
                            0);
                    topicsLVAdapter.swapCursor(db.getTopics(0));
                }
                else{
                    Toast.makeText(getApplicationContext(),"Failed to delete topic",Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_subscribe, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this,SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            topicsLVAdapter.swapCursor(db.getTopics(0));
        }
    };
    private BroadcastReceiver statReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getExtras().get(MQTTService.MQTT_STATUS_MSG).toString();
            statusTv.setText(status);

        }
    };
    @Override
    protected void onResume(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(MQTTService.MQTT_MSG_RECEIVED_INTENT);
        registerReceiver(receiver, filter);
        IntentFilter filterStat = new IntentFilter();
        filterStat.addAction(MQTTService.MQTT_STATUS_INTENT);
        registerReceiver(statReceiver, filterStat);
        topicsLVAdapter.swapCursor(db.getTopics(0));
        super.onResume();
    }

    @Override
    protected void onPause(){
        unregisterReceiver(receiver);
        unregisterReceiver(statReceiver);
        super.onPause();
    }
}
