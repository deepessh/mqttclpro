package in.dc297.mqttclpro;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class SubscribeActivity extends AppCompatActivity {

    private DBHelper db = null;
    private ArrayList<TopicsListViewModel> tlvmal = null;
    ListView topicsLv = null;
    SubscribedTopicsListAdapter topicsLVAdapter = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        db = new DBHelper(getApplicationContext());
        topicsLv = (ListView) findViewById(R.id.subsctibeTopicListView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        String[] from = new String[] { "topic", "count","message","timest"};
        int[] to = new int[]{R.id.topic_tv, R.id.message_count,R.id.message_tv,R.id.timestamp_tv };
        topicsLVAdapter = new SubscribedTopicsListAdapter(this,R.layout.subscribed_topics_list_item,db.getTopics(0),from,to,0);
        topicsLv.setAdapter(topicsLVAdapter);

        final EditText topicEdit = (EditText) findViewById(R.id.subscribeTopicEditText);
        if (topicEdit != null) {
            topicEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    String topic = topicEdit.getText().toString();
                    if (topic != null && !topic.equals("")) {
                        int atret = db.addTopic(topic, 0);
                        switch (atret) {
                            case 0:
                                //Snackbar.make(v, "Topic successfully added.", Snackbar.LENGTH_LONG)
                                //        .setAction("Action", null).show();
                                topicsLVAdapter.swapCursor(db.getTopics(0));
                                topicEdit.setText("");
                                break;
                            case 1:
                                Snackbar.make(v, "Topic already added.", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            default:
                                Snackbar.make(v, "Unknown error Occurred.", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                        }
                    }
                    return false;
                }
            });
            //tlvmal = db.getTopics(0);

            //topicsLv.setAdapter();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
