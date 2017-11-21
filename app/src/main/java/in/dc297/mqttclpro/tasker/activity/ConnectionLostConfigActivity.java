package in.dc297.mqttclpro.tasker.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.tasker.BreadCrumber;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

import static in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BUNDLE;

public class ConnectionLostConfigActivity extends AbstractPluginActivity {

    private ReactiveEntityStore<Persistable> data = null;
    private List<BrokerEntity> brokerEntityList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_lost_config);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setupTitleApi11();
        }
        else
        {
            setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),
                    getString(R.string.connection_lost_title)));
        }

        Bundle taskerBundle = getIntent().getBundleExtra(Intent.EXTRA_BUNDLE);
        Long brokerId = 0L;
        if(taskerBundle!=null){
            brokerId = taskerBundle.getLong(Intent.EXTRA_BROKER_ID);
        }

        data = ((MQTTClientApplication)getApplication()).getData();
        brokerEntityList = data.select(BrokerEntity.class).get().toList();
        if(brokerEntityList.size()<=0){
            Toast.makeText(getApplicationContext(),"Please add at least 1 broker before configuring tasker event.",Toast.LENGTH_SHORT).show();
            this.mIsCancelled = true;
            finish();
            return;
        }

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

        if(brokerId!=0){
            brokerSpinner.setSelection(selIndex_b);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupTitleApi11()
    {
        setTitle(getString(R.string.connection_lost_title));
    }

    @Override
    public void finish()
    {
        if (!isCanceled()) {
            Spinner brokerSpinner = (Spinner) findViewById(R.id.brokerSpinner);
            int brokerPosition = brokerSpinner.getSelectedItemPosition();
            Long brokerId = (brokerEntityList.size()>brokerPosition) ? brokerEntityList.get(brokerPosition).getId() : 0;
            final String brokerNickName = brokerSpinner.getSelectedItem()!=null ? brokerSpinner.getSelectedItem().toString() : "";
            if(brokerId<=0L){
                Toast.makeText(getApplicationContext(), "Invalid broker", Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle taskerBundle = new Bundle();
            taskerBundle.putLong(Intent.EXTRA_BROKER_ID, brokerId);
            taskerBundle.putString(Intent.QUERY_OPERATION,Intent.CONNECTION_LOST);

            final String blurb = generateBlurb(getApplicationContext(), brokerNickName);
            final android.content.Intent resultIntent = new android.content.Intent();
            resultIntent.putExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_STRING_BLURB, blurb);

            resultIntent.putExtra(EXTRA_BUNDLE,taskerBundle);

            setResult(RESULT_OK, resultIntent);
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
