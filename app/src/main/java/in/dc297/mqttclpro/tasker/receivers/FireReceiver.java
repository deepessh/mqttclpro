/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package in.dc297.mqttclpro.tasker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Locale;

import in.dc297.mqttclpro.tasker.Constants;

import static in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BUNDLE;
import static in.dc297.mqttclpro.tasker.activity.Intent.MQTT_PUBLISH_ACTION;

public final class FireReceiver extends BroadcastReceiver
{

    private static final String LOG_TAG = "tasker.FireReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (!in.dc297.mqttclpro.tasker.activity.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG,
                      String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }
        Intent newIntent = new Intent(context, in.dc297.mqttclpro.tasker.services.MyIntentService.class);
        newIntent.putExtras(intent);

        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null) {
            String intentAction = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.ACTION_OPERATION);

            if (intentAction != null) {
                Log.i(FireReceiver.class.getName(), "Received fire from tasker " + intentAction);
                newIntent.setAction(intentAction);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(newIntent);
                }
                else{
                    context.startService(newIntent);
                }
            }
        }
    }
}