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

package in.dc297.mqttclpro.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

import in.dc297.mqttclpro.MQTTService;
import in.dc297.mqttclpro.MyIntentService;

public final class FireReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (!in.dc297.mqttclpro.tasker.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG,
                      String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        Log.i("firereceiver","Received a fire :D"+intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC));
        Intent newIntent = new Intent(context, MyIntentService.class);
        newIntent.putExtras(intent);
        newIntent.setAction(MQTTService.MQTT_PUBLISH);
        context.startService(newIntent);
        //BundleScrubber.scrub(intent);

        //final Bundle bundle = intent.getBundleExtra(in.dc297.mqttclpro.tasker.Intent.EXTRA_BUNDLE);
        //BundleScrubber.scrub(bundle);

        /*if (PluginBundleManager.isBundleValid(bundle))
        {
            final String message = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }*/
    }
}