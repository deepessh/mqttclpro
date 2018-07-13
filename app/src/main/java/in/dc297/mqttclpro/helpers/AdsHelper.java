package in.dc297.mqttclpro.helpers;

import android.app.Activity;

import android.util.Log;
import android.view.View;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;

import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;



import java.util.Arrays;
import java.util.List;

import in.dc297.mqttclpro.BuildConfig;
import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.DonationActivity;
import in.dc297.mqttclpro.billing.BillingManager;

public class AdsHelper {

    private static BillingManager mBillingManager;

    private static Activity mActivity;
    private static AdView mAdView;
    private static BillingManager getmBillingManager(){
        return mBillingManager;
    }

    public static void initializeAds(final AdView AdView, final Activity activity){
        mBillingManager = new BillingManager(activity, new UpdateListener());
        mActivity = activity;
        mAdView = AdView;
    }

    private static void checkPurchasesAndInitializeAds(){
        BillingManager billingManager = getmBillingManager();
        billingManager.queryPurchasesAsync(new PurchaseHistoryResponseListener() {
            @Override
            public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchasesList) {
                boolean showAd= true;
                if(purchasesList==null || purchasesList.size()>0){
                    showAd = false;
                }
                if(showAd){
                    String admob_id = mActivity.getApplicationContext().getString(R.string.admob_id);
                    AdRequest adRequest = new AdRequest.Builder()
                            .addTestDevice("EDCFC931B9C2209977916BD1354384EA")
                            .addTestDevice("97E166D68E02F1F3D5D16535ED291293")
                            .addTestDevice("59013680902FB8A4B58E6DB45722D086")
                            .build();
                    MobileAds.initialize(mActivity.getApplicationContext(), admob_id);
                    mAdView.loadAd(adRequest);
                }
                else{
                    mAdView.setVisibility(View.GONE);
                }
            }
        });
    }
    /**
     * Handler to billing updates
     */
    private static class UpdateListener implements BillingManager.BillingUpdatesListener {
        @Override
        public void onBillingClientSetupFinished() {
                checkPurchasesAndInitializeAds();
        }

        @Override
        public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {
        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchaseList) {
        }
    }


}
