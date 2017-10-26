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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import in.dc297.mqttclpro.R;

//import com.twofortyfouram.locale.api.R;

/**
 * Utility class to generate a breadcrumb title string for {@code Activity} instances in Locale.
 * <p>
 * This class cannot be instantiated.
 */
public final class BreadCrumber
{

    public static CharSequence generateBreadcrumb(final Context context, final Intent intent,
                                                  final String currentCrumb)
    {
        if (null == context)
        {
            throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
        }

        try
        {
            if (null == currentCrumb)
            {
                Log.w(Constants.LOG_TAG, "currentCrumb cannot be null"); //$NON-NLS-1$
                return ""; //$NON-NLS-1$
            }
            if (null == intent)
            {
                Log.w(Constants.LOG_TAG, "intent cannot be null"); //$NON-NLS-1$
                return currentCrumb;
            }

            /*
             * Note: this is vulnerable to a private serializable attack, but the try-catch will solve that.
             */
            final String breadcrumbString = intent.getStringExtra(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_STRING_BREADCRUMB);
            if (null != breadcrumbString)
            {
                return context.getString(R.string.twofortyfouram_locale_breadcrumb_format, breadcrumbString, context.getString(R.string.twofortyfouram_locale_breadcrumb_separator), currentCrumb);
            }
            return currentCrumb;
        }
        catch (final Exception e)
        {
            Log.e(Constants.LOG_TAG, "Encountered error generating breadcrumb", e); //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private BreadCrumber()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}