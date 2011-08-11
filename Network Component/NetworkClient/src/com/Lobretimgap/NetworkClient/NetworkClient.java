package com.Lobretimgap.NetworkClient;

import android.app.Activity;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

public class NetworkClient extends Activity {

	private TextView tv;
	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		ScrollView sc = new ScrollView(this);		
        tv = new TextView(this);
        sc.addView(tv);
        setContentView(sc);
	}
}
