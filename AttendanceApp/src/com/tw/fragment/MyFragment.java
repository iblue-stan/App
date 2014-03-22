package com.tw.fragment;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;

import com.example.appname.R;

public class MyFragment extends FragmentActivity {
	
	public static String user_phone;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);

		FragmentTabHost tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);

		tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

		// 1
		tabHost.addTab(tabHost.newTabSpec("A").setIndicator("QR卡"),
				QrFragment.class, null);
		// 2
		tabHost.addTab(tabHost.newTabSpec("B").setIndicator("查詢紀錄"),
				Check.class, null);
		// 3
		tabHost.addTab(tabHost.newTabSpec("C").setIndicator("請假"),
				Leave.class, null);
		// //4
		// tabHost.addTab(tabHost.newTabSpec("D").setIndicator("Twitter"),
		// FragmentD.class, null);
	}

	/**************************
	 * 給子頁籤呼叫用
	 **************************/
	public void setUserPhone(String phone){
		user_phone = phone;
	}

	/**
	 * public String getTwitterData() { return "Twitter abc"; }
	 **/
}
