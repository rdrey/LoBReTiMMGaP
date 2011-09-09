package android.lokemon;

import java.util.ArrayList;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.lokemon.G.Mode;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class MovesPopup extends ListActivity{
	
	private ArrayList<int[]> entries;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_popup);
		if (G.mode == Mode.BATTLE)
			entries = G.battle.getSelectedPokemon().getMovesAndPP();
		else if (G.mode == Mode.MAP)
			entries = new ArrayList<int[]>(); // get the selected pokemon's moves
		setListAdapter(new EntryAdapter(this, R.layout.move_item, entries));
	}

	public void onListItemClick(ListView l, View v, int pos, long id)
	{
		if (G.mode == Mode.BATTLE)
		{
			G.battle.selectMove(entries.get(pos)[0]);
			Intent intent = new Intent(v.getContext(), Wait.class);
	        startActivityForResult(intent, 0);
			finish();
		}
	}
	
	private class EntryAdapter extends ArrayAdapter<int[]>{
	
        private ArrayList<int[]> items;

        public EntryAdapter(Context context, int textViewResourceId, ArrayList<int[]> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	int[] entry = items.get(position);
        	Move move = G.moves[entry[0]];
        	if (v == null)
        	{
        		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.move_item, null);             
        	}
        	if (entry != null)
        	{
    			ViewGroup strip = (ViewGroup)v.findViewById(R.id.top);
    			strip.setBackgroundColor(move.type.colour_id);
    			TextView t = (TextView)v.findViewById(R.id.type);
    			TextView name = (TextView)v.findViewById(R.id.name);
    			TextView desc = (TextView)v.findViewById(R.id.effect);
    			TextView pp = (TextView)v.findViewById(R.id.pp);
    			TextView power = (TextView)v.findViewById(R.id.power);
    			TextView acc = (TextView)v.findViewById(R.id.accuracy);
    			t.setText(Util.capitalize(move.type.name));
    			name.setText(move.name);
    			desc.setText(move.description);
    			pp.setText(entry[1] + " PP");
    			power.setText(move.power + "");
    			acc.setText(move.accuracy +"%");
        	}
        	return v;
        }
        
        public boolean isEnabled(int position) 
        {
        	// if there is no pp left or we are not in battle the move cannot be selected
        	if (G.mode == Mode.MAP || items.get(position)[1] <= 0)
        		return false;
        	else
        		return true;
        }
	}
}
