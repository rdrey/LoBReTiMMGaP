package com.Lobretimgap.NetworkClient.Utility;

import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;

/***
 * Class to mimic the server game clock. Modifies local time with the difference in 
 * system clocks between server and client, so that accurate syncronisation can be 
 * performed.
 * @author Lawrence
 *
 */
public class GameClock implements TimeSource {

	/***
	 * The time difference we are applying to local time to synchronise with 
	 * server time.
	 */
	private long timeDelta = 0;
	
	private ArrayList<TimeSyncPacket> accumulationList = new ArrayList<GameClock.TimeSyncPacket>();
	
	public long getTimeDelta() {
		return timeDelta;
	}

	public void setTimeDelta(long timeDelta) {
		this.timeDelta = timeDelta;
	}

	public long currentTimeMillis() {
		return System.currentTimeMillis() - timeDelta;
	}
	
	/***
	 * Accumulates a info for a time sync packet. Once a couple have been accumulated,
	 * convergeSyncPackets can be called to converge this gameclock time to the server time.
	 * @param syncPacked
	 */
	public void accumulateSyncPacket(TimeSyncPacket syncPacked)
	{
		accumulationList.add(syncPacked);
		Log.i(NetworkVariables.TAG, "Latency of time request: "+syncPacked.latency+", time delta: "+ syncPacked.timedel);
	}
	
	/***
	 * Method used to remove outliers from accumulated sync packets, and average the time delta.
	 * Will then automatically add the time delta to current time delta to get an accurate estimate 
	 * of the server time.
	 */
	public void convergeSyncPackets()
	{
		
		//Transform to an array so we can sort.
		Object [] workList = accumulationList.toArray();
		Log.i(NetworkVariables.TAG, "Got here!");
		//Sort based on latency (see the comparable interface in TimeSyncPacket)
		Arrays.sort(workList);
		
		//Get the median latency
		long medianLatency = ((TimeSyncPacket)workList[2]).latency;
		Log.i(NetworkVariables.TAG, "Median latency was: "+ medianLatency);
		
		//Now lets work out the standard deviation
		//First we need the mean
		long meanLatency = 0;
		for(Object pct : workList)
		{
			meanLatency += ((TimeSyncPacket)pct).latency;
		}
		
		meanLatency /= workList.length;
		Log.i(NetworkVariables.TAG, "Mean latency was: "+ meanLatency);
		
		//Now we can get the square differences and finally std deviation
		double stdDeviation = 0;
		for(Object pct : workList)
		{
			stdDeviation += (((TimeSyncPacket)pct).latency - meanLatency) * (((TimeSyncPacket)pct).latency - meanLatency);
		}
		stdDeviation /= workList.length;
		stdDeviation = Math.sqrt(stdDeviation);
		Log.i(NetworkVariables.TAG, "Std Deviation was: "+ stdDeviation);
		
		//Ok, now we have the std deviation and the median, so we are going to
		//ignore all samples above approximately 1 standard deviation from the median, and 
		//get the mean of the remaining samples' timeDeltas.
		long averageTimeDelta = 0;
		int deltaCount = 0;
		for(Object pct: workList)
		{
			if(Math.abs(meanLatency - ((TimeSyncPacket)pct).latency) <= stdDeviation)
			{
				averageTimeDelta += ((TimeSyncPacket)pct).timedel;
				deltaCount++;
			}
		}
		averageTimeDelta /= deltaCount;
		Log.i(NetworkVariables.TAG, "Average Time Delta was: "+ averageTimeDelta);
		//Finally lets update the clock with this converged estimate for the client-server time delta
		timeDelta += averageTimeDelta;
		accumulationList.clear();
	}
	
	public class TimeSyncPacket implements Comparable<TimeSyncPacket>
	{
		public long latency;
		public long timedel;
		public TimeSyncPacket(long latency, long timeDelta)
		{
			this.latency = latency;
			timedel = timeDelta;
		}
		public int compareTo(TimeSyncPacket another) {
			return (int) (latency - another.latency);			
		}
	}
	
	

}
