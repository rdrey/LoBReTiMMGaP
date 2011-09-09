package android.lokemon;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.lokemon.G.Mode;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class BagPopup extends ListActivity{
	
	private ArrayList<Item> entries;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_popup);
		entries = new ArrayList<Item>(Arrays.asList(G.player.items));
		setListAdapter(new EntryAdapter(this, R.layout.bag_item, entries));
	}

	public void onListItemClick(ListView l, View v, int pos, long id)
	{
		/*if (G.mode == Mode.BATTLE)
		{
			G.battle.selectMove(entries.get(pos)[0]);
			Intent intent = new Intent(v.getContext(), Wait.class);
	        startActivityForResult(intent, 0);
			finish();
		}*/
	}
	
	private class EntryAdapter extends ArrayAdapter<Item>{
	
        private ArrayList<Item> items;

        public EntryAdapter(Context context, int textViewResourceId, ArrayList<Item> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	//int[] entry = items.get(position);
        	//Move move = G.moves[entry[0]];
        	if (v == null)
        	{
        		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.bag_item, null);             
        	}
        	/*if (entry != null)
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
    			// if there is no pp left or we are not in battle the move cannot be selected
    			if (entry[1] <= 0 || G.mode == Mode.MAP) 
    			{
    				v.setClickable(false);
    				v.setEnabled(false);
    			}
    			power.setText(move.power + "");
    			acc.setText(move.accuracy +"%");
        	}*/
        	return v;
        }
	}
}
