package android.lokemon.screens;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.lokemon.Battle;
import android.lokemon.G;
import android.lokemon.G.BattleMove;
import android.lokemon.G.BattleType;
import android.lokemon.G.Gender;
import android.lokemon.R;
import android.lokemon.game_objects.Pokemon;
import android.lokemon.popups.BagPopup;
import android.lokemon.popups.MovesPopup;
import android.lokemon.popups.PokemonPopup;
import android.os.Bundle;
import android.widget.*;
import android.util.Log;
import android.view.*;

public class BattleScreen extends Activity implements View.OnClickListener{
	
	// battle action buttons
	private Button switch_button;
	private Button attack_button;
	private Button bag_button;
	private Button run_button;
	private ViewGroup battle_buttons;
	private Gender opp_gender;
	private String opp_nick;
	
	// display widgets
	private ImageView player_poke;
	private TextView player_name;
	private TextView player_hp;
	private ProgressBar player_bar;
	private TextView player_level;
	private ImageView opp_poke;
	private TextView opp_name;
	private TextView opp_hp;
	private ProgressBar opp_bar;
	private TextView opp_level;
	
	private ImageView pokeballs;
	
	// dialogs
	private ProgressDialog progressDialog;
	private Toast toast;
	
	private boolean battle_finish_normal;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.battle);
        
        // get battle buttons
        switch_button = (Button)findViewById(R.id.switch_button);
        switch_button.setOnClickListener(this);
        attack_button = (Button)findViewById(R.id.attack_button);
        attack_button.setOnClickListener(this);
        bag_button = (Button)findViewById(R.id.bag_button);
        bag_button.setOnClickListener(this);
        run_button = (Button)findViewById(R.id.run_button);
        run_button.setOnClickListener(this);
        battle_buttons = (ViewGroup)findViewById(R.id.buttons);
        
        // get Pokemon display widgets
        player_poke = (ImageView)findViewById(R.id.my_poke);
        player_name = (TextView)findViewById(R.id.my_name);
        player_level = (TextView)findViewById(R.id.my_level);
        player_hp = (TextView)findViewById(R.id.my_health_num);
        player_bar = (ProgressBar)findViewById(R.id.my_health);
        opp_poke = (ImageView)findViewById(R.id.opp_poke);
        opp_name = (TextView)findViewById(R.id.opp_name);
        opp_level = (TextView)findViewById(R.id.opp_level);
        opp_hp = (TextView)findViewById(R.id.opp_health_num);
        opp_bar = (ProgressBar)findViewById(R.id.opp_health);
        
        pokeballs = (ImageView)findViewById(R.id.pokeballs);
        
        toast  = Toast.makeText(this, "", Toast.LENGTH_LONG);
        
        // initiate battle correctly
        Bundle data = getIntent().getExtras();
        if (data.getString("battle").equals("trainer"))
        {
        	opp_gender = Gender.values()[data.getInt("gender")];
        	opp_nick = data.getString("nick");
        	Pokemon opp_start = new Pokemon(data.getInt("index"), data.getInt("hp"), 0, data.getInt("level"), null, null, data.getIntArray("stats"), null);
        	new Battle(this, opp_start,data.getInt("seed"));
        }
        else
        	new Battle(this);
	}
	
	public void onClick(View v)
	{
		if (v == switch_button)
		{
			Intent intent = new Intent(v.getContext(), PokemonPopup.class);
	        startActivityForResult(intent, 1);
		}
		else if (v == attack_button)
		{
			Intent intent = new Intent(v.getContext(), MovesPopup.class);
	        startActivityForResult(intent, 2);
		}
		else if (v == bag_button)
		{
			Intent intent = new Intent(v.getContext(), BagPopup.class);
	        startActivityForResult(intent, 3);
		}
		else if (v == run_button)
			onBackPressed();
	}
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		switch(requestCode)
		{
		case 1:
			if (resultCode != RESULT_CANCELED)
				G.battle.switchPlayerPoke(resultCode - RESULT_FIRST_USER);
			break;
		case 2:
			if (resultCode != RESULT_CANCELED)
				G.battle.selectMove(resultCode - RESULT_FIRST_USER);
			break;
		case 3:
			if (resultCode != RESULT_CANCELED)
				G.battle.useItem(resultCode - RESULT_FIRST_USER);
			break;
		}
	}
	
	// players have to leave battle by explicitly running away
	public void onBackPressed()
	{
		// create an alert dialog to ask for confirmation
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to run away?");
		builder.setCancelable(false);
		builder.setPositiveButton("Run!", new DialogInterface.OnClickListener() 
			{
	           public void onClick(DialogInterface dialog, int id)  {G.battle.run(); }
	       })
	       .setNegativeButton("Stay", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	           }
	       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public void onPause()
	{
		super.onPause();
		showBattleInterface(false);
	}
	
	public void onStop()
	{
		super.onStop();
		if (!battle_finish_normal)
		{
			if (G.battle.battleType == BattleType.TRAINER)
				G.game.sendSimpleBattleMessage(BattleMove.DISCONNECTED, -1);
			setResult(RESULT_CANCELED);
			endBattle();
			Log.i("Battle", "Battle stopping abnormally");
		}
		Log.i("Battle", "Battle activity stopped");
	}
	
	public void onResume()
	{
		super.onResume();
		showBattleInterface(true);
	}
	
	// display new player Pokemon
	public void switchPlayerPoke(Pokemon newPoke)
	{
		player_poke.setImageResource(newPoke.getSpriteAttack());
		setPlayerPokeDetails(newPoke);
	}
	
	// display new opponent Pokemon
	public void switchOppPoke(Pokemon newPoke)
	{
		opp_poke.setImageResource(newPoke.getSpriteNormal());
		setOppPokeDetails(newPoke);
	}
	
	public void showProgressDialog(String message)
    {
		progressDialog = ProgressDialog.show(this, "", message, true, true, new OnCancelListener(){
    		public void onCancel(DialogInterface dialog)
    		{G.game.sendCancelMessage();
    		showToast("You canceled the battle");
    		endBattle();
    		progressDialog.dismiss();}
    	});
    	progressDialog.show();
    }
	
	public void cancelProgressDialog() {progressDialog.dismiss();}
	
	// display new player Pokemon stats
	public void setPlayerPokeDetails(Pokemon poke)
	{
		player_name.setText(poke.getName());
		player_level.setText("Lv. " + poke.getLevel());
		int hp_c = poke.getHP();
		int hp_t = poke.getTotalHP();
		player_hp.setText(hp_c + "/" + hp_t);
		player_bar.setProgress((int)(hp_c/(float)hp_t * 100.0f));
	}
	
	// display new opponent Pokemon stats
	public void setOppPokeDetails(Pokemon poke)
	{
		opp_name.setText(poke.getName());
		opp_level.setText("Lv. " + poke.getLevel());
		int hp_c = poke.getHP();
		int hp_t = poke.getTotalHP();
		opp_hp.setText(hp_c + "/" + hp_t);
		opp_bar.setProgress((int)(hp_c/(float)hp_t * 100.0f));
	}
	
	public void showBattleInterface(boolean show)
	{
		int state = show?View.VISIBLE:View.INVISIBLE;
		battle_buttons.setVisibility(state);
	}
	
	public void setNumPokemon(int num)
	{
		pokeballs.getDrawable().setLevel(num);
	}
	
	public void disableSwitch() 
	{
		switch_button.setClickable(false);
		switch_button.getBackground().setAlpha(128);
	}
	
	public void disableBag() 
	{
		bag_button.setClickable(false);
		bag_button.getBackground().setAlpha(128);
	}
	
	public void endBattle()
	{
		battle_finish_normal = true;
		finish();
	}
	
	public void showToast(String message)
	{
		toast.setText(message);
    	toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
    	toast.show();
	}
	
	public String getOppNick() {return opp_nick;}
	public Gender getOppGender() {return opp_gender;}
}
