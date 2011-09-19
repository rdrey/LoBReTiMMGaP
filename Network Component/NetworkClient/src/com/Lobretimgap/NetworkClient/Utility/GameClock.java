package com.Lobretimgap.NetworkClient.Utility;

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
	
	public long getTimeDelta() {
		return timeDelta;
	}

	public void setTimeDelta(long timeDelta) {
		this.timeDelta = timeDelta;
	}

	public long currentTimeMillis() {
		return System.currentTimeMillis() - timeDelta;
	}
	
	

}
