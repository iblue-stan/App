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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.madmatrix.zxing.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * 
 * 此Activity所做的事： 1. 開啟camera，在後台獨立線程中完成掃描任務； 2.
 * 繪製一個掃描區（viewfinder）來幫助用戶將條碼置於其中以準確掃描； 3. 掃描成功後會將掃描結果顯示在界面上。
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class CaptureActivity extends Activity implements SurfaceHolder.Callback {

	// ****************************************************************************
	private ProgressDialog pDialog;
	private final String URI_CLOCK_IN = "http://flash60905.qov.tw/test.php";
	public static final int REFRESH_DATA = 0x00000001;

	// 掃描條碼的結果顯示
	@SuppressLint("HandlerLeak")
	Handler myHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case REFRESH_DATA:
				String result = null;
				//看msg.obj是否為String的實例
				if (msg.obj instanceof String) {
					result = (String) msg.obj;
				}
				if (result != null) {
					// Internet的回傳值
					/*
					 * Toast.makeText(getApplicationContext(), result,
					 * Toast.LENGTH_SHORT).show();
					 */
					//顯示打卡是否成功
					showMyToast(result);
				}
				break;
			}

		};
	};
	// ****************************************************************************

//	private static final String TAG = CaptureActivity.class.getSimpleName();

	private static final long BULK_MODE_SCAN_DELAY_MS = 3000L;//下一次掃描的延遲

	private CameraManager cameraManager;

	private CaptureActivityHandler handler;

	private ViewfinderView viewfinderView;

	/**
	 * 掃描提示，例如"請將條碼置於取景框內掃描"之類的提示
	 */
	private TextView statusView;

	/**
	 * 掃描結果展示窗口
	 */
	private View resultView;

	private Result lastResult;

	private boolean hasSurface;

	private IntentSource source;

	/**
	 * 【輔助解碼的參數(用作MultiFormatReader的參數)】 編碼類型，該參數告訴掃描器采用何種編碼方式解碼，即EAN-13，QR
	 * Code等等 對應於DecodeHintType.POSSIBLE_FORMATS類型
	 * 參考DecodeThread構造函數中如下代碼：hints.put(DecodeHintType.POSSIBLE_FORMATS,
	 * decodeFormats);
	 */
	private Collection<BarcodeFormat> decodeFormats;

	/**
	 * 【輔助解碼的參數(用作MultiFormatReader的參數)】 字符集，告訴掃描器該以何種字符集進行解碼
	 * 對應於DecodeHintType.CHARACTER_SET類型
	 * 參考DecodeThread構造器如下代碼：hints.put(DecodeHintType.CHARACTER_SET,
	 * characterSet);
	 */
	private String characterSet;

	/**
	 * 【輔助解碼的參數(用作MultiFormatReader的參數)】 該參數最終會傳入MultiFormatReader，
	 * 上面的decodeFormats和characterSet最終會先加入到decodeHints中 最終被設置到MultiFormatReader中
	 * 參考DecodeHandler構造器中如下代碼：multiFormatReader.setHints(hints);
	 */
	private Map<DecodeHintType, ?> decodeHints;

	/**
	 * 活動監控器。如果手機沒有連接電源線，那麼當相機開啟後如果一直處於不被使用狀態則該服務會將當前activity關閉。
	 * 活動監控器全程監控掃描活躍狀態，與CaptureActivity生命周期相同.每一次掃描過後都會重置該監控，即重新倒計時。
	 */
	private InactivityTimer inactivityTimer;

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	CameraManager getCameraManager() {
		return cameraManager;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// 在掃描功能開啟後，保持屏幕處於點亮狀態
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);

		// 這裏僅僅是對各個組件進行簡單的創建動作，真正的初始化動作放在onResume中
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// CameraManager must be initialized here, not in onCreate(). This is
		// necessary because we don't
		// want to open the camera driver and measure the screen size if we're
		// going to show the help on
		// first launch. That led to bugs where the scanning rectangle was the
		// wrong size and partially
		// off screen.
		/**
		 * 上面這段話的意思是說，相機初始化的動作需要開啟相機並測量屏幕大小，這些操作
		 * 不建議放到onCreate中，因為如果在onCreate中加上首次啟動展示幫助信息的代碼的話，會導致掃描窗口的尺寸計算有誤的bug
		 */
		cameraManager = new CameraManager(getApplication());

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);
		resultView = findViewById(R.id.result_view);
		statusView = (TextView) findViewById(R.id.status_view);

		handler = null;
		lastResult = null;

		// 重置狀態窗口，掃描窗口和結果窗口的狀態
		resetStatusView();

		// 攝像頭預覽功能必須借助SurfaceView，因此也需要在一開始對其進行初始化
		// 如果需要了解SurfaceView的原理，參考:http://blog.csdn.net/luoshengyang/article/details/8661317
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			// 如果SurfaceView已經渲染完畢，會回調surfaceCreated，在surfaceCreated中調用initCamera()
			surfaceHolder.addCallback(this);
		}

		// 恢複活動監控器
		inactivityTimer.onResume();

		source = IntentSource.NONE;
		decodeFormats = null;
		characterSet = null;
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}

		// 暫停活動監控器
		inactivityTimer.onPause();

		// 關閉攝像頭
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// 停止活動監控器
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: // 攔截返回鍵
			if ((source == IntentSource.NONE) && lastResult != null) {
				restartPreviewAfterDelay(0L);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_FOCUS:
		case KeyEvent.KEYCODE_CAMERA:
			// Handle these events so they don't launch the Camera app
			return true;
			// Use volume up/down to turn on light
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			cameraManager.setTorch(false);
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			cameraManager.setTorch(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		/*if (holder == null) {
			Log.e(TAG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}*/
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param scaleFactor
	 *            amount by which thumbnail was scaled
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 */
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		inactivityTimer.onActivity();
		lastResult = rawResult;

		boolean fromLiveScan = barcode != null;
		if (fromLiveScan) {
			// Then not from history, so beep/vibrate and we have an image to
			// draw on
			drawResultPoints(barcode, scaleFactor, rawResult);
		}

		switch (source) {
		case NONE:

			// 設置中選擇"批量掃描模式"，則掃描多個條碼的時候，會對其逐一進行掃描
			/*
			 * Toast.makeText( getApplicationContext(), "已掃描成功!",
			 * Toast.LENGTH_SHORT).show();
			 */
			showMyToast("掃描完成...");

			String resToSql = rawResult.getText();
			new SendPostToInternet().execute(resToSql);

			/*
			 * Message msg = new Message(); Bundle myBundle = msg.getData();
			 * resEcho = myBundle.getString("echo"); showMyToast(resEcho);
			 */
			// Wait a moment or else it will scan the same barcode
			// continuously about 3 times
			restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
			break;
		}
	}

	/**
	 * Superimpose a line for 1D or dots for 2D to highlight the key features of
	 * the barcode.
	 * 
	 * @param barcode
	 *            A bitmap of the captured image.
	 * @param scaleFactor
	 *            amount by which thumbnail was scaled
	 * @param rawResult
	 *            The decoded results which contains the points to draw.
	 */
	private void drawResultPoints(Bitmap barcode, float scaleFactor,
			Result rawResult) {
		ResultPoint[] points = rawResult.getResultPoints();
		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_points));
			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				drawLine(canvas, paint, points[0], points[1], scaleFactor);
			} else if (points.length == 4
					&& (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
							.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
				// Hacky special case -- draw two lines, for the barcode and
				// metadata
				drawLine(canvas, paint, points[0], points[1], scaleFactor);
				drawLine(canvas, paint, points[2], points[3], scaleFactor);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					if (point != null) {
						canvas.drawPoint(scaleFactor * point.getX(),
								scaleFactor * point.getY(), paint);
					}
				}
			}
		}
	}

	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b, float scaleFactor) {
		if (a != null && b != null) {
			canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(),
					scaleFactor * b.getX(), scaleFactor * b.getY(), paint);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
//			Log.w(TAG,
//					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new CaptureActivityHandler(this, decodeFormats,
						decodeHints, characterSet, cameraManager);
			}

		} catch (IOException ioe) {
//			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
//			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit();
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	/**
	 * 在經過一段延遲後重置相機以進行下一次掃描。 成功掃描過後可調用此方法立刻准備進行下次掃描
	 * 
	 * @param delayMS
	 */
	public void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
		}
		resetStatusView();
	}

	/**
	 * 展示狀態視圖和掃描窗口，隱藏結果視圖
	 */
	private void resetStatusView() {
		resultView.setVisibility(View.GONE);
		statusView.setText(R.string.msg_default_status);
		statusView.setVisibility(View.VISIBLE);
		viewfinderView.setVisibility(View.VISIBLE);
		lastResult = null;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	// 自訂toast的風格
	public void showMyToast(String content) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.toast_layout,
				(ViewGroup) findViewById(R.id.toast_layout));

		TextView toast_content = (TextView) layout
				.findViewById(R.id.toast_content);

		toast_content.setText(content);

		Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(layout);
		toast.show();
	}
	
	class SendPostToInternet extends AsyncTask<String, Void, String> {
		private String result;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(CaptureActivity.this);
			pDialog.setMessage("Clock In ..");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... arg0) {

			HttpPost httpRequest = new HttpPost(URI_CLOCK_IN);

			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("var_phone", arg0[0]));

			try {
				httpRequest.setEntity(new UrlEncodedFormEntity(params,
						HTTP.UTF_8));
				HttpResponse httpResponse = new DefaultHttpClient()
						.execute(httpRequest);
				if (httpResponse.getStatusLine().getStatusCode() == 200) {

					String webResult = EntityUtils.toString(httpResponse
							.getEntity());

					return result = webResult;
				}
			} catch (Exception e) {
				result = "請確認網路是否開啟";
			}
			return null;
		}

		@Override
		protected void onPostExecute(String file_url) {
			// dismiss the dialog once done
			pDialog.dismiss();
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
					.show();
		}

	}
}
