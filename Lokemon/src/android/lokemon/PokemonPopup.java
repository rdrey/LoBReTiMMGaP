package android.lokemon;

import java.util.ArrayList;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.lokemon.G.Mode;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class PokemonPopup extends ListActivity{
	
	private ArrayList<Pokemon> entries;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_popup);
		if (G.mode == Mode.MAP)
			entries = G.player.pokemon;
		else if (G.mode == Mode.BATTLE)
		{
			entries = new ArrayList<Pokemon>();
			for (Pokemon p:G.player.pokemon)
				if (p.getHP() > 0 && !p.inBattle) entries.add(p);
		}
		Log.e("battle",this.toString() + " " /*+ entries.toString()*/);
		setListAdapter(new EntryAdapter(this, R.layout.pokemon_item, entries));
	}

	public void onListItemClick(ListView l, View v, int pos, long id)
	{
		if (G.mode == Mode.BATTLE)
		{
			G.battle.switchPlayerPoke(entries.get(pos));
			finish();
		}
		else if (G.mode == Mode.MAP)
		{
			// show moves popup
			Intent intent = new Intent(v.getContext(), MovesPopup.class);
			intent.putExtra("android.lokemon.PokeIndex", pos);
	        startActivity(intent);
		}
	}
	
	public void onPause()
	{
		super.onPause();
		this.setVisible(false);
	}
	
	public void onResume()
	{
		super.onResume();
		this.setVisible(true);
	}
	
	private class EntryAdapter extends ArrayAdapter<Pokemon>{
	
        private ArrayList<Pokemon> items;

        public EntryAdapter(Context context, int textViewResourceId, ArrayList<Pokemon> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	Pokemon entry = items.get(position);
        	if (v == null)
        	{
        		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.pokemon_item, null);             
        	}
        	if (entry != null)
        	{
    			TextView name = (TextView)v.findViewById(R.id.name);
    			TextView hp = (TextView)v.findViewById(R.id.hp);
    			ProgressBar bar = (ProgressBar)v.findViewById(R.id.health_bar);
    			TextView type = (TextView)v.findViewById(R.id.type);
    			TextView attack = (TextView)v.findViewById(R.id.attack);
    			TextView defense = (TextView)v.findViewById(R.id.defense);
    			TextView speed = (TextView)v.findViewById(R.id.speed);
    			TextView special = (TextView)v.findViewById(R.id.special);
    			TextView level = (TextView)v.findViewById(R.id.level);
    			ImageView icon = (ImageView)v.findViewById(R.id.icon);
    			level.setText(entry.getLevel() + "");
    			name.setText(entry.getName());
    			hp.setText(entry.getHP() + "");
    			bar.setProgress((int)(entry.getHP()/(float)entry.getTotalHP()*100.0f));
    			String t = Util.capitalize((entry.getType1().name));
    			if (entry.getType2() != null)t += "/" + Util.capitalize((entry.getType2().name));
    			type.setText(t);
    			v.findViewById(R.id.col_1).setBackgroundColor(entry.getType1().colour_id);
    			if (entry.getType2() == null) 
    				v.findViewById(R.id.col_2).setBackgroundColor(entry.getType1().colour_id);
    			else
    				v.findViewById(R.id.col_2).setBackgroundColor(entry.getType2().colour_id);
    			attack.setText(entry.getAttack() + "");
    			defense.setText(entry.getDefense() + "");
    			special.setText(entry.getSpecial() + "");
    			speed.setText(entry.getSpeed() + "");
    			icon.setImageResource(entry.getSpriteNormal());
        	}
        	return v;
        }
	}
}
