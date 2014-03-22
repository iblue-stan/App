/*
 * Copyright (C) 2008 ZXing authors
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
import android.os.Bundle;

/**
 * The main settings activity.
 * 
 * <br/>
 * 該類對應界面上的設置界面，基本上每一項在設置中都能找到對應的配置項
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class PreferencesActivity extends Activity {
	
	public static final String KEY_DECODE_QR = "preferences_decode_QR";
	public static final String KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix";

	/**
	 * 使用自動對焦
	 */
	public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";

	/**
	 * 沒有持續關注（只使用標准對焦模式）,具體參考CameraConfigurationManager.setDesiredCameraParameters()方法中對應邏輯
	 */
	public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";

	// public static final String KEY_DISABLE_EXPOSURE =
	// "preferences_disable_exposure";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();
	}

}
