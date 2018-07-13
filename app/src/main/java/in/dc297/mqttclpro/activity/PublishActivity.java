package in.dc297.mqttclpro.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.databinding.TopicListItemBinding;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.entity.Message;
import in.dc297.mqttclpro.entity.MessageEntity;
import in.dc297.mqttclpro.entity.TopicEntity;
import in.dc297.mqttclpro.helpers.AdsHelper;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.MutableResult;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.sql.EntityDataStore;
import io.requery.sql.RowCountException;
import io.requery.util.CloseableIterator;

public class PublishActivity extends AppCompatActivity {

    public static final String EXTRA_BROKER_ID = "EXTRA_BROKER_ID";

    private ReactiveEntityStore<Persistable> data;
    private BrokerEntity broker;
    private ExecutorService executor;
    private TopicsListAdapter adapter;
    private long brokerId;
    private static MQTTClients mqttClients;
    private CloseableIterator<TopicEntity> topicEntityIterator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        data = ((MQTTClientApplication)getApplication()).getData();
        brokerId = getIntent().getLongExtra(EXTRA_BROKER_ID,-1);
        if(brokerId==-1) {
            Toast.makeText(getApplicationContext(),"Unknown Error!",Toast.LENGTH_SHORT).show();
            finish();
        }
        data.findByKey(BrokerEntity.class,brokerId)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BrokerEntity>() {
                    @Override
                    public void accept(BrokerEntity brokerEntity) throws Exception {
                        broker = brokerEntity;
                        setTitle(broker.getNickName() + " - Publish Messages");
                    }
                });
        mqttClients = MQTTClients.getInstance((MQTTClientApplication)getApplication());
        final Spinner qosSpinner = (Spinner) findViewById(R.id.qos_spinner);

        ArrayAdapter qosAdapter = ArrayAdapter.createFromResource(this, R.array.qos_array, android.R.layout.simple_spinner_item);
        qosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        qosSpinner.setAdapter(qosAdapter);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        executor = Executors.newSingleThreadExecutor();
        adapter = new TopicsListAdapter();
        adapter.setExecutor(executor);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        data.count(TopicEntity.class).where(TopicEntity.BROKER_ID.eq(brokerId).and(TopicEntity.TYPE.eq(1))).get().single()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        if (integer == 0) {
                            Toast.makeText(getApplicationContext(), "No message published on this broker yet!",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        final EditText topicEditText = (EditText) findViewById(R.id.topic_edittext);
        final EditText messageEditText = (EditText) findViewById(R.id.message_edittext);
        final Switch retainedSwitch = (Switch) findViewById(R.id.message_retained);
        Button publishButton = (Button) findViewById(R.id.publish_button);
        if (publishButton != null) {
            publishButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String topic = topicEditText.getText().toString();
                    final String message = messageEditText.getText().toString();
                    final String qos = qosSpinner.getSelectedItem().toString();
                    final boolean retained = retainedSwitch.isChecked();
                    if(topic==null || topic.equals("")){
                        Toast.makeText(getApplicationContext(),"Invalid topic value",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(message==null){
                        Toast.makeText(getApplicationContext(),"Invalid message value",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        MqttMessage.validateQos(Integer.parseInt(qos));
                    }
                    catch(Exception e){
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
                    topicEntityIterator = data.select(TopicEntity.class)
                            .where(TopicEntity.NAME.eq(topic)
                                    .and(TopicEntity.TYPE.eq(1)
                                            .and(TopicEntity.BROKER.eq(broker))
                                    )
                            )
                            .get()
                            .iterator();

                    MessageEntity messageEntity = new MessageEntity();
                    messageEntity.setDisplayTopic(topic);
                    messageEntity.setQOS(Integer.valueOf(qos));
                    messageEntity.setPayload(message);
                    messageEntity.setTimeStamp(new Timestamp(System.currentTimeMillis()));
                    messageEntity.setRetained(retained);
                    if(topicEntityIterator.hasNext()){
                        messageEntity.setTopic(topicEntityIterator.next());
                    }
                    else{
                        TopicEntity topicEntity = new TopicEntity();
                        topicEntity.setBroker(broker);
                        topicEntity.setQOS(0);//setting to 0 as in case of published message, qos will be set on message level
                        topicEntity.setType(1);
                        topicEntity.setName(topic);
                        messageEntity.setTopic(topicEntity);
                    }
                    data.insert(messageEntity)
                            .subscribeOn(Schedulers.single())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<MessageEntity>() {
                                @Override
                                public void accept(MessageEntity messageEntity) throws Exception {
                                    adapter.queryAsync();
                                    Log.i(PublishActivity.class.getName(),"Sending "+messageEntity.getId());
                                    mqttClients.publishMessage(broker,topic,message,Integer.parseInt(qos),retained, messageEntity.getId());
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Toast.makeText(getApplicationContext(), "Unknown error occurred!", Toast.LENGTH_SHORT).show();
                                    throwable.printStackTrace();
                                }
                            });
                }
            });
        }

        AdsHelper.initializeAds((AdView)findViewById(R.id.adView),this);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menu){
        switch(menu.getItemId()){
            case R.id.delete:
                if(adapter.toDelete!=null) {
                    try {
                        data.delete(adapter.toDelete)
                                .subscribeOn(Schedulers.single())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                adapter.queryAsync();
                                                mqttClients.unSubscribe(broker,adapter.toDelete.getName());
                                            }
                                        });
                                    }
                                });;
                    }
                    catch(RowCountException rce){
                        rce.printStackTrace();
                    }
                }
                break;
            default:
                super.onContextItemSelected(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        adapter.queryAsync();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        adapter.close();
        if(topicEntityIterator!=null) topicEntityIterator.close();
        super.onDestroy();
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
            return data.select(TopicEntity.class).where(TopicEntity.BROKER_ID.eq(brokerId).and(TopicEntity.TYPE.eq(1))).get();
        }

        @Override
        public void onBindViewHolder(final TopicEntity topic, BindingHolder<TopicListItemBinding> topicListItemBindingBindingHolder, int i) {
            List<MessageEntity> messageEntityList = data.select(MessageEntity.class).where(MessageEntity.TOPIC_ID.eq(topic.getId())).orderBy(MessageEntity.TIME_STAMP.desc()).limit(1).get().toList();
            topic.setUnreadCount(0);
            topic.setLatestMessage(messageEntityList.size()>0?messageEntityList.get(0):new MessageEntity());
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
                    inflater.inflate(R.menu.subscribe_topic_menu, menu);
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
}
