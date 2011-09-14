package android.lokemon.screens;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import android.lokemon.G;
import android.lokemon.R;
import android.lokemon.G.Mode;
import android.lokemon.R.drawable;
import android.lokemon.R.id;
import android.lokemon.R.layout;
import android.lokemon.game_objects.NetworkPlayer;
import android.lokemon.popups.BagPopup;
import android.lokemon.popups.PokemonPopup;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.mapsforge.android.maps.*;

public class MapScreen extends MapActivity implements View.OnTouchListener, View.OnClickListener{
    
	private MapScreen self; // for use in inner classes
	private MapController mapController; // used to zoom in/out and pan
	private MapView mapView;
	
	// buttons
	private Button poke_button;
	private Button bag_button;
	private ViewGroup hud;
	
	// battle alert dialog
	private AlertDialog battleAlert;
	
	// map overlays (declared in drawing order)
	private ArrayWayOverlay regions;
	private ArrayItemizedOverlay shadows_player;
	private ArrayItemizedOverlay shadows_item;
	private ArrayItemizedOverlay players;
	private ArrayItemizedOverlay items;
	
	// timer used to update the overlays
	private Timer overlayTimer;
	
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
        mapController.setZoom(18);
        
        bag_button = (Button)findViewById(R.id.bag_button);
        bag_button.setOnClickListener(this);
        poke_button = (Button)findViewById(R.id.poke_button);
        poke_button.setOnClickListener(this);
        
        hud = (ViewGroup)findViewById(R.id.hud);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {dialog.cancel();}});
        battleAlert = builder.create();
        
        players = new ArrayItemizedOverlay(G.player_marker_available,this) {
        	public boolean onTap(int index) 
        	{
        		showBattleOutgoingDialog(); 
        		return true;
        	}
        };
        shadows_player = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.marker_shadow), this);
        items = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.pokeball),this);
        shadows_item = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.marker_shadow), this);
        regions = new ArrayWayOverlay(null, null);
        
        // added in drawing order
        //mapView.getOverlays().add(regions);
        mapView.getOverlays().add(shadows_player);
        //mapView.getOverlays().add(shadows_item);
        mapView.getOverlays().add(players);
        //mapView.getOverlays().add(items);
        
        self = this;
    }
    
    public void onResume()
    {
    	super.onResume();
    	G.mode = Mode.MAP;
    	hud.setVisibility(View.VISIBLE);
    	overlayTimer = new Timer();
    	overlayTimer.scheduleAtFixedRate(new OverlayUpdate(),0, 1000);
    }
    
    public void onPause()
    {
    	super.onPause();
    	hud.setVisibility(View.INVISIBLE);
    	overlayTimer.cancel();
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
    	battleAlert.setTitle("Battle Request");
    	battleAlert.setMessage("Do you want to request a battle?");
    	battleAlert.setButton("Yes", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){}});
		battleAlert.show();
    }
    
    public void showBattleIncomingDialog()
    {
    	// create alert dialog to ask for incoming battle confirmation
    	battleAlert.setTitle("Battle Offer");
    	battleAlert.setMessage("Do you want to enter into battle?");
		battleAlert.setButton("Yes", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){}});
		battleAlert.show();
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
    
    private class OverlayUpdate extends TimerTask
    {
    	public void run()
    	{
    		LinkedList<NetworkPlayer> list1 = G.game.getOldPlayers();
    		while (list1.size() > 0)
    		{
    			NetworkPlayer p = list1.removeLast();
    			shadows_player.removeItem(p.getShadow());
    			players.removeItem(p.getMarker());
    		}
    		LinkedList<NetworkPlayer> list2 = G.game.getNewPlayers();
    		while (list2.size() > 0)
    		{
    			NetworkPlayer p = list2.removeLast();
    			shadows_player.addItem(p.getShadow());
    			players.addItem(p.getMarker());
    		}
    		Log.i("Interface", "Overlays updated (Markers: " + players.size() + ", Players: " + G.game.getAllPlayers().size() +  ")");
    	}
    }
}
