package in.dc297.mqttclpro.tasker.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.dialog.AddTopicDialogFragment;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.entity.Topic;
import in.dc297.mqttclpro.tasker.BreadCrumber;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

import static in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BUNDLE;

public class ConfigureTaskerEventActivity extends AbstractPluginActivity {

    private ReactiveEntityStore<Persistable> data = null;
    private List<BrokerEntity> brokerEntityList = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_tasker_event);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setupTitleApi11();
        }
        else
        {
            setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),
                    getString(R.string.message_arrived)));
        }
        String topic = "";
        String message = "";
        String topicVar = "";
        long brokerId = 0;
        int topicComparator = 0;
        int messageComparator = 0;
        String topicToCompare = "";
        String messageToCompare = "";

        Bundle taskerBundle = getIntent().getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null) {
            brokerId = taskerBundle.getLong(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID);
            topic = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC);
            message = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE);
            topicVar = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC_VAR);
            topicComparator = taskerBundle.getInt(Intent.EXTRA_TOPIC_COMPARATOR);
            messageComparator = taskerBundle.getInt(Intent.EXTRA_MESSAGE_COMPARATOR);
            topicToCompare = taskerBundle.getString(Intent.EXTRA_TOPIC_COMPARE_TO);
            messageToCompare = taskerBundle.getString(Intent.EXTRA_MESSAGE_COMPARE_TO);
        }

        final String topicComp = topic;
        final long brokerIdComp = brokerId;

        data = ((MQTTClientApplication)getApplication()).getData();
        brokerEntityList = data.select(BrokerEntity.class).get().toList();
        if(brokerEntityList.size()<=0){
            Toast.makeText(getApplicationContext(),"Please add at least 1 broker before configuring tasker event.",Toast.LENGTH_SHORT).show();
            this.mIsCancelled = true;
            finish();
            return;
        }
        //comparators stuff starts
        final Spinner topicComparatorSpinner = (Spinner)findViewById(R.id.topicComparatorSpinner);
        ArrayAdapter dataAdapterTopicComparator = ArrayAdapter.createFromResource(this, R.array.comparators_array, R.layout.simple_spinner_item_black);
        dataAdapterTopicComparator.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_black);

        final Button topicComparatorButton = (Button) findViewById(R.id.topicComparatorButton);
        topicComparatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topicComparatorSpinner.performClick();
            }
        });

        topicComparatorSpinner.setAdapter(dataAdapterTopicComparator);
        final String[] comparatorValues = getResources().getStringArray(R.array.comparators_array_method);
        topicComparatorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                topicComparatorButton.setText(comparatorValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i(ConfigureTaskerEventActivity.class.getName(),"Nothing selected");
            }
        });

        final Spinner messageComparatorSpinner = (Spinner)findViewById(R.id.messageComparatorSpinner);
        ArrayAdapter dataAdapterMessageComparator = ArrayAdapter.createFromResource(this, R.array.comparators_array, R.layout.simple_spinner_item_black);
        dataAdapterMessageComparator.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_black);

        final Button messageComparatorButton = (Button) findViewById(R.id.messageComparatorButton);
        messageComparatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageComparatorSpinner.performClick();
            }
        });

        messageComparatorSpinner.setAdapter(dataAdapterMessageComparator);
        messageComparatorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                messageComparatorButton.setText(comparatorValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i(ConfigureTaskerEventActivity.class.getName(),"Nothing selected");
            }
        });
        //comparators stuff end
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

        final  Spinner topicSpinner = (Spinner) findViewById(R.id.editText);
        /*int i_t = 0;
        int selIndex_t = 0;
        Iterator<Topic> topicIterator = brokerEntityList.get(selIndex_b).getTopics().iterator();
        List<Topic> topicList = new ArrayList<Topic>();
        while(topicIterator.hasNext()){
            Topic topic1 = topicIterator.next();
            if(topic1.getType()==0){
                topicList.add(topic1);
            }
        }

        if(topicList.size()==0){
            Toast.makeText(getApplicationContext(),"No Topics added for this broker",Toast.LENGTH_SHORT).show();
        }
        String[] topics = new String[topicList.size()];
        for(Topic topic1 : topicList){
            topics[i_t] = topic1.getName();
            if(topic1.getName().equals(topic)) selIndex_t = i_t;
            i_t++;
        }*/

        brokerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Iterator<Topic>  topicIterator1 = brokerEntityList.get(position).getTopics().iterator();
                List<Topic> topicEntityList = new ArrayList<Topic>();
                while(topicIterator1.hasNext()){
                    Topic topic1 = topicIterator1.next();
                    if(topic1.getType()==0){
                        topicEntityList.add(topic1);
                    }
                }
                if(topicEntityList.size()==0){
                    Toast.makeText(getApplicationContext(),"Please subscribe to at least one topic for this broker to setup a tasker event.",Toast.LENGTH_LONG).show();
                    AddTopicDialogFragment addTopicDialogFragment = new AddTopicDialogFragment();
                    addTopicDialogFragment.setBrokerId(brokerEntityList.get(position).getId());
                    addTopicDialogFragment.show(getFragmentManager(),"ADD_TOPIC_DIALOG");
                }
                String[] topics = new String[topicEntityList.size()];
                int i_t=0;
                int selIndex_t = 0;
                for(Topic topic1 : topicEntityList){
                    topics[i_t] = topic1.getName();
                    if(brokerEntityList.get(position).getId() == brokerIdComp && topic1.getName().equals(topicComp)){
                        selIndex_t = i_t;
                    }
                    i_t++;
                }
                // Creating adapter for spinner
                ArrayAdapter dataAdapter = new ArrayAdapter(getApplicationContext(),
                        R.layout.simple_spinner_item_black,topics);

                // Drop down layout style - list view with radio button
                dataAdapter
                        .setDropDownViewResource(R.layout.simple_spinner_dropdown_item_black);
                topicSpinner.setAdapter(dataAdapter);
                topicSpinner.setSelection(selIndex_t);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getApplicationContext(), "Some magic is going on!",Toast.LENGTH_SHORT).show();
            }
        });

        if (null == savedInstanceState)
        {
            if(message!=null && !"".equals(message)) {
                ((EditText) findViewById(R.id.editText2)).setText(message);
            }
            if(topicVar!=null && !"".equals(topicVar)){
                ((EditText) findViewById(R.id.mqttTopicVar)).setText(topicVar);
            }
            if(brokerId!=0){
                brokerSpinner.setSelection(selIndex_b);
            }
            topicComparatorSpinner.setSelection(topicComparator);
            messageComparatorSpinner.setSelection(messageComparator);
            ((EditText) findViewById(R.id.topicComparatorEditText)).setText(topicToCompare);
            ((EditText) findViewById(R.id.messageComparatorEditText)).setText(messageToCompare);
            /*if(topic!=null && !"".equals(topic)) {
                topicSpinner.setSelection(selIndex_t);
            }*/
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupTitleApi11()
    {
        setTitle(getString(R.string.message_arrived));
    }

    @Override
    public void finish()
    {
        if (!isCanceled())
        {
            Spinner topicSpinner = (Spinner) findViewById(R.id.editText);
            Spinner brokerSpinner = (Spinner) findViewById(R.id.brokerSpinner);
            Spinner topicComparatorSpinner = (Spinner) findViewById(R.id.topicComparatorSpinner);
            Spinner messageComparatorSpinner = (Spinner) findViewById(R.id.messageComparatorSpinner);

            if(topicSpinner!=null && topicSpinner.getSelectedItem()!=null && brokerSpinner!=null && brokerSpinner.getSelectedItem()!=null) {
                final String topic = topicSpinner.getSelectedItem().toString();
                final String message = ((EditText) findViewById(R.id.editText2)).getText().toString();
                final String topicVar =  ((EditText) findViewById(R.id.mqttTopicVar)).getText().toString();
                final String brokerNickName = brokerSpinner.getSelectedItem().toString();
                final long brokerId = brokerEntityList.get(brokerSpinner.getSelectedItemPosition()).getId();
                final String topicToCompareTo = ((EditText)findViewById(R.id.topicComparatorEditText)).getText().toString();
                final String messageToCompareTo = ((EditText)findViewById(R.id.messageComparatorEditText)).getText().toString();
                final String topicCompBlurb = TextUtils.isEmpty(topicToCompareTo)?"":"Topic " + topicComparatorSpinner.getSelectedItem() + " " + topicToCompareTo;
                final String messageCompBlurb = TextUtils.isEmpty(topicToCompareTo)?"":"Message " + messageComparatorSpinner.getSelectedItem() + " " + messageToCompareTo;

                if(TextUtils.isEmpty(topic)){
                    Toast.makeText(getApplicationContext(), "Please subscribe to at least one topic for this broker to setup a tasker event!", Toast.LENGTH_LONG).show();
                    return;
                }

                if(brokerId<=0){
                    Toast.makeText(getApplicationContext(), "Invalid broker", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(message)){
                    Toast.makeText(getApplicationContext(), "Invalid message variable", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(topicVar)){
                    Toast.makeText(getApplicationContext(), "Invalid topic variable", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (message.length() > 0 && topic.length() > 0 && topicVar.length() >0) {
                    final android.content.Intent resultIntent = new android.content.Intent();
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                    Bundle taskerBundle = new Bundle();
                    taskerBundle.putString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC, topic);
                    taskerBundle.putString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE, message);
                    taskerBundle.putString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC_VAR,topicVar);
                    taskerBundle.putLong(Intent.EXTRA_BROKER_ID, brokerId);
                    taskerBundle.putInt(Intent.EXTRA_TOPIC_COMPARATOR,topicComparatorSpinner.getSelectedItemPosition());
                    taskerBundle.putInt(Intent.EXTRA_MESSAGE_COMPARATOR,messageComparatorSpinner.getSelectedItemPosition());
                    taskerBundle.putString(Intent.EXTRA_TOPIC_COMPARE_TO,topicToCompareTo);
                    taskerBundle.putString(Intent.EXTRA_MESSAGE_COMPARE_TO,messageToCompareTo);
                    taskerBundle.putString(Intent.QUERY_OPERATION,Intent.MESSAGE_ARRIVED);

                    final String blurb = generateBlurb(getApplicationContext(), brokerNickName + " : " + topic + " : " + message + " : " + topicVar + " : " + topicCompBlurb + " : " + messageCompBlurb);
                    resultIntent.putExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_STRING_BLURB, blurb);

                    resultIntent.putExtra(EXTRA_BUNDLE,taskerBundle);

                    setResult(RESULT_OK, resultIntent);
                }
            }
            else{
                //broker can't be invalid
                Toast.makeText(getApplicationContext(), "Please subscribe to at least one topic for this broker to setup a tasker event!", Toast.LENGTH_LONG).show();
                return;
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
