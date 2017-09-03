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
import android.text.TextUtils;
import android.util.Log;

import net.dinglisch.android.tasker.TaskerPlugin;

import java.util.Locale;

import in.dc297.mqttclpro.Util;

public final class QueryReceiver extends BroadcastReceiver
{


    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (!in.dc297.mqttclpro.tasker.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction()))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG,
                      String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        final String topic = intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_TOPIC);
        final String message = intent.getExtras().getString(in.dc297.mqttclpro.tasker.Intent.EXTRA_MESSAGE);

        int messageID = TaskerPlugin.Event.retrievePassThroughMessageID(intent);
        if ( messageID == -1 ) {
            setResultCode(in.dc297.mqttclpro.tasker.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }
        BundleScrubber.scrub(intent);

        //final Bundle bundle = intent.getBundleExtra(in.dc297.mqttclpro.tasker.Intent.EXTRA_BUNDLE);

        final Bundle publishedBundle = TaskerPlugin.Event.retrievePassThroughData(intent);

        String  publishedTopic,publishedMessage;
        BundleScrubber.scrub(publishedBundle);

        if(publishedBundle!=null) {
            publishedTopic = publishedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_TOPIC);
            publishedMessage = publishedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
            if(!Util.mosquitto_topic_matches_sub(topic, publishedTopic)){
                setResultCode(in.dc297.mqttclpro.tasker.Intent.RESULT_CONDITION_UNSATISFIED);
                return;
            }
        }
        else{
            setResultCode(in.dc297.mqttclpro.tasker.Intent.RESULT_CONDITION_UNSATISFIED);
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG,
                        String.format(Locale.US, "No publish data in intent!")); //$NON-NLS-1$
            }
            return;
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG,"Received a query."); //$NON-NLS-1$
        }

        setResultCode(in.dc297.mqttclpro.tasker.Intent.RESULT_CONDITION_SATISFIED);
        if ( TaskerPlugin.Setting.hostSupportsVariableReturn( intent.getExtras() ) ) {
            Bundle vars = new Bundle();
            vars.putString( "%"+message, publishedMessage );
            Log.i("Query success","Returning var name "+message+" with value "+publishedMessage);
            TaskerPlugin.addVariableBundle( getResultExtras( true ), vars );
        }
        else{
            Log.i("Query success","Seems like host doesnt support variable setting");
        }
    }
}