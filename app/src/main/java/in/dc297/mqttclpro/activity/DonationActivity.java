package in.dc297.mqttclpro.activity;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import org.sufficientlysecure.donations.DonationsFragment;

import in.dc297.mqttclpro.BuildConfig;
import in.dc297.mqttclpro.R;


public class DonationActivity extends AppCompatActivity {

    public static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApicSu8e8x5MitX+Nh8MJVyDpoAJE8fF5023KrYe7bubRclewZgAJnXR5dvif0ttTC2qKgxa0bih4Nxi22bOao8Pza8H9Obl2BMYWk/fKpBo8XumLX+s+U4cxU661hTHocQXcUZ27BOVHDMiN3jwxWzLBNiDHwF/yi2Qre5RdJ0wUhesGq1hCmGUJhe3pK8N1MYBAl14HOGuXkVj+qtdQUl4I6FgD2f6X3pgQwpLZkQ8wcEMu5egLgPbsWCuWMhDe6ine22OFnom/1/STXJPQPsjqXI1/euYs9F5EIoMfRnabDnmdFq7gfWli6b67LWoJDaI0HTcjy958yvQD8rBKtwIDAQAB";
    public static final String[] GOOGLE_CATALOG = new String[]{"ntpsync.donation.1", "ntpsync.donation.3", "ntpsync.donation.5", "ntpsync.donation.13",
            "ntpsync.donation.20"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DonationsFragment donationsFragment;
        donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                getResources().getStringArray(R.array.donation_google_catalog_values));
        ft.replace(R.id.donations_activity_container, donationsFragment, "donationsFragment");
        ft.commit();
        setTitle(getResources().getString(R.string.action_donate));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_donation, menu);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("donationsFragment");
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
