/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.madmatrix.zxing.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Finishes an activity after a period of inactivity if the device is on battery
 * power. <br/><br/>
 * 
 * 該活動監控器全程監控掃描活躍狀態，與CaptureActivity生命周期相同
 */
final class InactivityTimer {

	private static final String TAG = InactivityTimer.class.getSimpleName();

	/**
	 * 如果在30min內掃描器沒有被使用過，則自動finish掉activity
	 */
	private static final long INACTIVITY_DELAY_MS = 30 * 60 * 1000L;

	/**
	 * 在本app中，此activity即為CaptureActivity
	 */
	private final Activity activity;
	/**
	 * 接受系統廣播：手機是否連通電源
	 */
	private final BroadcastReceiver powerStatusReceiver;
	private boolean registered;
	private AsyncTask<?, ?, ?> inactivityTask;

	InactivityTimer(Activity activity) {
		this.activity = activity;
		powerStatusReceiver = new PowerStatusReceiver();
		registered = false;
		onActivity();
	}

	/**
	 * 首先終止之前的監控任務，然後新起一個監控任務
	 */
	@SuppressWarnings("unchecked")
	synchronized void onActivity() {
		cancel();
		inactivityTask = new InactivityAsyncTask();
		inactivityTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public synchronized void onPause() {
		cancel();
		if (registered) {
			activity.unregisterReceiver(powerStatusReceiver);
			registered = false;
		} else {
			//Log.w(TAG, "PowerStatusReceiver was never registered?");
		}
	}

	public synchronized void onResume() {
		if (registered) {
			//Log.w(TAG, "PowerStatusReceiver was already registered?");
		} else {
			activity.registerReceiver(powerStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			registered = true;
		}
		onActivity();
	}

	/**
	 * 取消監控任務
	 */
	private synchronized void cancel() {
		AsyncTask<?, ?, ?> task = inactivityTask;
		if (task != null) {
			task.cancel(true);
			inactivityTask = null;
		}
	}

	void shutdown() {
		cancel();
	}

	/**
	 * 監聽是否連通電源的系統廣播。如果連通電源，則停止監控任務，否則重啟監控任務
	 */
	private final class PowerStatusReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
				// 0 indicates that we're on battery
				boolean onBatteryNow = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0;
				if (onBatteryNow) {
					InactivityTimer.this.onActivity();
				} else {
					InactivityTimer.this.cancel();
				}
			}
		}
	}

	/**
	 * 該任務很簡單，就是在INACTIVITY_DELAY_MS時間後終結activity
	 */
	private final class InactivityAsyncTask extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... objects) {
			try {
				Thread.sleep(INACTIVITY_DELAY_MS);
//				Log.i(TAG, "Finishing activity due to inactivity");
				activity.finish();
			} catch (InterruptedException e) {
				// continue without killing
			}
			return null;
		}
	}

}
