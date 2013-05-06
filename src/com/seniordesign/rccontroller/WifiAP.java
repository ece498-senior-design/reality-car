package com.seniordesign.rccontroller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gmail.radikll.pro.reflect.Extract;

/**
 * This class uses reflection to use undocumented methods from the Android API.
 * These methods allow us to create an access point programmatically.
 * 
 * @author Jon Green
 */
public class WifiAP {
	
	static final private String TAG = "WifiAP-class";
	
	// access point states
	public static final int WIFI_AP_STATE_UNKNOWN 	= -1;
	public static final int WIFI_AP_STATE_DISABLING	= 0;
	public static final int WIFI_AP_STATE_DISABLED 	= 1;
	public static final int WIFI_AP_STATE_ENABLING 	= 2;
	public static final int WIFI_AP_STATE_ENABLED 	= 3;
	public static final int WIFI_AP_STATE_FAILED 	= 4;
	
	public static final int WIFI_NETWORK_OPEN	= 0;
	public static final int WIFI_NETWORK_WEP	= 1;
	public static final int WIFI_NETWORK_WPA	= 2;

    private WifiManager wifiManager;
    private Context sContext;
    
    //TODO these objects should be able to be set in a constructor
    private String sSsid = "AccessPoint";
    private String sKey = "clusteroffrogs";
    private int sNetType = WIFI_NETWORK_OPEN;
    private boolean sIshidden = false;
    
    public WifiAP(Context context){
    	
    	this.sContext = context;
    	
    	wifiManager = (WifiManager) sContext.getSystemService(Context.WIFI_SERVICE);
 
    }

    /**
     * This method will create the access point.
     */
	public void createWifiAccessPoint() {
		
		if(wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(false);          
		
		WifiConfiguration netConfig = new WifiConfiguration();
		netConfig.SSID = sSsid; 
		netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		netConfig.hiddenSSID = sIshidden;

		if(setWifiApActive(netConfig, true))
			Log.d(TAG, "Access Point created");   
		else 
			Log.d(TAG, "Access Point creation failed");

		while(!(Boolean)isWifiApActive()){};
	}
	
	// TODO create a destoryWifiAccessPoint method also consider making this a static class
	
	/* TODO probably not possible to make a connect method
	 * 
	 * helpful link :p
	 * 	https://android.googlesource.com/platform/frameworks/base/+/jb-release/wifi/java/android/net/wifi/WifiManager.java
	  
	 public void connect(){
	  
	 }
	*/
	
	 /**
	  * Checks if the wifi access point is exists.
	  * @return true if access point is active and false if not.
	  */
	public boolean isWifiApActive(){

		Method isWifiApEnabled = Extract.getMethod(wifiManager.getClass(), "isWifiApEnabled");
		
		if(isWifiApEnabled.equals(null))
			return false;
		
		boolean isEnabled = false;
		
		try {
			isEnabled = (Boolean)isWifiApEnabled.invoke(wifiManager);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return isEnabled;
	}
	
	/**
	 * Allows you to enable or disable (turn on/off) the wifi access point on your device.
	 * @param config is the wifi configuration. Check the android documentation for more info.
	 * @param value set true to activate and false to deactivate.
	 * @return true if active and false if inactive .
	 */
	public boolean setWifiApActive(WifiConfiguration config, boolean value){
		
		Method setWifiApEnabled = Extract.getMethod(wifiManager.getClass(), "setWifiApEnabled");
		
		if(setWifiApEnabled.equals(null))
			return false;
		
		boolean isSet = false;
		
		try {
			isSet = (Boolean) setWifiApEnabled.invoke(wifiManager, config, true);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return isSet;
	}
	
	/**
	 * Gets the current state of the access point.
	 * @return A integer -1 through 4. See list of access point states at the top of the file.
	 */
    public int getWifiApState(){
    	
    	Method getWifiApState = Extract.getMethod(wifiManager.getClass(), "getWifiApState");
    	
    	if(getWifiApState.equals(null))
    		return WIFI_AP_STATE_UNKNOWN;
    	
    	int apstate = WIFI_AP_STATE_UNKNOWN;
    	
    	try {
    		apstate = (Integer)getWifiApState.invoke(wifiManager);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
    	
    	return apstate;
    }
    
    public void setSSID(String ssid){
    	sSsid = ssid;
    }
    
    public void setKey(String key){
    	sKey = key;
    }
    
    public void setNetworkType(int type){
    	sNetType = type;
    }
    
    public void setHidden(boolean hide){
    	sIshidden = hide;
    }
}
