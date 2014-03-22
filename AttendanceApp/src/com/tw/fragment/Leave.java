package com.tw.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.appname.R;

public class Leave extends Fragment implements OnClickListener,
		OnItemSelectedListener {

	private Spinner conditionSpinner;
	private ArrayAdapter<String> listAdapter;
	private int[] drawableIds = { R.drawable.img1, R.drawable.img2,
			R.drawable.img3, R.drawable.img4, R.drawable.img5, R.drawable.img6 };	
	private String selected;
	private Button startDateBtn, startTimeBtn, endDateBtn, endTimeBtn, sendBtn;
	private EditText memoEditArea;
	private int setYear, setMonth, setDate, setHour, setMinute;
	private Calendar startCalendar, endCalendar;
	private ProgressDialog pDialog;
	private final String URI_CLOCK_IN = "http://flash60905.qov.tw/QRcode/member_leave_res.php";
	public static final int REFRESH_DATA = 0x00000001;
	private boolean isDateStart, isDateEnd, isTimeStart, isTimeEnd;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frg_leave, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setUpUIComponent();
		
	}

	private void setUpUIComponent() {
		conditionSpinner = (Spinner) getView().findViewById(
				R.id.conditionSpinner);

		BaseAdapter ba = new BaseAdapter() {
			// @Override
			public int getCount() {
				return 6;
			}
			// @Override
			public Object getItem(int arg0) {
				return null;
			}
			// @Override
			public long getItemId(int arg0) {
				return 0;
			}
			// @Override
			public View getView(int arg0, View arg1, ViewGroup arg2) {
				/*
				 * 动态生成每个下拉项对应的View，每个下拉项View由LinearLayout
				 * 中包含一个ImageView及一个TextView构成
				 */
				// 初始化LinearLayout
				LinearLayout ll = new LinearLayout(Leave.this.getActivity());
				ll.setOrientation(LinearLayout.HORIZONTAL); // 设置朝向
				// 初始化ImageView
				ImageView ii = new ImageView(Leave.this.getActivity());
				ii.setImageDrawable(getResources().getDrawable(
						drawableIds[arg0]));// 设置图片
				ll.addView(ii);// 添加到LinearLayout中
				
				return ll;
			}
		};

		conditionSpinner.setAdapter(ba);// 为Spinner设置内容适配器
		conditionSpinner.setOnItemSelectedListener(this);

		startDateBtn = (Button) getView().findViewById(R.id.startDateBtn);
		startTimeBtn = (Button) getView().findViewById(R.id.startTimeBtn);
		endDateBtn = (Button) getView().findViewById(R.id.endDateBtn);
		endTimeBtn = (Button) getView().findViewById(R.id.endTimeBtn);
		sendBtn = (Button) getView().findViewById(R.id.sendBtn);
		memoEditArea = (EditText) getView().findViewById(R.id.memoEditArea);
		startDateBtn.setOnClickListener(this);
		startTimeBtn.setOnClickListener(this);
		endDateBtn.setOnClickListener(this);
		endTimeBtn.setOnClickListener(this);
		sendBtn.setOnClickListener(this);
		startCalendar = Calendar.getInstance();
		endCalendar = Calendar.getInstance();
	}

	private void showDatePickerDialog(final int i) {
		if (i == 1) {
			setYear = endCalendar.get(Calendar.YEAR);
			setMonth = endCalendar.get(Calendar.MONTH);
			setDate = endCalendar.get(Calendar.DAY_OF_MONTH);
		} else if (i == 0) {
			setYear = startCalendar.get(Calendar.YEAR);
			setMonth = startCalendar.get(Calendar.MONTH);
			setDate = startCalendar.get(Calendar.DAY_OF_MONTH);
		}
		new DatePickerDialog(getActivity(),
				new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year,
							int monthOfYear, int dayOfMonth) {
						if (i == 1) {
							// 結束
							endCalendar.set(year, monthOfYear, dayOfMonth);
							endDateBtn.setText(year + "/"
									+ String.format("%02d", (monthOfYear + 1))
									+ "/" + String.format("%02d", dayOfMonth));
							if (startCalendar.getTime().after(
									endCalendar.getTime())) {
								startCalendar
										.set(year, monthOfYear, dayOfMonth);
								Toast.makeText(getActivity(),
										"start after end", Toast.LENGTH_LONG)
										.show();

							}
							startDateBtn.setText(startCalendar
									.get(Calendar.YEAR)
									+ "/"
									+ String.format(
											"%02d",
											(startCalendar.get(Calendar.MONTH) + 1))
									+ "/"
									+ String.format("%02d", startCalendar
											.get(Calendar.DAY_OF_MONTH)));
						} else if (i == 0) {
							// 開始
							startCalendar.set(year, monthOfYear, dayOfMonth);
							startDateBtn.setText(year + "/"
									+ String.format("%02d", (monthOfYear + 1))
									+ "/" + String.format("%02d", dayOfMonth));
							if (startCalendar.getTime().after(
									endCalendar.getTime())) {
								endCalendar.set(year, monthOfYear, dayOfMonth);
							}
							endDateBtn.setText(endCalendar.get(Calendar.YEAR)
									+ "/"
									+ String.format(
											"%02d",
											(endCalendar.get(Calendar.MONTH) + 1))
									+ "/"
									+ String.format("%02d", endCalendar
											.get(Calendar.DAY_OF_MONTH)));
						}
					}
				}, setYear, setMonth, setDate).show();
	}

	private void showTimePickerDialog(final int i) {
		if (i == 1) {
			setHour = endCalendar.get(Calendar.HOUR_OF_DAY);
			setMinute = endCalendar.get(Calendar.MINUTE);
		} else if (i == 0) {
			setHour = startCalendar.get(Calendar.HOUR_OF_DAY);
			setMinute = startCalendar.get(Calendar.MINUTE);
		}
		new TimePickerDialog(getActivity(),
				new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay,
							int minute) {
						if (i == 1) {
							endCalendar.set((endCalendar.get(Calendar.YEAR)),
									endCalendar.get(Calendar.MONTH),
									endCalendar.get(Calendar.DAY_OF_MONTH),
									hourOfDay, minute);
							endTimeBtn.setText(String.format("%02d", hourOfDay)
									+ " : " + String.format("%02d", minute));
							if ((hourOfDay * 60 + minute) < (startCalendar
									.get(Calendar.HOUR_OF_DAY) * 60 + startCalendar
									.get(Calendar.MINUTE))) {
								startCalendar.set(startCalendar
										.get(Calendar.YEAR), startCalendar
										.get(Calendar.MONTH), startCalendar
										.get(Calendar.DAY_OF_MONTH), hourOfDay,
										minute);
							}
							startTimeBtn.setText(String.format("%02d",
									startCalendar.get(Calendar.HOUR_OF_DAY))
									+ " : "
									+ String.format("%02d",
											startCalendar.get(Calendar.MINUTE)));
						} else if (i == 0) {
							startCalendar.set(startCalendar.get(Calendar.YEAR),
									startCalendar.get(Calendar.MONTH),
									startCalendar.get(Calendar.DAY_OF_MONTH),
									hourOfDay, minute);
							startTimeBtn.setText(String.format("%02d",
									hourOfDay)
									+ " : "
									+ String.format("%02d", minute));
							if ((hourOfDay * 60 + minute) > (endCalendar
									.get(Calendar.HOUR_OF_DAY) * 60 + endCalendar
									.get(Calendar.MINUTE))) {
								endCalendar.set(endCalendar.get(Calendar.YEAR),
										endCalendar.get(Calendar.MONTH),
										endCalendar.get(Calendar.DAY_OF_MONTH),
										hourOfDay, minute);
							}
							endTimeBtn.setText(String.format("%02d",
									endCalendar.get(Calendar.HOUR_OF_DAY))
									+ " : "
									+ String.format("%02d",
											endCalendar.get(Calendar.MINUTE)));
						}
					}
				}, setHour, setMinute, true).show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.startDateBtn:
			showDatePickerDialog(0);
			isDateStart = true;
			break;
		case R.id.startTimeBtn:
			showTimePickerDialog(0);
			isTimeStart = true;
			break;
		case R.id.endDateBtn:
			showDatePickerDialog(1);
			isDateEnd = true;
			break;
		case R.id.endTimeBtn:
			showTimePickerDialog(1);
			isTimeEnd = true;
			break;
		case R.id.sendBtn:
			SimpleDateFormat myForm = new SimpleDateFormat("yyyy-MM-dd HH:mm",
					java.util.Locale.getDefault());
			String startTime = myForm.format(startCalendar.getTime());
			String endTime = myForm.format(endCalendar.getTime());
			String[] msg = { MyFragment.user_phone, startTime, endTime,
					selected, memoEditArea.getText().toString() };
			if (MyFragment.user_phone.isEmpty()) {
				Toast.makeText(getActivity(), "尚未輸入手機號碼", Toast.LENGTH_SHORT)
				.show();
			} 
			else if (isDateStart && isDateEnd && isTimeStart && isTimeEnd) {
				new SendPostToInternet().execute(msg);
			} else {
				Toast.makeText(getActivity(), "請選擇日期時間", Toast.LENGTH_SHORT)
						.show();
			}
			break;
		}

	}

	class SendPostToInternet extends AsyncTask<String, Void, String> {
		private String result;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(getActivity());
			pDialog.setMessage("請稍候 ..");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... arg0) {

			HttpPost httpRequest = new HttpPost(URI_CLOCK_IN);

			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("l_phone", arg0[0]));
			params.add(new BasicNameValuePair("l_start", arg0[1]));
			params.add(new BasicNameValuePair("l_end", arg0[2]));
			params.add(new BasicNameValuePair("l_condition", arg0[3]));
			params.add(new BasicNameValuePair("l_memo", arg0[4]));

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
			Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		selected = (position + 1) + "";
		sendBtn.setEnabled(true);

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

}