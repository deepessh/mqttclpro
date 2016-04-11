package in.dc297.mqttclpro;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

public class PublishedTopicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_published_topic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DBHelper db = new DBHelper(getApplicationContext());
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String topic = null;
        Intent pubTopInt = getIntent();
        if(pubTopInt!=null){
            topic = pubTopInt.getStringExtra("topic");
        }

        if(topic==null || topic.equals("")) finish();

        setTitle("'"+topic+"' Published Messages");
        ListView pubTopsLV = (ListView)findViewById(R.id.pub_tops_lv);
        String[] from = new String[] { "message", "timestamp"};
        int[] to = new int[]{R.id.mmessage_tv,R.id.mtimestamp_tv};
        MessagesListAdapter pubmlvs = new MessagesListAdapter(getApplicationContext(),R.layout.messages_list_item,db.getMessages(topic,1),from,to,0);
        pubTopsLV.setAdapter(pubmlvs);
    }

}
