package com.tw.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.appname.R;

public class MyAdapter extends BaseAdapter {

	Context contx;
	LayoutInflater inflater;
	ArrayList<HashMap<String, String>> list;
	
	public MyAdapter(Context contx, ArrayList<HashMap<String, String>> list) {
		 this.contx = contx;
		 this.list = list;
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//View 的產生器，用來把R.layout.list_item3.xml轉換成View物件
		inflater = LayoutInflater.from(contx);
		
		//設定convertView容器中的元件，透過findViewById()的方法
		ViewTag viewTag;
		if(convertView==null){
			viewTag = new ViewTag();
			convertView = inflater.inflate(R.layout.my_list_item, null);
			viewTag.llayout = (LinearLayout)convertView.findViewById(R.id.list_layout);
			viewTag.tv_first = (TextView)convertView.findViewById(R.id.tv_first);
			viewTag.tv_last = (TextView)convertView.findViewById(R.id.tv_last);
			viewTag.tv_condition = (TextView)convertView.findViewById(R.id.tv_condition);
			convertView.setTag(viewTag);
			
			viewTag.tv_first.setText(list.get(position).get("var_first"));
			viewTag.tv_last.setText(list.get(position).get("var_last"));
			viewTag.tv_condition.setText(list.get(position).get("var_condition"));
			
			//設定ViewTag裡的元件的各別資料
			if(viewTag.tv_condition.getText().equals("準時"))
				viewTag.tv_condition.setTextColor(Color.BLUE);

		}
		
		return convertView;
	}
	class ViewTag{
		LinearLayout llayout;
		TextView tv_first, tv_last, tv_condition;
	}

}
