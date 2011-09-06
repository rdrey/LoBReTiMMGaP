package android.lokemon;

import java.util.ArrayList;
import android.app.ListActivity;
import android.content.Context;
import android.lokemon.G.Mode;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class MovesPopup extends ListActivity{
	
	private ArrayList<Move> entries;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_popup);
		entries = new ArrayList<Move>();
		for (int i = 0; i < 6; i++) entries.add(new Move());
		setListAdapter(new EntryAdapter(this, R.layout.pokemon_item, entries));
	}

	public void onListItemClick(ListView l, View v, int pos, long id)
	{
		// battle.attack
		finish();
	}
	
	private class EntryAdapter extends ArrayAdapter<Move>{
	
        private ArrayList<Move> items;

        public EntryAdapter(Context context, int textViewResourceId, ArrayList<Move> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	Move entry = items.get(position);
        	if (v == null)
        	{
        		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.move_item, null);             
        	}
        	if (entry != null)
        	{
    			ViewGroup strip = (ViewGroup)v.findViewById(R.id.top);
    			strip.setBackgroundColor(G.types[(int)(Math.random() * 12)].colour_id);
        	}
        	return v;
        }
	}
}
