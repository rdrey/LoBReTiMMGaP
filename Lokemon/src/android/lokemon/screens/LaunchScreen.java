package android.lokemon.screens;

import android.app.Activity;
import android.content.Intent;
import android.lokemon.G;
import android.lokemon.Game;
import android.lokemon.R;
import android.lokemon.G.Mode;
import android.lokemon.R.id;
import android.lokemon.R.layout;
import android.lokemon.game_objects.Trainer;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class LaunchScreen extends Activity implements View.OnClickListener{
	
	private Button new_button;
	private Button continue_button;
	private TextView welcome_text;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // get menu elements
        new_button = (Button)findViewById(R.id.new_button);
        new_button.setOnClickListener(this);
        continue_button = (Button)findViewById(R.id.continue_button);
        continue_button.setOnClickListener(this);
        welcome_text = (TextView)findViewById(R.id.welcome_text);
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
    	if (requestCode == 1)
    	{
    		if (resultCode == RESULT_FIRST_USER)
    		{
		    	Intent intent = new Intent(this.getApplicationContext(), MapScreen.class);
		        startActivity(intent);
    		}
    	}
    }
    
    public void onStart () 
    {
    	// load game data
    	Game.loadGameData(this);
    	// load trainer data if there is any
        Trainer.loadTrainer(this);
    	// check if a player already exists
        if (G.player == null)
        {
        	welcome_text.setText("Welcome, trainer!");
        	continue_button.setEnabled(false);
        	continue_button.setVisibility(View.GONE);
        }
        else
        {
        	welcome_text.setText("Trainer: " + G.player.nickname);
        	continue_button.setEnabled(true);
        	continue_button.setVisibility(View.VISIBLE);
        }
    	super.onStart();
    } 
    
    public void onStop()
    {
    	super.onStop();
    	if (G.debug)
    		Game.saveGameData(this);
    }
    
    public void onClick(View v)
    {
    	if (v == new_button)
    	{
    		Intent intent = new Intent(v.getContext(), IntroScreen.class);
            startActivityForResult(intent,1);
    	}
    	else if (v == continue_button)
    	{
    		Intent intent = new Intent(v.getContext(), MapScreen.class);
            startActivity(intent);
    	}
    }
}