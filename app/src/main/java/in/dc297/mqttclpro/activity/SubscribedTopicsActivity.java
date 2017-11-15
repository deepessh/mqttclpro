package in.dc297.mqttclpro.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.databinding.TopicListItemBinding;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.entity.Message;
import in.dc297.mqttclpro.entity.MessageEntity;
import in.dc297.mqttclpro.entity.TopicEntity;
import in.dc297.mqttclpro.mqtt.Constants;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.MutableResult;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import io.requery.sql.StatementExecutionException;

public class SubscribedTopicsActivity extends AppCompatActivity {


    public static final String EXTRA_BROKER_ID = "EXTRA_BROKER_ID";

    private EntityDataStore<Persistable> data;
    private BrokerEntity broker;
    private ExecutorService executor;
    private TopicsListAdapter adapter;
    private long brokerId;
    private static MQTTClients mqttClients = null;
    private MqttBroadcastReceiver mqttBroadcastReceiver = null;
    private MqttStatusBroadcastReceiver mqttStatusBroadcastReceiver = null;
    private TextView statusTV = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);
        Intent intent = getIntent();
        brokerId = intent.getLongExtra(EXTRA_BROKER_ID, -1);
        if (brokerId == -1) {
            Toast.makeText(getApplicationContext(), "Something's wrong", Toast.LENGTH_SHORT).show();
            finish();
        }
        mqttClients = MQTTClients.getInstance((MQTTClientApplication)getApplication());
        statusTV = (TextView)findViewById(R.id.statusTV);
        data = ((MQTTClientApplication) getApplication()).getData();
        final Spinner qosSpinner = (Spinner) findViewById(R.id.qos_spinner);

        ArrayAdapter qosAdapter = ArrayAdapter.createFromResource(this, R.array.qos_array, android.R.layout.simple_spinner_item);
        qosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        qosSpinner.setAdapter(qosAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pubActivityIntent = new Intent(getApplicationContext(), PublishActivity.class);
                pubActivityIntent.putExtra(PublishActivity.EXTRA_BROKER_ID, brokerId);
                startActivity(pubActivityIntent);
            }
        });

        final EditText topicEdit = (EditText) findViewById(R.id.subscribeTopicEditText);
        if (topicEdit != null) {
            topicEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(final TextView v, int actionId, KeyEvent event) {
                    final String topic = topicEdit.getText().toString();
                    final String qos = qosSpinner.getSelectedItem().toString();
                    try{
                        MqttTopic.validate(topic,true);
                    }
                    catch(Exception e){
                        Snackbar.make(v,"Invalid MQTT Topic",Snackbar.LENGTH_SHORT)
                                .setAction("Error",null).show();
                        return false;
                    }

                    try{
                        MqttMessage.validateQos(Integer.parseInt(qos));
                    }
                    catch(Exception e){
                        Snackbar.make(v,"Invalid QOS",Snackbar.LENGTH_SHORT)
                                .setAction("Error",null).show();
                        return false;
                    }

                    TopicEntity topicEntity = new TopicEntity();
                    topicEntity.setName(topic);
                    topicEntity.setQOS(Integer.parseInt(qos));
                    topicEntity.setBroker(broker);
                    TopicEntity insertedTopicEntity = data.insert(topicEntity);
                    if(insertedTopicEntity.getId()>0) {
                        adapter.queryAsync();
                        mqttClients.subscribeToTopic(broker, topic, Integer.parseInt(qos));
                        return true;
                    }
                    Toast.makeText(getApplicationContext(), "Topic already Exists or some error occured!", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        executor = Executors.newSingleThreadExecutor();
        adapter = new TopicsListAdapter();
        adapter.setExecutor(executor);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Integer integer = data.count(TopicEntity.class).where(TopicEntity.BROKER_ID.eq(brokerId).and(TopicEntity.TYPE.eq(0))).get().value();
        if (integer == 0) {
            Toast.makeText(getApplicationContext(), "Please add a topic!",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        BrokerEntity brokerEntity = data.findByKey(BrokerEntity.class, brokerId);
        broker = brokerEntity;
        setTitle(broker.getNickName() + " - subscribed topics");
        statusTV.setText(broker.getStatus());

        adapter.queryAsync();
        mqttBroadcastReceiver = new MqttBroadcastReceiver();
        mqttStatusBroadcastReceiver = new MqttStatusBroadcastReceiver();
        registerReceiver(mqttBroadcastReceiver, new IntentFilter(Constants.INTENT_FILTER_SUBSCRIBE+brokerId));
        registerReceiver(mqttStatusBroadcastReceiver,new IntentFilter(Constants.INTENT_FILTER_STATUS+brokerId));
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mqttBroadcastReceiver!=null) {
            unregisterReceiver(mqttBroadcastReceiver);
            mqttBroadcastReceiver = null;
        }
        if(mqttStatusBroadcastReceiver!=null){
            unregisterReceiver(mqttStatusBroadcastReceiver);
            mqttStatusBroadcastReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        adapter.close();
        super.onDestroy();
    }

    @Override
    public boolean onContextItemSelected(MenuItem menu){
        switch(menu.getItemId()){
            case R.id.delete:
                if(adapter.toDelete!=null) {
                    data.delete(adapter.toDelete);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.queryAsync();
                            mqttClients.unSubscribe(broker,adapter.toDelete.getName());
                        }
                    });
                }
                break;
            case R.id.deleteMessages:
                if(adapter.toDelete!=null){
                    data.delete(MessageEntity.class)
                            .where(MessageEntity.TOPIC
                                    .eq(adapter.toDelete))
                            .get();
                    adapter.queryAsync();
                }
                break;
            default:
                super.onContextItemSelected(menu);
        }
        return true;
    }
    public class TopicsListAdapter extends QueryRecyclerAdapter<TopicEntity, BindingHolder<TopicListItemBinding>> implements View.OnClickListener {

        public TopicEntity toDelete;

        TopicsListAdapter(){
            super(TopicEntity.$TYPE);
        }
        @Override
        public void onClick(View v) {
            TopicListItemBinding binding = (TopicListItemBinding) v.getTag();
            if(binding!=null){
                Intent intent = new Intent(v.getContext(),MessageActivity.class);
                intent.putExtra(MessageActivity.EXTRA_TOPIC_ID,binding.getTopic().getId());
                //Toast.makeText(v.getContext(),binding.getBroker().toString(),Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        }

        @Override
        public Result<TopicEntity> performQuery() {
            return data.select(TopicEntity.class).where(TopicEntity.BROKER_ID.eq(brokerId).and(TopicEntity.TYPE.eq(0))).get();
        }

        @Override
        public void onBindViewHolder(final TopicEntity topic, BindingHolder<TopicListItemBinding> topicListItemBindingBindingHolder, int i) {
            MutableResult<Message> messages = topic.getMessages();
            topic.setUnreadCount(0);
            topic.setLatestMessage(new MessageEntity());
            messages.each(new io.requery.util.function.Consumer<Message>() {
                @Override
                public void accept(Message message) {
                    if(message.getRead()==0) topic.setUnreadCount(topic.getUnreadCount()+1);
                    if(topic.getLatestMessage()!=null
                            && topic.getLatestMessage().getTimeStamp()!=null
                            && topic.getLatestMessage().getTimeStamp().before(message.getTimeStamp()))
                        topic.setLatestMessage(message);
                    else if (topic.getLatestMessage()!=null && topic.getLatestMessage().getTimeStamp()==null) topic.setLatestMessage(message);
                }
            });
            topic.setCountVisibility(topic.getUnreadCount() == 0 ? View.INVISIBLE : View.VISIBLE);
            topicListItemBindingBindingHolder.binding.setTopic(topic);
        }

        @Override
        public BindingHolder<TopicListItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater  = LayoutInflater.from(parent.getContext());
            final TopicListItemBinding binding = TopicListItemBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    MenuInflater inflater = getMenuInflater();
                    inflater.inflate(R.menu.delete_subscribe_topic_menu, menu);
                    toDelete = (TopicEntity) binding.getTopic();
                }
            });
            TextView topicTV = (TextView) binding.getRoot().findViewById(R.id.topic_tv);
            if(getApplication().getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE) topicTV.setMaxEms(20);
            else topicTV.setMaxEms(8);
            topicTV.setSelected(true);

            return new BindingHolder<>(binding);
        }
    }

    private class MqttBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            adapter.queryAsync();
            Log.i(SubscribedTopicsActivity.class.getName(),"Received broadcast");
        }
    }

    private class MqttStatusBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            BrokerEntity brokerEntity = data.findByKey(BrokerEntity.class, brokerId);
            broker = brokerEntity;
            setTitle(broker.getNickName() + " - subscribed topics");
            statusTV.setText(broker.getStatus());
        }
    }
}
