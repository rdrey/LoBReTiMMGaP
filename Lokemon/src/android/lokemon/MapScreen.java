package android.lokemon;

import android.lokemon.G.Mode;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.mapsforge.android.maps.*;

public class MapScreen extends MapActivity implements View.OnTouchListener, View.OnClickListener{
    
	private MapController mapController; // used to zoom in/out and pan
	private MapView mapView;
	
	// buttons
	private Button poke_button;
	private Button bag_button;
	private ViewGroup hud;
	
	// map overlays
	private ArrayCircleOverlay players;
	private ArrayItemizedOverlay items;
	private ArrayWayOverlay regions;
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setZoomMin((byte)16);
        mapView.setKeepScreenOn(true);
        mapView.setClickable(true);
        mapView.setOnTouchListener(this);
        mapView.setMapFile("/sdcard/Lokemon/berlin-0.2.4.map");
        
        mapController = mapView.getController();
        // UCT Upper Campus: (-33.957657, 18.46125)
        mapController.setCenter(new GeoPoint(52.52696,13.415701));
        mapController.setZoom(20);
        
        bag_button = (Button)findViewById(R.id.bag_button);
        bag_button.setOnClickListener(this);
        poke_button = (Button)findViewById(R.id.poke_button);
        poke_button.setOnClickListener(this);
        
        hud = (ViewGroup)findViewById(R.id.hud);
        
        // create paint objects for markers
        Paint fill = new Paint();
        fill.setColor(Color.RED);
        fill.setShadowLayer(2.0f, 2.0f, 2.0f, Color.BLACK);
        fill.setStyle(Paint.Style.FILL);
        fill.setAntiAlias(true);
        Paint outline = new Paint();
        outline.setColor(Color.BLACK);
        outline.setStrokeWidth(2.0f);
        outline.setStyle(Paint.Style.STROKE);
        outline.setAntiAlias(true);
        
        // add some overlays
        /*ArrayWayOverlay players = new ArrayWayOverlay(fill, outline);
        GeoPoint [][] points = new GeoPoint[1][4];
        points[0][0] = new GeoPoint(52.52709,13.416012);
        points[0][1] = new GeoPoint(52.527312,13.415609);
        points[0][2] = new GeoPoint(52.527407,13.415114);
        points[0][3] = new GeoPoint(52.327198,13.415969);
        //for (int i = 0; i < points[0].length; i++)
        players.addWay(new OverlayWay(points));
        mapView.getOverlays().add(players);*/
        players = new ArrayCircleOverlay(fill, outline, this) {
        	public boolean onTap(int index) 
        	{
        		showBattleOutgoingDialog(); 
        		return true;
        	}
        };
        players.addCircle(new OverlayCircle(new GeoPoint(52.52709,13.416012),2,"Riz1"));
        players.addCircle(new OverlayCircle(new GeoPoint(52.527312,13.415609),2,"Riz2"));     
        players.addCircle(new OverlayCircle(new GeoPoint(52.527407,13.415114),2,"Riz3"));
        players.addCircle(new OverlayCircle(new GeoPoint(52.327198,13.415969),2,"Riz4"));
        mapView.getOverlays().add(players);
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
    	/*if (e.getAction() == MotionEvent.ACTION_UP)
    	{
    		Intent intent = new Intent(v.getContext(), BattleScreen.class);
            startActivity(intent);
    	}*/
    	return false;
    }
    
    public void showBattleOutgoingDialog()
    {
    	// create alert dialog to ask for outgoing battle confirmation
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Do you want to request a battle?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){}});
	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {dialog.cancel();}});
		AlertDialog alert = builder.create();
		alert.show();
    }
    
    public void showBattleIncomingDialog()
    {
    	// create alert dialog to ask for incoming battle confirmation
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Do you want to battle ");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
			{
	           public void onClick(DialogInterface dialog, int id){}
	       });
	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {dialog.cancel();}
	       });
	    AlertDialog alert = builder.create();
	    alert.show();
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
