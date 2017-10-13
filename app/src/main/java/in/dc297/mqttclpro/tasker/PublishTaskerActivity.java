package in.dc297.mqttclpro.tasker;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import java.util.ArrayList;

import in.dc297.mqttclpro.DBHelper;
import in.dc297.mqttclpro.R;

import static in.dc297.mqttclpro.tasker.Intent.EXTRA_BUNDLE;

public class PublishTaskerActivity extends AbstractPluginActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_tasker);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String topic = "";
        String message = "";
        String topicVar = "";

        Bundle taskerBundle = getIntent().getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null) {
            topic = taskerBundle.getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC);
            message = taskerBundle.getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE);
            topicVar = taskerBundle.getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC_VAR);
        }
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        Spinner topicSpinner = (Spinner) findViewById(R.id.editText);
        DBHelper dbHelper = new DBHelper(this.getApplicationContext());

        Cursor topicCursor = dbHelper.getTopics(0);
        if(topicCursor.getCount()==0){
            Toast.makeText(getApplicationContext(),"Please subscribe to at least 1 topic before configuring tasker event.",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String[] topics = new String[topicCursor.getCount()];
        topicCursor.moveToFirst();
        int i=0;
        int selIndex = 0;
        while(!topicCursor.isAfterLast()){
            topics[i] = topicCursor.getString(topicCursor.getColumnIndex("topic"));
            if(topics[i].equals(topic)) selIndex = i;
            i++;
            topicCursor.moveToNext();
        }

        // Creating adapter for spinner
        ArrayAdapter dataAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item,topics);

        // Drop down layout style - list view with radio button
        dataAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(dataAdapter);
        if (null == savedInstanceState)
        {
            if(message!=null && !"".equals(message)) {
                ((EditText) findViewById(R.id.editText2)).setText(message);
            }
            if(topic!=null && !"".equals(topic)) {
                topicSpinner.setSelection(selIndex);
            }
            if(topicVar!=null && !"".equals(topicVar)){
                ((EditText) findViewById(R.id.mqttTopicVar)).setText(topicVar);
            }
        }
    }

    @Override
    public void finish()
    {
        if (!isCanceled())
        {
            Spinner topicSpinner = (Spinner) findViewById(R.id.editText);
            if(topicSpinner!=null && topicSpinner.getSelectedItem()!=null) {
                final String topic = topicSpinner.getSelectedItem().toString();
                final String message = ((EditText) findViewById(R.id.editText2)).getText().toString();
                final String topicVar =  ((EditText) findViewById(R.id.mqttTopicVar)).getText().toString();

                if (message.length() > 0 && topic.length() > 0 && topicVar.length() >0) {
                    final Intent resultIntent = new Intent();
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                    Bundle taskerBundle = new Bundle();
                    taskerBundle.putString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC, topic);
                    taskerBundle.putString(in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE, message);
                    taskerBundle.putString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC_VAR,topicVar);

                    final String blurb = generateBlurb(getApplicationContext(), topic + " : " + message + " : " + topicVar);
                    resultIntent.putExtra(in.dc297.mqttclpro.tasker.Intent.EXTRA_STRING_BLURB, blurb);

                    resultIntent.putExtra(EXTRA_BUNDLE,taskerBundle);

                    setResult(RESULT_OK, resultIntent);
                }
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
