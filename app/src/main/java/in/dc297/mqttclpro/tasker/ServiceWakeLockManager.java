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
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * A simple utility class to provide static access to acquiring a WakeLock across receipt of Intent through
 * starting of Service.
 * <p>
 * This class is not thread-safe and is intended to only be called from the main thread.
 */
public final class ServiceWakeLockManager
{
    /**
     * Private instance of WakeLock that is lazily initialized the first time {@link #aquireLock(Context)} is
     * called.
     */
    private static WakeLock sWakeLock;

    /**
     * Acquire a WakeLock. Calls to this method should eventually be balanced by called to
     * {@link #releaseLock()}.
     *
     * @param c {@code Context} for obtaining a WakeLock. This parameter cannot be null.
     */
    public static void aquireLock(final Context context)
    {
        if (Constants.IS_CORRECT_THREAD_CHECKING_ENABLED)
        {
            if (!Thread.currentThread().equals(Looper.getMainLooper().getThread()))
            {
                throw new RuntimeException(
                                           String.format("Called from thread %s instead of main thread", Thread.currentThread().getName())); //$NON-NLS-1$
            }
        }

        if (null == sWakeLock)
        {
            sWakeLock =
                    ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                                                                 Constants.LOG_TAG);
            sWakeLock.setReferenceCounted(true);
        }

        sWakeLock.acquire();
    }

    /**
     * Release a WakeLock. This method can only be called after a call to {@link #aquireLock(Context)}.
     */
    public static void releaseLock()
    {
        if (Constants.IS_CORRECT_THREAD_CHECKING_ENABLED)
        {
            if (!Thread.currentThread().equals(Looper.getMainLooper().getThread()))
            {
                throw new RuntimeException(
                                           String.format("Called from thread %s instead of main thread", Thread.currentThread().getName())); //$NON-NLS-1$
            }
        }

        sWakeLock.release();
    }
}