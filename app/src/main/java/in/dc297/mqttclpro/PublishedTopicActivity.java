package in.dc297.mqttclpro;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class PublishedTopicActivity extends AppCompatActivity {

    private DBHelper db;
    private String topic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_published_topic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        db = new DBHelper(getApplicationContext());
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        topic = null;
        Intent pubTopInt = getIntent();
        if(pubTopInt!=null){
            topic = pubTopInt.getStringExtra("topic");
        }

        if(topic==null || topic.equals("")) finish();

        setTitle("'"+topic+"' Published Messages");
        ListView pubTopsLV = (ListView)findViewById(R.id.pub_tops_lv);
        String[] from = new String[] { "message", "timestamp","display_topic"};
        int[] to = new int[]{R.id.mmessage_tv,R.id.mtimestamp_tv,R.id.mtopic_tv};
        MessagesListAdapter pubmlvs = new MessagesListAdapter(getApplicationContext(),R.layout.messages_list_item,db.getMessages(topic,1),from,to,0);
        pubTopsLV.setAdapter(pubmlvs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            db.deleteMessages(topic,1);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
