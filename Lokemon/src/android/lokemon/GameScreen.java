package android.lokemon;

import android.app.Activity;
import android.os.*;
import android.widget.TextView;

public class GameScreen extends Activity {
	
	private TextView status;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        status = (TextView)findViewById(R.id.status);
        status.setText("Connecting...");
    }
    
    public void onStart () 
    {
    	super.onStart();
    	Game.startGame(this);
    }
    
    public void onStop()
    {
    	Game.endGame();
    	super.onStop();
    }
    
    public void setStatusText(String text)
    {
    	status.setText(text);
    }
}
