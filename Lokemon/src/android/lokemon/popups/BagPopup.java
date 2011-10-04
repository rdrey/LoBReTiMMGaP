package android.lokemon.popups;

import java.util.ArrayList;

import android.content.Context;
import android.lokemon.G;
import android.lokemon.G.BattleType;
import android.lokemon.G.Potions;
import android.lokemon.R;
import android.lokemon.G.Mode;
import android.lokemon.game_objects.BagItem;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class BagPopup extends FadePopup{
	
	private ArrayList<BagItem> entries;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		entries = new ArrayList<BagItem>();
		if (G.mode == Mode.MAP || G.battle.battleType == BattleType.WILD)
		{
			for (BagItem i:G.player.items)
				if (i.getCount() > 0)
					entries.add(i);
		}
		else
		{
			for (int i = 1; i < G.player.items.length; i++)
				if (G.player.items[i].getCount() > 0)
					entries.add(G.player.items[i]);
		}
		setListAdapter(new EntryAdapter(this, R.layout.bag_item, entries));
	}

	public void onListItemClick(ListView l, View v, int pos, long id)
	{
		// returns the index of the item in the player's inventory
		setResult(RESULT_FIRST_USER + entries.get(pos).getIndex());
		finish();
	}
	
	private class EntryAdapter extends ArrayAdapter<BagItem>{
	
        private ArrayList<BagItem> items;

        public EntryAdapter(Context context, int textViewResourceId, ArrayList<BagItem> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	BagItem entry = items.get(position);
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
        	if (G.mode == Mode.MAP && entries.get(position).getIndex()-1 != Potions.HP.ordinal())
        		v.setEnabled(false);
        	return v;
        }
        
        public boolean isEnabled(int position)
        {
        	// if we are in map mode items should not be usable
        	if (G.mode == Mode.MAP)
        	{
        		if (entries.get(position).getIndex()-1 == Potions.HP.ordinal())
        			return true;
        		else
        			return false;
        	}
        	else
        		return true;
        }
	}
}
