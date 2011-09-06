package android.lokemon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.*;

public class IntroScreen extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private Button next;
	private Button back;
	private RadioButton bulbasaur;
	private RadioButton squirtle;
	private RadioButton charmander;
	
	// first, second and third screen views
	private View [] first;
	private View [] second;
	private View [] third;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro);
        
        // get interface elements
        next = (Button)findViewById(R.id.next_button);
        next.setOnClickListener(this);
        back = (Button)findViewById(R.id.back_button);
        back.setOnClickListener(this);
        bulbasaur = (RadioButton)findViewById(R.id.bulbasaur);
        bulbasaur.setOnCheckedChangeListener(this);
        charmander = (RadioButton)findViewById(R.id.charmander);
        charmander.setOnCheckedChangeListener(this);
        squirtle = (RadioButton)findViewById(R.id.squirtle);
        squirtle.setOnCheckedChangeListener(this);
        
        first = new View [5];
        first[0] = findViewById(R.id.nick_label);
        first[1] = findViewById(R.id.nick_field);
        first[2] = findViewById(R.id.avatar_label);
        first[3] = findViewById(R.id.avatars);
        first[4] = next;
        
        second = new View[6];
        second[0] = back;
        second[1] = next;
        second[2] = findViewById(R.id.poke_label);
        second[3] = findViewById(R.id.pokemon);
        second[4] = findViewById(R.id.poke_description);
        second[5] = findViewById(R.id.poke_stats);
        
        third = new View[0];
        
        enableViews(second, false);
        enableViews(third, false);
        enableViews(first, true);
        
        ((TextView)second[4]).setText(G.basePokemon[0].getDescription());
        ((TextView)second[5]).setText(Html.fromHtml(G.basePokemon[0].getBaseStats()));
    }
    
    public void onClick(View v)
    {
    	if (v == next)
    	{
    		if (back.isEnabled())
    		{
    			
    			enableViews(second, false);
    			// create new player
    			new Trainer(((EditText)first[1]).getText().toString(), (bulbasaur.isChecked()?0:(charmander.isChecked()?3:6)));
    			Trainer.saveTrainer(this);
    			Intent intent = new Intent(v.getContext(), BattleScreen.class);
                startActivityForResult(intent, 0);
    			
                //enableViews(third, true);
    		}
    		else
    		{
    			enableViews(first, false);
    			enableViews(second, true);
    		}
    	}
    	else if (v == back)
    	{
    		enableViews(second, false);
    		enableViews(first, true);
    	}
    }
    
    public void onCheckedChanged(CompoundButton b, boolean checked)
    {
    	if (checked)
    	{
    		TextView textView = (TextView)second[4];
    		TextView stats = (TextView)second[5];
    		if (b == bulbasaur) {textView.setText(G.basePokemon[0].getDescription());
    		stats.setText(Html.fromHtml(G.basePokemon[0].getBaseStats()));}
    		else if (b == charmander) {textView.setText(G.basePokemon[3].getDescription());
    		stats.setText(Html.fromHtml(G.basePokemon[3].getBaseStats()));}
    		else if (b == squirtle){ textView.setText(G.basePokemon[6].getDescription());
    		stats.setText(Html.fromHtml(G.basePokemon[6].getBaseStats()));}
    	}
    }
    
    public void enableViews(View [] array, boolean enable)
    {
    	for (int i = 0; i < array.length; i++)
        {
    		array[i].setEnabled(enable);
        	if (enable) array[i].setVisibility(View.VISIBLE);
        	else array[i].setVisibility(View.GONE);
        }
    }
}
