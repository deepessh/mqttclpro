package in.dc297.mqttclpro.helpers;

import android.content.Context;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.sufficientlysecure.donations.google.util.IabHelper;
import org.sufficientlysecure.donations.google.util.IabResult;
import org.sufficientlysecure.donations.google.util.Inventory;


import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.DonationActivity;

public class AdsHelper {

    private static IabHelper mIabHelper;

    public static void initializeAds(final AdView mAdView, final Context context){
        mIabHelper = new IabHelper(context, DonationActivity.GOOGLE_PUBKEY);
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if(result.isSuccess()){
                    mIabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            boolean showAd= true;
                            if(result.isSuccess()){
                                for(String v : DonationActivity.GOOGLE_CATALOG){
                                    if(inv.hasDetails(v)) showAd = false;
                                }
                            }
                            if(showAd){
                                String admob_id = context.getString(R.string.admob_id);
                                AdRequest adRequest = new AdRequest.Builder().addTestDevice("EDCFC931B9C2209977916BD1354384EA").addTestDevice("97E166D68E02F1F3D5D16535ED291293").build();
                                MobileAds.initialize(context, admob_id);
                                mAdView.loadAd(adRequest);
                            }
                        }
                    });
                }
            }
        });
    }
}
