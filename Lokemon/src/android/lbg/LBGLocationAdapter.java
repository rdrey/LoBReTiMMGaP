package android.lbg;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.util.Log;

public class LBGLocationAdapter implements LocationListener {
	
	private LocationManager locationManager;
	private int locationProvider;
	private long minTime;
	private float minDistance;
	private LocationListener listener;
	private boolean isTracking;
	
	// location provider options
	public static final int GPS_LOCATION_ONLY = 0;
	public static final int GPS_AND_NETWORK_LOCATION = 1;
	public static final int NETWORK_LOCATION_ONLY = 2;
	
	// error codes
	
	public LBGLocationAdapter(Activity activity, LocationListener listener)
	{
		this(activity, GPS_AND_NETWORK_LOCATION, 0, 0, listener);
	}
	
	public LBGLocationAdapter(Activity activity, int locationProvider, long minTime, float minDistance, LocationListener listener)
	{
		this.locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		this.locationProvider = locationProvider;
		this.minTime = minTime;
		this.minDistance = minDistance;
		this.listener = listener;
		this.isTracking = false;
	}
	
	public void startTracking()
	{
		if (!isTracking)
		{
			switch (locationProvider)
			{
			case NETWORK_LOCATION_ONLY:
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
				break;
			case GPS_LOCATION_ONLY:
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
				break;
			case GPS_AND_NETWORK_LOCATION:
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
		        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
				break;
			}
			isTracking = true;
			Log.i("Location", "Tracking started");
		}
	}
	
	public void stopTracking()
	{
		if (isTracking)
		{
			locationManager.removeUpdates(this);
			isTracking = false;
			Log.i("Location", "Tracking stopped");
		}
	}
	
	public void onLocationChanged(Location location) 
	{ 
		if (listener != null)
			listener.onLocationChanged(location);
		Log.i("Location", location.toString());
	}
	
	public void onProviderEnabled(String provider) {}
	public void onProviderDisabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	public interface LocationListener {
		public void onLocationChanged(Location location);
		public void onLocationError(int errorCode);
	}
}
