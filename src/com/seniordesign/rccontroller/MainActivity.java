package com.seniordesign.rccontroller;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{

	private static final String TAG = "AND Reality Car";	
	ImageView title; 
	Button carB;
	Button controlB;
	TextView name1;
	TextView name2;
	TextView name3;
	MediaPlayer sound;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//play music
		sound = MediaPlayer.create(MainActivity.this,R.raw.mario);
		sound.start();
	
		//create objects
		 title = (ImageView)findViewById(R.id.imageView1);
		 controlB = (Button)findViewById(R.id.buttonController);
		 carB = (Button)findViewById(R.id.buttonCar);
		 name1 = (TextView)findViewById(R.id.TextView1);
		 name2 = (TextView)findViewById(R.id.TextView2);
		 name3 = (TextView)findViewById(R.id.TextView3);
		 
		 controlB.setOnClickListener(this);
		 carB.setOnClickListener(this);
		 
		//So Android doesn't go into network lockdown
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy);
	}
	

	@Override
	public void onClick(View v) {
		sound.release();
		//Log.v(TAG, "outside if" + v.toString());
		if(v.equals(controlB))
		{	
		Intent i = new Intent("com.seniordesign.rccontroller.con"/*getBaseContext(), ControlActivity.class*/);
		startActivity(i);	
		}
		if(v.equals(carB)){
			Intent i = new Intent("com.seniordesign.rccontroller.rec");
			startActivity(i);
		}
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		sound.stop();
		sound = null;
		
	}
	
	

}



