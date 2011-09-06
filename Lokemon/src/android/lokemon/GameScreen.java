package android.lokemon;

import android.app.Activity;
import android.os.*;
import android.widget.*;
import android.util.Log;
import android.view.*;

public class GameScreen extends Activity implements View.OnClickListener {
	
	private TextView status;
	private ImageView oppPoke;
	private TextView oppHealth;
	private ImageView myPoke;
	private TextView myHealth;
	private TextView oppNick;
	private TextView myNick;
	
	private Button run_button;
	private Button attack_button;
	
	private Spinner moves_spinner;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        status = (TextView)findViewById(R.id.status);
        status.setText("Connecting...");
        
        oppPoke = (ImageView)findViewById(R.id.opp_poke);
        oppHealth = (TextView)findViewById(R.id.opp_health);
        myPoke = (ImageView)findViewById(R.id.my_poke);
        myHealth = (TextView)findViewById(R.id.my_health);
        oppNick = (TextView)findViewById(R.id.opp_name);
        myNick = (TextView)findViewById(R.id.my_name);
        
        removeOpp();
        switch (G.player.pokemon.get(0).index)
        {
        case 0:
        	myPoke.setImageResource(R.drawable.bulbasaur);
        	break;
        case 3:
        	myPoke.setImageResource(R.drawable.charmander);
        	break;
        case 6:
        	myPoke.setImageResource(R.drawable.squirtle);
        	break;
        }
        myHealth.setText("HP: 100");
        myNick.setText(G.player.nickname);
        
        run_button = (Button)findViewById(R.id.run_button);
        run_button.setOnClickListener(this);
        attack_button = (Button)findViewById(R.id.attack_button);
        attack_button.setOnClickListener(this);
        
        moves_spinner = (Spinner)findViewById(R.id.moves_spinner);
        moves_spinner.setPrompt("Select a move");
        enableAttackInterface(false);
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
    
    public void onClick(View v)
    {
    	if (v == attack_button)
    	{
    		setStatusText("You selected " + moves_spinner.getSelectedItem().toString());
    		enableAttackInterface(false);
    		if (moves_spinner.getSelectedItem().toString().equals("Growl"))
    			G.game.attack(0);
    		if (moves_spinner.getSelectedItem().toString().equals("Tackle"))
    			G.game.attack(1);
    	}
    	else if (v == run_button)
    	{
    		G.game.runAway();
    		setStatusText("You ran away...");
    		removeOpp();
    	}
    }
    
    public void setStatusText(String text)
    {
    	status.setText(text);
    }
    
    public void setOppPoke(int index)
    {
    	switch (index)
        {
        case 0:
        	oppPoke.setImageResource(R.drawable.bulbasaur);
        	break;
        case 3:
        	oppPoke.setImageResource(R.drawable.charmander);
        	break;
        case 6:
        	oppPoke.setImageResource(R.drawable.squirtle);
        	break;
        }
    }
    
    public void setOppHealth(int hp)
    {
    	oppHealth.setText("HP: " + hp);
    }
    
    public void setMyHealth(int hp)
    {
    	myHealth.setText("HP: " + hp);
    }
    
    public void setOppNick(String nick)
    {
    	oppNick.setText(nick);
    }
    
    public void removeOpp()
    {
    	oppPoke.setImageDrawable(null);
    	oppHealth.setText("");
        oppNick.setText("");
    }
    
    public void enableAttackInterface(boolean enable)
    {
    	attack_button.setEnabled(enable);
    	run_button.setEnabled(enable);
    	moves_spinner.setEnabled(enable);
    }
}
