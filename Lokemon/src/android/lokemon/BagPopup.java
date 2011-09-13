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

public class BagPopup extends FadePopup{
	
	private ArrayList<Item> entries;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		entries = new ArrayList<Item>();
		for (Item i:G.player.items)
			if (i.getCount() > 0)
				entries.add(i);
		setListAdapter(new EntryAdapter(this, R.layout.bag_item, entries));
	}

	public void onListItemClick(ListView l, View v, int pos, long id)
	{
		if (G.mode == Mode.BATTLE)
		{
			G.battle.useItem(entries.get(pos));
			finish();
		}
	}
	
	private class EntryAdapter extends ArrayAdapter<Item>{
	
        private ArrayList<Item> items;

        public EntryAdapter(Context context, int textViewResourceId, ArrayList<Item> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	Item entry = items.get(position);
        	if (v == null)
        	{
        		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.bag_item, null);             
        	}
        	if (entry != null)
        	{
        		TextView name = (TextView)v.findViewById(R.id.name);
        		TextView desc = (TextView)v.findViewById(R.id.description);
        		TextView count = (TextView)v.findViewById(R.id.count);
        		ImageView icon = (ImageView)v.findViewById(R.id.icon);
        		
        		name.setText(entry.getName());
        		desc.setText(entry.getDescription());
        		count.setText(entry.getCount() + "");
        		icon.setImageResource(entry.getSprite());
        	}
        	if (G.mode == Mode.MAP)
				v.setEnabled(false);
        	return v;
        }
        
        public boolean isEnabled(int position)
        {
        	// if we are in map mode items should not be usable
        	if (G.mode == Mode.MAP)
        		return false;
        	else
        		return true;
        }
	}
}
