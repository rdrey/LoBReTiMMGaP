package android.lokemon;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import org.mapsforge.android.maps.*;

public class MapScreen extends MapActivity implements View.OnTouchListener{
    
	private MapController mapController; // used to zoom in/out and pan
	
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
    }
    
    // we want to disable panning and zooming using gestures (this is the only way with SDK version 2.2)
    public boolean onTouch(View v, MotionEvent e)
    {
    	if (e.getAction() == MotionEvent.ACTION_UP)
    	{
    		Intent intent = new Intent(v.getContext(), BattleScreen.class);
            startActivityForResult(intent, 0);
    	}
    	return true;
    }
}
