package com.tw.fragment;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appname.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrFragment extends Fragment {
	private Button btnInput;
	private EditText txtInput;
	private BitMatrix bitMatrix;
	boolean isHere;
	private String strInput;
	private AlertDialog.Builder dialog;
	private String FILENAME = "myfile";
	private RelativeLayout QRimgLayout;
	private MyFragment mainActivity;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mainActivity = (MyFragment) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frg_qr, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		findViews();// 建構元件

		strInput = readFile();// 讀取號碼
		mainActivity.setUserPhone(strInput);
		if (!strInput.contentEquals("")) {
			// 如果已經輸入過了
			bitMatrix = getBitMatrix(strInput);
			// 將結果輸出在QRimgLayout上
			setFragmentContentView(new QRCodeDrawer(getView().getContext(),
					bitMatrix));
		}

		// 再次確認的Dialog
		dialog = new Builder(getActivity());
		dialog.setCancelable(false);
		dialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 按下確定動作
				writeFile();// 寫入號碼
				mainActivity.setUserPhone(strInput);
				// 將結果輸出在QRimgLayout上
				setFragmentContentView(new QRCodeDrawer(getView().getContext(),
						bitMatrix));
			}
		});
		dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 按下取消動作
			}
		});

		btnInput.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// 按下button先抓字串，丟到getBitMatrix，而後show dialog
				strInput = txtInput.getText().toString();
				if (strInput.isEmpty()) {
					Toast.makeText(getActivity(), "尚未輸入手機號碼", Toast.LENGTH_SHORT)
					.show();
				} else {
					bitMatrix = getBitMatrix(strInput);
					dialog.setTitle("您的號碼：" + strInput); // 設定dialog 的title顯示內容
					dialog.show();
				}
			}
		});
	}

	// 建構元件
	private void findViews() {
		btnInput = (Button) this.getView().findViewById(R.id.btnInput);
		txtInput = (EditText) this.getView().findViewById(R.id.txtInput);
		QRimgLayout = (RelativeLayout) this.getView().findViewById(
				R.id.QRrelativeLayout);
	}

	// Fragment裡面的setContentView
	void setFragmentContentView(View view) {
		// TextView加入RelativeLayout的參數設定
		TextView txtPhone = new TextView(getView().getContext());
		txtPhone.setText("Phone :  " + strInput);
		txtPhone.setId(1);
		RelativeLayout.LayoutParams TextViewlayoutParams = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		TextViewlayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		TextViewlayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				RelativeLayout.TRUE);
		// QR產生的View加入RelativeLayout的參數設定
		RelativeLayout.LayoutParams QrViewlayoutParams = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		QrViewlayoutParams.addRule(RelativeLayout.BELOW, 1);
		QrViewlayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				RelativeLayout.TRUE);
		// 清除ViewGroup裡面的View
		QRimgLayout.removeAllViews();
		// 將TextView和QR的View加入QRimgLayout(Fragment)裡面
		QRimgLayout.addView(txtPhone, TextViewlayoutParams);
		QRimgLayout.addView(view, QrViewlayoutParams);
	}

	// 寫入內存區
	private void writeFile() {
		FileOutputStream fos;
		try {
			fos = getView().getContext().openFileOutput(FILENAME,
					Context.MODE_PRIVATE);
			fos.write(strInput.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 讀取內存區
	private String readFile() {
		FileInputStream fis;
		try {
			fis = getView().getContext().openFileInput(FILENAME);
			byte[] input = new byte[fis.available()];
			while (fis.read(input) != -1) {
			}
			String output = new String(input);
			return output;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 將字串轉換成位元陣列
	 */
	private BitMatrix getBitMatrix(String target) {
		QRCodeWriter qrcodewriter = new QRCodeWriter();
		BitMatrix bitmatrix = null;
		try {
			Hashtable<EncodeHintType, Object> hst = new Hashtable<EncodeHintType, Object>();
			hst.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			bitmatrix = qrcodewriter.encode(target, BarcodeFormat.QR_CODE, 1,
					1, hst);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bitmatrix;
	}

	/**
	 * 將位元陣列繪圖成QR碼
	 */
	class QRCodeDrawer extends View {
		BitMatrix bitmatrix;
		Paint paint;

		public QRCodeDrawer(Context context, BitMatrix bitmatrix) {
			super(context);
			this.bitmatrix = bitmatrix;
			paint = new Paint();
		}

		@SuppressLint("DrawAllocation")
		@Override
		public void onDraw(Canvas c) {
			if (bitmatrix == null)
				return;
			c.drawColor(Color.WHITE);
			Rect bounds = c.getClipBounds();
			int w = bounds.width();
			int h = bounds.height();

			int imageSize = bitmatrix.getHeight();
			int blockSize = w / imageSize;

			int top_offset = (h - imageSize * blockSize) / 2;
			int left_offset = (w - imageSize * blockSize) / 2;

			for (int i = 0; i < imageSize; i++)
				for (int j = 0; j < imageSize; j++)
					if (bitmatrix.get(i, j))
						c.drawRect(left_offset + i * blockSize, top_offset + j
								* blockSize, left_offset + (i + 1) * blockSize,
								top_offset + (j + 1) * blockSize, paint);
		}
	}
}
