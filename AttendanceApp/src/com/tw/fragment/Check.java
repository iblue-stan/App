package com.tw.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.appname.R;

public class Check extends ListFragment {

	private static String url_all_attend = "http://flash60905.qov.tw/QRcode/all_attendance.php";
	private ArrayList<HashMap<String, String>> list;

	// 載入畫面之前.利用手機號碼讀取資料
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		list = new ArrayList<HashMap<String, String>>();

		if (MyFragment.user_phone.isEmpty()) {
			Toast.makeText(getActivity(), "尚未輸入手機號碼", Toast.LENGTH_SHORT)
			.show();
		} else {
			new SendPostToInternet().execute(MyFragment.user_phone);
		}
		return inflater.inflate(R.layout.frg_check, container, false);
	}

	// 當選項被按下的時候..
	public void onListItemClick(ListView parent, View v, int position, long id) {

	}

	class SendPostToInternet extends AsyncTask<String, Void, String> {
		private ProgressDialog pDialog;
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

			HttpPost httpRequest = new HttpPost(url_all_attend);

			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("l_phone", arg0[0]));

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
			// decode JSON
			try {
				JSONArray array = new JSONArray(result);
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					HashMap<String, String> item = new HashMap<String, String>();
					item.put("var_first", obj.getString("var_first"));
					item.put("var_last", obj.getString("var_last"));
					item.put("var_condition", obj.getString("var_condition"));
					list.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.d("LOG", e.toString());
			}

			MyAdapter myAdapter = new MyAdapter(getActivity(), list);
			setListAdapter(myAdapter);
		}
	}

}
