package in.dc297.mqttclpro.activity;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.angads25.filepicker.view.FilePickerPreference;

import java.util.function.Consumer;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;
import in.dc297.mqttclpro.mqtt.internal.Util;
import in.dc297.mqttclpro.preferences.MyBrokerPreferences;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class AddEditBrokersActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    public static final String EXTRA_BROKER_ID = "EXTRA_BROKER_ID";

    private MyBrokerPreferences mBindablepreferences;

    private BrokerEntity broker;

    private ReactiveEntityStore<Persistable> data;

    private MQTTClients mqttClients = null;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mBindablepreferences;
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0
                        ? listPreference.getEntries()[index]
                        : null);
            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }
            else if (preference instanceof FilePickerPreference){
                if (TextUtils.isEmpty(stringValue) && "ClientP12Crt".equals(preference.getKey())) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(preference.getContext().getString(R.string.client_p12_desc));

                }
                else{
                    preference.setSummary(stringValue.split(":")[0]);
                }
                SharedPreferences preferences = preference.getSharedPreferences();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(preference.getKey(), stringValue.split(":")[0]);
                editor.commit();
            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        data = ((MQTTClientApplication) getApplication()).getData();
        mqttClients = MQTTClients.getInstance((MQTTClientApplication)getApplication());
        long brokerId = getIntent().getLongExtra(EXTRA_BROKER_ID,-1);

        if(brokerId!=-1) {
            Log.i(AddEditBrokersActivity.class.getName(),"Right about now");
            data.findByKey(BrokerEntity.class,brokerId)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new io.reactivex.functions.Consumer<BrokerEntity>() {
                        @Override
                        public void accept(BrokerEntity brokerEntity) throws Exception {
                            broker = brokerEntity;
                            mBindablepreferences = new MyBrokerPreferences(broker);
                            getFragmentManager().beginTransaction().add(android.R.id.content, new GeneralPreferenceFragment()).commit();
                            setTitle(broker.getNickName() + " - Edit");
                        }
                    }, new io.reactivex.functions.Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            if(throwable!=null) throwable.printStackTrace();
                        }
                    });
        }
        else{
            broker = new BrokerEntity();
            mBindablepreferences = new MyBrokerPreferences(broker);
        }
    }



    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
/*    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers_broker, target);
    }
*/
    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static void removePreference(Preference preference){
        SharedPreferences preferences = preference.getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(preference.getKey());
        editor.commit();
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_edit_brokers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.save:
                broker = mBindablepreferences.getBroker();
                if(TextUtils.isEmpty(broker.getPort()) || TextUtils.isEmpty(broker.getNickName()) || TextUtils.isEmpty(broker.getClientId()) || !Util.isHostValid(broker.getHost())){
                    Toast.makeText(getApplicationContext(), "Invalid host or port or nickname or client ID.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                Single<BrokerEntity> single = broker.getId() == 0 ? data.insert(broker) : data.update(broker);
                single.subscribeOn(Schedulers.single())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new io.reactivex.functions.Consumer<BrokerEntity>() {
                                       @Override
                                       public void accept(BrokerEntity brokerEntity) throws Exception {
                                           if(brokerEntity!=null) mqttClients.addBroker(brokerEntity);
                                           finish();
                                       }
                                   },
                                new io.reactivex.functions.Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        if(throwable!=null)throwable.printStackTrace();
                                    }
                                });
                break;
            case R.id.cancel:
                finish();
                break;
            case R.id.delete:
                if(broker.getId() != 0){
                    data.delete(broker);
                    mqttClients.removeBroker(broker);
                    Completable cDelete = broker.getId() == 0 ? null : data.delete(broker);
                    if(cDelete!=null) {
                        cDelete.subscribeOn(Schedulers.single())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mqttClients.removeBroker(broker);
                                                finish();
                                            }
                                        });
                                    }
                                });
                    }
                    else {
                        finish();
                    }
                }

        }
        return true;
    }
    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general_broker);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("ssl_switch"));
            bindPreferenceSummaryToValue(findPreference("Host"));
            bindPreferenceSummaryToValue(findPreference("Port"));
            bindPreferenceSummaryToValue(findPreference("Username"));
            bindPreferenceSummaryToValue(findPreference("Password"));
            bindPreferenceSummaryToValue(findPreference("CACrt"));
            bindPreferenceSummaryToValue(findPreference("ClientCrt"));
            bindPreferenceSummaryToValue(findPreference("ClientKey"));
            bindPreferenceSummaryToValue(findPreference("ClientP12Crt"));
            bindPreferenceSummaryToValue(findPreference("LastWillTopic"));
            bindPreferenceSummaryToValue(findPreference("LastWillMessage"));
            bindPreferenceSummaryToValue(findPreference("LastWillQOS"));
            bindPreferenceSummaryToValue(findPreference("NickName"));

            final FilePickerPreference fileDialog = (FilePickerPreference) findPreference("CACrt");
            fileDialog.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            fileDialog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!"".equals(PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""))) removePreference(preference);
                    fileDialog.onPreferenceClick(preference);
                    return false;
                }
            });

            final FilePickerPreference fileDialog1 = (FilePickerPreference) findPreference("ClientCrt");
            fileDialog1.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            fileDialog1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!"".equals(PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""))) removePreference(preference);
                    fileDialog1.onPreferenceClick(preference);
                    return false;
                }
            });

            final FilePickerPreference fileDialog2 = (FilePickerPreference) findPreference("ClientKey");
            fileDialog2.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            fileDialog2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!"".equals(PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""))) removePreference(preference);
                    fileDialog2.onPreferenceClick(preference);
                    return false;
                }
            });

            final FilePickerPreference fileDialog3 = (FilePickerPreference) findPreference("ClientP12Crt");
            fileDialog3.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            fileDialog3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!"".equals(PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""))) removePreference(preference);
                    fileDialog3.onPreferenceClick(preference);
                    return false;
                }
            });

        }


        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), AddEditBrokersActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}
