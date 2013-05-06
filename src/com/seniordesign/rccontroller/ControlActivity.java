package com.seniordesign.rccontroller;

//import io.vov.vitamio.MediaPlayer;
//import io.vov.vitamio.widget.MediaController;
//import io.vov.vitamio.widget.VideoView;

import android.media.MediaPlayer;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.example.andrealitycarinterface.utils.BitmapScaler;

/**This is the Controller Activity (Tablet) that sends wireless control commands to the 
 * device on the RC car
 */
public class ControlActivity extends Activity implements OnTouchListener, 
			 OnSeekBarChangeListener, SensorEventListener, OnCheckedChangeListener, OnClickListener{
	
	private static final String TAG = "ContrActivity";
	
	private ViewGroup parent;  //portion of the view which shows the steering wheel
	private SteeringSurface steeringWheel;  
	private float x,y;
	
	// demo mode stuff?
	public boolean transmitS,demoMode=false;
	public int cnt=0,prevSteer,currSteer;
	private ToggleButton toggleTilt;
	private ToggleButton toggleDemo;
	private Button brake;
	
	private SensorManager sensorManager;
	private SeekBar driveSlider_;
	private SeekBar steerSlider_;
	
	public String message=" ";
	private String dprogStr;
	private String sprogStr;
	public boolean sensorsOn=false;
	
	private boolean isRunControlLoop = true;
	
	private VideoView mVideoView;
	
	// communication part
	private InetAddress carConn = null;
	public String SERVERIP = null;
	public static final int SERVERPORT = 4444;
	
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
		//	return;
		
		setContentView(R.layout.activity_drive);
		
		/*toggleTilt=(ToggleButton)findViewById(R.id.toggleTilt);
		toggleTilt.setOnCheckedChangeListener(this);
		
		toggleDemo=(ToggleButton)findViewById(R.id.toggleDemo);
		toggleDemo.setOnCheckedChangeListener(this);
		
		brake=(Button)findViewById(R.id.buttonBrake);
		brake.setOnClickListener(this);*/
		
		Context context = getApplicationContext();
		context.getExternalCacheDir();
		
		parent = (ViewGroup)findViewById(R.id.wheel);
		
		mVideoView = (VideoView) findViewById(R.id.videoview);
		//mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_MEDIUM);
		mVideoView.setMediaController(new MediaController(context));
		//mVideoView.setBufferSize(0);

		steeringWheel = new SteeringSurface(this); 
		steeringWheel.setOnTouchListener(this); //implement onTouch
		x=0; 
		y=0;
		
		parent.addView(steeringWheel);
		driveSlider_= (SeekBar) findViewById(R.id.accSlider);
		driveSlider_.setOnSeekBarChangeListener(this);
		driveSlider_.setMax(100);
		driveSlider_.setProgress(50);
		//driveSlider_.setThumb(getResources().getDrawable(R.drawable.thumb5));
		
		ProgressDialog progress = new ProgressDialog(this);
		progress.setMessage("Waiting for Connection...");
		new MyTask(progress).execute();
		
		//Loop to snap the controls back to neutral position when not being pressed
		Runnable runControlLoop=new Runnable(){
			public void run(){
				while(isRunControlLoop){
					
				if(!driveSlider_.isPressed())
					driveSlider_.setProgress(50);
				
				message= "s" + Integer.toString(steeringWheel.steerval(steeringWheel.getCurrRotation()));
				
				//message= "s"+sprogStr;
				transmit();
				//Log.d(TAG,message);
				//Log.d(TAG,"d"+dprogStr);
				}
			}
			
		};
		Thread controlLoop=new Thread(runControlLoop);
		controlLoop.start();
		
	}
	
	public class MyTask extends AsyncTask<Void, Void, Void> {
		  private ProgressDialog progress;

		public MyTask(ProgressDialog progress) {
		    this.progress = progress;
		  }

		  public void onPreExecute() {
		    progress.show();
		  }

		  public void doInBackground() {
		    //... do your loading here ...
		  }

		  public void onPostExecute() {			  
		    progress.dismiss();
		  }

		  @Override
		  protected Void doInBackground(Void... params) {
			  WifiAP controllerAP = new WifiAP(getApplicationContext());

			  if(!controllerAP.isWifiApActive()) // if no ap then make one
				  controllerAP.createWifiAccessPoint();
			  
			  /* Gets the ip for the car device by wanting for a packet from it. */
			 if(carConn == null){
				  try {
					  ServerSocket socket = new ServerSocket(SERVERPORT); // yes it's tcp but that doesn't matter
					  //socket.setSoTimeout(100);
					  // waits until a client connects to the server port.
					  carConn = socket.accept().getInetAddress();

					  Log.v(TAG, "Got connection from " + carConn.getHostAddress());

					  socket.close();
					  onPostExecute();

				  } catch (IOException e) {
					  Log.e(TAG, e.toString());
					  e.printStackTrace();
				  }

			  }
			  
			  Log.i(TAG, "Starting Video");
			  
			  //while(!mVideoView.isPlaying()){
			  runOnUiThread(new Runnable() {
				    public void run() {
				    	mVideoView.setVideoURI(Uri.parse("rtsp://"+carConn.getHostAddress()+":8086"));
				    	//mVideoView.setVideoURI(Uri.parse("rtsp://192.168.43.142:8086"));
				    	//mVideoView.setVideoURI(Uri.parse("rtsp://v4.cache5.c.youtube.com/CjYLENy73wIaLQkBlQn2OQt0IBMYDSANFEIJbXYtZ29vZ2xlSARSBXdhdGNoYPPshPKHtrCAUQw=/0/0/0/video.3gp"));
				        mVideoView.requestFocus();
				        mVideoView.start();
				    }
				});
			  //}
			  
			  onPostExecute();
			  return null;
		  }
	}
	
	public void turnOnSensors(){
		sensorsOn=true;
		sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
		// add listener. The listener will be HelloAndroid (this) class
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
		
	}
	
	/**
	 * UDP Runnable connects to the host(receiver) and sends control commands
	 */
	public void transmit() {
        try {
                // Retrieve the ServerName
        		//if(carConn == null) // just in case
        		//	throw new NullPointerException("Client not set.");
               
                //Log.d(TAG, "C: Connecting... " + carConn.getHostAddress());
                
                /* Create new UDP-Socket */
                DatagramSocket socket = new DatagramSocket();
               
                /* Prepare some data to be sent. */
                byte[] buf = (message).getBytes();
               
                /* Create UDP-packet with
                 * data & destination(url+port) */
                DatagramPacket packet = new DatagramPacket(buf, buf.length, carConn, SERVERPORT);
                //Log.d(TAG, "C: Sending: '" + message + "'");
               
                /* Send out the packet */
                socket.send(packet);

        } catch (Exception e) {
                //Log.e(TAG, e.toString());
        }
	}

	/**
	 * Check to see if the user is moving sliders.  If so, run the UDP Runnable
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if(progress<10){
			dprogStr="0"+Integer.toString(progress);
		}
		else
			dprogStr=Integer.toString(progress);
		
		if(seekBar.getId()==R.id.accSlider){
			message= "d"+dprogStr; //Sends command in the form of (sliderID,value) i.e s30=steer to 30 pos
			//Log.d(TAG,message);
			transmit();
		}
					
		transmit();
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		driveSlider_.setProgress(50);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@SuppressWarnings("unused")
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
			
			// assign directions
			float x=event.values[0];
			float y=event.values[1];
			float z=event.values[2];
			
			steerSlider_.setProgress((int) (100-(x*10+50)));
		}	
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		if(arg1==true)
			turnOnSensors();
		else{
			sensorManager.unregisterListener(this);
			sensorsOn=false;
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		steeringWheel.pause();
		isRunControlLoop = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		steeringWheel.resume(); 
		isRunControlLoop = true;
	}

	@Override
	//gathers the x and y coordinates when the surface is touched. 
	public boolean onTouch(View v, MotionEvent event) {
		x=event.getX();
		y=event.getY();
		
		//When user touch the screen, resume the thread 
		if(event.getAction() == MotionEvent.ACTION_DOWN)
			steeringWheel.resume();
		
		//When user stops touching the screen, pause the thread
		else if(event.getAction() == MotionEvent.ACTION_UP)
			steeringWheel.pause();
			
		return true; //keep checking variables
	} 
	
	public class SteeringSurface extends SurfaceView implements Runnable{
		private SurfaceHolder myHolder; 
		private Thread myThread = null;
		private boolean isRunning=false;
		
		private float currRotation = 0;
		private Context mContext;
		private int progress = 0;

		//SteeringSurface constructor
		public SteeringSurface(Context context){
			super(context);
			mContext = context;
			myHolder = getHolder(); 
		}
		
		//calculate angle at the point where user pressed
		public float turnAngle(){
			float xOrigin = parent.getWidth()/2;
			float yOrigin = parent.getHeight()/2;
			
			float deltaY = y-yOrigin;
			float deltaX = x-xOrigin;
			
			float degree = (float) -Math.toDegrees(Math.atan(deltaY/deltaX))-90;
			
			//Log.v("drive angle", "Original degree: " + degree);
			
			if(x > xOrigin && y < yOrigin) //quadrant 1 
				degree = -degree; 
			else if(x < xOrigin && y < yOrigin) //quadrant 2
				degree = 180 - degree; 
			else if(x < xOrigin && y > yOrigin)  //quadrant 3
				degree = 180 - degree;
			else if(x > xOrigin && y > yOrigin)  //quadrant 4 
				degree = -degree;
			else 
				degree = degree;
			
			//Log.v("drive angle", "degree: " + degree + " x: " + x + " y: " + y);
			
			return degree;
		}
		
		//turn steering wheel
		public void imageManipulate(float rotDegree, Canvas canvas){

			Matrix transform = new Matrix();

			transform.postRotate(rotDegree);
			
			//get bitmap
			BitmapScaler scaler = new BitmapScaler(getResources(),R.drawable.steering_wheel6,
					parent.getWidth());
			
			//scale bitmap to desired size
			Bitmap transformed = Bitmap.createBitmap(scaler.getBitmap(), 0, 0,
					parent.getWidth(), parent.getHeight(), transform, true);
			
			float centerX= parent.getWidth()/2 - transformed.getWidth()/2;
			float centerY= parent.getHeight()/2 - transformed.getHeight()/2;
			canvas.drawBitmap(transformed,centerX,centerY,null); //center picture to cursor
			
			currRotation = rotDegree;
		}
		
		//Calculate progress value (0-100) of slider when we have the angle 
		public int steerval(float degree){
			
			if(degree >= 0 && degree <= 135)
				return (int) (50 + (degree/2.7));   //progress value 50-100 turns right
			else if (degree >= 225 && degree <= 359)
				return (int) (50 - ((360-degree)/2.7)); //progress value 0-49 turns left
			else
				return 50;  //default to middle of slider
		}
		
		//getter function to get progress value
		public int getProgress(){
			return progress;
		}
		
		public void display(float angle){
			Canvas canvas = myHolder.lockCanvas();

			//clear canvas or else multiple steering wheel images appear
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); 
			imageManipulate(angle, canvas);
			progress = steerval(angle);  //calculate progress
			
			myHolder.unlockCanvasAndPost(canvas);
		}
		
		public void run(){
			float angle = 0;
			while(isRunning){
				if(!myHolder.getSurface().isValid())  //if surface is not valid, loop
					continue;
				angle = turnAngle();
				
				//let the wheel steer between 0-135 and 225-360
				if(x!=0 && y!=0 && (angle <= 135 || angle >= 225))
					display(angle);
			}
			if(!isRunning){ // moves wheel back to starting position if screen not touched
				display(0); // reset to the origin
			}
			
		}//end of run() 
		
		public float getCurrRotation(){
			if(isRunning)
				return currRotation;
			else
				return 0;
		}

		public void pause(){
			isRunning=false;
			sprogStr="50";
			while(true){
				try{
					myThread.join();   //wait for threads
				}
				catch(Exception e){
				}		
				break;
			}
			myThread=null;
		}
		
		public void resume(){
			isRunning=true; 
			myThread=new Thread(this);
			myThread.start();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		//if (mVideoView != null)
			//mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
