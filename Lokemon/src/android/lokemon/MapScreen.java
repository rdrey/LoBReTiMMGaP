package android.lokemon;

import android.lokemon.G.Mode;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.mapsforge.android.maps.*;

public class MapScreen extends MapActivity implements View.OnTouchListener, View.OnClickListener{
    
	private MapController mapController; // used to zoom in/out and pan
	
	// buttons
	private Button poke_button;
	private Button bag_button;
	private ViewGroup hud;
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        MapView mapView = (MapView)findViewById(R.id.mapview);
        mapView.setZoomMin((byte)16);
        mapView.setKeepScreenOn(true);
        mapView.setClickable(true);
        mapView.setOnTouchListener(this);
        mapView.setMapFile("/sdcard/Lokemon/berlin-0.2.4.map");
        
        mapController = mapView.getController();
        // UCT Upper Campus: (-33.957657, 18.46125)
        mapController.setCenter(new GeoPoint(52.52696,13.415701));
        mapController.setZoom(19);
        
        bag_button = (Button)findViewById(R.id.bag_button);
        bag_button.setOnClickListener(this);
        poke_button = (Button)findViewById(R.id.poke_button);
        poke_button.setOnClickListener(this);
        
        hud = (ViewGroup)findViewById(R.id.hud);
    }
    
    public void onResume()
    {
    	super.onResume();
    	G.mode = Mode.MAP;
    	hud.setVisibility(View.VISIBLE);
    }
    
    public void onPause()
    {
    	super.onPause();
    	hud.setVisibility(View.INVISIBLE);
    }
    
    // we want to disable panning and zooming using gestures (this is the only way with SDK version 2.2)
    public boolean onTouch(View v, MotionEvent e)
    {
    	if (e.getAction() == MotionEvent.ACTION_UP)
    	{
    		Intent intent = new Intent(v.getContext(), BattleScreen.class);
            startActivity(intent);
    	}
    	return true;
    }
    
    public void onClick(View v)
    {
    	if (v == bag_button)
    	{
    		Intent intent = new Intent(v.getContext(), BagPopup.class);
	        startActivity(intent);
    	}
    	else if (v == poke_button)
    	{
    		Intent intent = new Intent(v.getContext(), PokemonPopup.class);
	        startActivity(intent);
    	}
    }
}
