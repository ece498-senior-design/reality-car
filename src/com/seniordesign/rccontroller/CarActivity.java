package com.seniordesign.rccontroller;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import net.majorkernelpanic.networking.RtspServer;
import net.majorkernelpanic.networking.Session;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

/**Receiver Activity (This is for the device mounted on the RC car)*/

public class CarActivity extends IOIOActivity implements Runnable{
	
	private static final String TAG = "CarActivity";
	
	private ToggleButton button_;
	private SeekBar driveSlider_;
	private SeekBar steerSlider_;
	private TextView drivePwmValTxt;
	private TextView steerPwmValTxt;
	private TextView ipText;
	
	private int drivePwmVal;
	private int steerPwmVal;
	
	public int dSliderVal=50;
	public int sSliderVal=50;
	
	public InetAddress controlConn = null;
	public static final int SERVERPORT = 4444;
	
	// rtsp stuff
	static private RtspServer rtspServer = null;
	private final int defaultRtspPort = 8086;
	public static int videoEncoder = Session.VIDEO_H263;
	public static int audioEncoder = Session.AUDIO_AMRNB;
	public static Exception lastCaughtException;
	public static VideoQuality videoQuality = new VideoQuality(1280,720,15,500000);
	private SurfaceHolder holder;
	private SurfaceView camera;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_car);
		
		camera = (SurfaceView)findViewById(R.id.smallcameraview);
		
		camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
		Session.setSurfaceHolder(holder);
		Session.setHandler(handler);
		Session.setDefaultVideoEncoder(videoEncoder);
        Session.setDefaultVideoQuality(videoQuality);
        Session.setDefaultAudioEncoder(audioEncoder);
		
		rtspServer = new RtspServer(defaultRtspPort, handler);
		
		try {
			rtspServer.start();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
		
		/*Bundle extras = getIntent().getExtras(); 
		if(extras != null) {
			try {
				controlConn = InetAddress.getByName(extras.getString("hostIp"));
			} catch (UnknownHostException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
		}*/
		
		button_ = (ToggleButton) findViewById(R.id.button);
		
		driveSlider_ = (SeekBar) findViewById(R.id.driveSlider);
		driveSlider_.setMax(100);
		driveSlider_.setProgress(50);
		
		steerSlider_ = (SeekBar) findViewById(R.id.steerSlider);
		steerSlider_.setMax(100);
		steerSlider_.setProgress(50);
		
		drivePwmValTxt = (TextView) findViewById(R.id.pwmValTxt);
		steerPwmValTxt = (TextView) findViewById(R.id.pwmValTxtS);
		ipText = (TextView) findViewById(R.id.ipText);
		
		ipText.setText(getLocalIP());
		
		if(controlConn == null)
			try {
				controlConn = InetAddress.getByName(getLocalIP());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
		
		// Connects tries to connect to the controller. So the controller can get the car's ip.
		try {
			SocketAddress address = new InetSocketAddress(InetAddress.getByName(getContrIP()), SERVERPORT);
			
			Socket controller = new Socket();
			
			controller.connect(address);
			
			Log.v(TAG, "Connected to controller at " + controlConn.getHostAddress());
			
			controller.close();
		} catch (IOException e) {
			Log.e(TAG,e.toString());
			e.printStackTrace();
		}
		
		//new Thread(this).start();

	}
	
	/**
	 * Uses the local ip address to find the controller ip. We know that they're on the same network, 
	 * and that the controller is the access point. Adapted from stallion's code.
	 * @return A string in the form (192.168.1.1). Subnet will differ.
	 * @see http://stackoverflow.com/a/5308046
	 */
	private String getContrIP(){
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		
		// we need to know the network prefix and subnet are the same and that the host number is 1.
		String contrIP = (ip & 0xFF) + "." + ((ip >> 8 ) & 0xFF) + "." + ((ip >> 16 ) & 0xFF) + ".1";
		
		Log.d(TAG,"Controller IP: " + contrIP);
		
		return contrIP;
	}
	
	private String getLocalIP(){
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		
		// we need to know the network prefix and subnet are the same and that the host number is 1.
		String contrIP = (ip & 0xFF) + "." + ((ip >> 8 ) & 0xFF) + "." + ((ip >> 16 ) & 0xFF) + "." + ((ip >> 24 ) & 0xFF);
		
		Log.d(TAG,"Controller IP: " + contrIP);
		
		return contrIP;
	}
	
	/**This is the UDP Runnable: Establishes the Receiver as the host and waits for the 
	 * Controller to send commands.  Contains safeties to prevent the RC car from going crazy
	 */
	@Override
	public void run() {
		DatagramSocket socket = null;
		try {

			String incoming;
			String contID;
			String contVal;

			Log.d("UDP", "S: Connecting... " + controlConn.getHostAddress());
			/* Create new UDP-Socket */
			socket = new DatagramSocket(SERVERPORT, controlConn);

			/* By magic we know, how much data will be waiting for us */
			byte[] buf = new byte[17];
			/* Prepare a UDP-Packet that can
			 * contain the data we want to receive */
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			Log.d("UDP", "S: Receiving...");

			/* Receive the UDP-Packet */
			socket.setSoTimeout(1000);
			socket.receive(packet);

			incoming = new String(packet.getData());
			contVal= incoming.substring(1,3);
			contID=incoming.substring(0,1);
			if(contID.equals("d")){
				dSliderVal=Integer.valueOf(contVal);
				sSliderVal=50;
			}
			else if(contID.equals("s")){
				sSliderVal=Integer.valueOf(contVal);
				dSliderVal=50;
			}
			else{
				sSliderVal=dSliderVal=50;
			}

			Log.d("UDP", "S: Received: '" + contVal+contID +"'");
			Log.d("UDP", "S: Done.");



			socket.close();
		} catch (Exception e) {
			Log.e("UDP", "S: Error", e);
			//Stops the RC car in case of error
			sSliderVal=dSliderVal=50;
			socket.close();
		}

	}
	
	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {
		// The on-board LED. 
		private DigitalOutput led_;
		private PwmOutput drivePwm;
		private PwmOutput steerPwm;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException, InterruptedException {
			led_ = ioio_.openDigitalOutput(0, true);
			drivePwm = ioio_.openPwmOutput(3, 50);
			drivePwm.setPulseWidth(1500);
			steerPwm = ioio_.openPwmOutput(6, 50);
			steerPwm.setPulseWidth(1500);
			enableUi(true);
			//Thread.sleep(1500);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			led_.write(!button_.isChecked());
			
			run();//Get the slider values from the controller
			setSlider(false,sSliderVal);
			setSlider(true,dSliderVal);
			
			drivePwmVal= 1000+(driveSlider_.getProgress())*10;			
			drivePwm.setPulseWidth(drivePwmVal);	
			
			steerPwmVal= 2000-(steerSlider_.getProgress())*10;	
			steerPwm.setPulseWidth(steerPwmVal);
			
			setText(Integer.toString(drivePwmVal),Integer.toString(steerPwmVal));
			
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	/**Can't modify the UI inside IOIO Loop so do it here*/
	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				driveSlider_.setEnabled(enable);
				button_.setEnabled(enable);
			}
		});
	}

	private void setText(final String str,final String str2) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				drivePwmValTxt.setText(str);
				steerPwmValTxt.setText(str2);
			}
		});
	}
	
	private void setSlider(final boolean drive,final int val) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(drive==true){
					driveSlider_.setProgress(val);
				}
				else
					steerSlider_.setProgress(val);
			}
		});
	}
	
	private boolean streaming = false;
	
	// The Handler that gets information back from the RtspServer and Session
    private final Handler handler = new Handler() {
    	
    	public void handleMessage(Message msg) { 
    		switch (msg.what) {
    		case RtspServer.MESSAGE_ERROR:
    			Exception e1 = (Exception)msg.obj;
    			lastCaughtException = e1;
    			//log(e1.getMessage()!=null?e1.getMessage():"An error occurred !");
    			break;
    		case RtspServer.MESSAGE_LOG:
    			//log((String)msg.obj);
    			break;
    		/*case HttpServer.MESSAGE_ERROR:
    			Exception e2 = (Exception)msg.obj;
    			lastCaughtException = e2;
    			break;*/    			
    		case Session.MESSAGE_START:
    			streaming = true;
    			//streamingState(1);
    			break;
    		case Session.MESSAGE_STOP:
    			streaming = false;
    			//displayIpAddress();
    			break;
    		}
    	}
    	
    };

    private void stopRtsp(){
    	if (rtspServer != null) {
    		rtspServer.stop();
    		rtspServer = null;
    	}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopRtsp();
	}
	
}

