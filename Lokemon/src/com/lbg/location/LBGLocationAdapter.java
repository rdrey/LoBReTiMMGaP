package com.lbg.location;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.util.Log;

/**
 * @author Rizmari Versfeld
 * @version 1.0, 10/24/2011
 * 
 * LBGLocationAdapter is an adapter class for android.location.LocationListener, i.e. it implements default functionality for
 * all the methods specified in android.location.LocationListener. An instance of this class can be used to start and stop receiving
 * location updates from either the device's GPS, or a network provider. This class is intended to be used along with an instance of
 * its inner LocationListener interface.
 */
public class LBGLocationAdapter implements LocationListener {
	
	private LocationManager locationManager;
	private int locationProvider;
	private long minTime;
	private float minDistance;
	private LocationListener listener;
	private boolean isTracking;
	
	/**
	 * Only receive updates from device's GPS.
	 */
	public static final int GPS_LOCATION_ONLY = 0;
	
	/**
	 * Receive updates from both the GPS and network providers.
	 */
	public static final int GPS_AND_NETWORK_LOCATION = 1;
	
	/**
	 * Only receive updates from network providers.
	 */
	public static final int NETWORK_LOCATION_ONLY = 2;
	
	/**
	 * This constructor sets both minTime and minDistance to 0, resulting in the highest possible frequency of location updates. It also registers the listener for both GPS and network location updates.
	 * 
	 * @param activity the Android activity that requires location tracking
	 * @param listener an instance of a class that implements the LocationListener interface. The instance will receive location updates.
	 * @throws NullPointerException If either activity or listener is null
	 */
	public LBGLocationAdapter(Activity activity, LocationListener listener)
	{
		this(activity, GPS_AND_NETWORK_LOCATION, 0, 0, listener);
	}
	
	/**
	 * This constructor should be used to limit the frequency of location updates if the application allows it, thus reducing power consumption. It registers the listener for location updates from the specified provider. 
	 * 
	 * @param activity the Android activity that requires location tracking
	 * @param locationProvider specifies the type of location provider. One of the following fields: GPS_LOCATION_ONLY, GPS_AND_NETWORK_LOCATION or NETWORK_LOCATION_ONLY.
	 * @param minTime the minimum time that should go by between location updates. Less regular updates reduce power consumption.
	 * @param minDistance the minimum distance that the device should have moved between location updates. If the device doesn't move much few updates will occur, thus reducing power consumption.
	 * @param listener an instance of a class that implements the LocationListener interface. The instance will receive location updates.
	 * @throws NullPointerException If either activity or listener is null
	 * @throws IllegalArgumentException If locationProvider is not one of GPS_LOCATION_ONLY, GPS_AND_NETWORK_LOCATION or NETWORK_LOCATION_ONLY
	 */
	public LBGLocationAdapter(Activity activity, int locationProvider, long minTime, float minDistance, LocationListener listener)
	{
		if (activity == null || listener == null)
			throw new NullPointerException();
		else if (locationProvider != GPS_LOCATION_ONLY || locationProvider != GPS_AND_NETWORK_LOCATION || locationProvider != NETWORK_LOCATION_ONLY)
			throw new IllegalArgumentException();
		else
		{
			this.locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
			this.locationProvider = locationProvider;
			this.minTime = minTime;
			this.minDistance = minDistance;
			this.listener = listener;
			this.isTracking = false;
		}
	}
	
	// this constructor should not be usable
	private LBGLocationAdapter() {}
	
	/**
	 * Registers with system's LocationManager to receive location updates.
	 */
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
	
	/**
	 * Deregisters with system's LocationManager to stop receiving location updates.
	 */
	public void stopTracking()
	{
		if (isTracking)
		{
			locationManager.removeUpdates(this);
			isTracking = false;
			Log.i("Location", "Tracking stopped");
		}
	}
	
	/**
	 * The default implementation calls listener.onLocationChanged(Location location).
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	public void onLocationChanged(Location location) 
	{ 
		if (listener != null)
			listener.onLocationChanged(location);
		Log.i("Location", location.toString());
	}
	
	/**
	 * The default implementation does nothing.
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	public void onProviderEnabled(String provider) {}
	
	/**
	 * The default implementation does nothing.
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	public void onProviderDisabled(String provider) {}
	
	/**
	 * The default implementation does nothing.
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	/**
	 * @author Rizmari Versfeld
	 * @version 1.0, 10/24/2011
	 * 
	 * LocationListener is a simplified interface, replacing android.location.LocationListener, which can be implemented
	 * to receive regular location updates via its onLocationChanged(Location location) method. This interface must be
	 * used in conjunction with LBGLocationAdapter.
	 */
	public interface LocationListener {
		/**
		 * Called by an instance of LBGLocationAdapter when a new location estimate has been received by the system.
		 * 
		 * @param location an instance of android.location.Location representing the device's current location.
		 */
		public void onLocationChanged(Location location);
	}
}
