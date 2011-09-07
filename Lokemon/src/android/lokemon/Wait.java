package android.lokemon;

import android.app.Activity;
import android.os.Bundle;

public class Wait extends Activity {
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wait);
	}
	
	// the player cannot go back to the battle screen before getting network reply
	public void onBackPressed(){return;}
}
