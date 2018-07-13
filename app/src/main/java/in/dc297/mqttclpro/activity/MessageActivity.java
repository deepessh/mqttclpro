package in.dc297.mqttclpro.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.databinding.MessagesListItemBinding;
import in.dc297.mqttclpro.entity.MessageEntity;
import in.dc297.mqttclpro.entity.TopicEntity;
import in.dc297.mqttclpro.helpers.AdsHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;

public class MessageActivity extends AppCompatActivity {

    private ReactiveEntityStore<Persistable> data;
    private TopicEntity topic;
    private ExecutorService executor;
    private MessagesListAdapter adapter;
    private long topicId;

    public static final String EXTRA_TOPIC_ID = "EXTRA_TOPIC_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        data = ((MQTTClientApplication)getApplication()).getData();
        topicId = getIntent().getLongExtra(EXTRA_TOPIC_ID,-1);
        if(topicId==-1) {
            Toast.makeText(getApplicationContext(),"Unknown Error!",Toast.LENGTH_SHORT).show();
            finish();
        }
        data.findByKey(TopicEntity.class,topicId)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<TopicEntity>() {
                    @Override
                    public void accept(TopicEntity topicEntity) throws Exception {
                        topic = topicEntity;
                        if(topic.getType()==0) {
                            data.update(MessageEntity.class)
                                    .set(MessageEntity.READ, 1)
                                    .where(
                                            MessageEntity.TOPIC.eq(topic)
                                                    .and(MessageEntity.READ.eq(0)))
                                    .get().single()
                                    .subscribeOn(Schedulers.single())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe();
                        }
                        setTitle(topic.getName() + " - " + (topic.getType()==0?"Received":"Published")+" messages");
                    }
                });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        executor = Executors.newSingleThreadExecutor();
        adapter = new MessagesListAdapter();
        adapter.setExecutor(executor);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        data.count(MessageEntity.class).where(MessageEntity.TOPIC_ID.eq(topicId)).get().single()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        if (integer == 0) {
                            Toast.makeText(getApplicationContext(), "No message received/published for this topic!",Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
        AdsHelper.initializeAds((AdView)findViewById(R.id.adView),this);
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
    public boolean onContextItemSelected(MenuItem menu){
        switch(menu.getItemId()){
            case R.id.delete:
                if(adapter.toDelete!=null) {
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
                                        }
                                    });
                                }
                            });
                }
                break;
            default:
                super.onContextItemSelected(menu);
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
        super.onDestroy();
    }

    public class MessagesListAdapter extends QueryRecyclerAdapter<MessageEntity, BindingHolder<MessagesListItemBinding>> implements View.OnClickListener {

        public MessageEntity toDelete;

        MessagesListAdapter(){
            super(MessageEntity.$TYPE);
        }
        @Override
        public void onClick(View v) {
            MessagesListItemBinding binding = (MessagesListItemBinding) v.getTag();
            if(binding!=null){
                /*Intent intent = new Intent(v.getContext(),SubscribedTopicsActivity.class);
                intent.putExtra(SubscribedTopicsActivity.EXTRA_BROKER_ID,binding.getTopic().getId());
                //Toast.makeText(v.getContext(),binding.getBroker().toString(),Toast.LENGTH_SHORT).show();
                startActivity(intent);*/
            }
        }

        @Override
        public Result<MessageEntity> performQuery() {
            return data.select(MessageEntity.class).where(MessageEntity.TOPIC_ID.eq(topicId)).orderBy(MessageEntity.TIME_STAMP.desc()).get();
        }

        @Override
        public void onBindViewHolder(final MessageEntity message, BindingHolder<MessagesListItemBinding> messageListItemBindingBindingHolder, int i) {
            messageListItemBindingBindingHolder.binding.setMessage(message);
        }

        @Override
        public BindingHolder<MessagesListItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater  = LayoutInflater.from(parent.getContext());
            final MessagesListItemBinding binding = MessagesListItemBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    MenuInflater inflater = getMenuInflater();
                    inflater.inflate(R.menu.subscribe_topic_menu, menu);
                    toDelete = (MessageEntity) binding.getMessage();
                }
            });
            return new BindingHolder<>(binding);
        }
    }
}
