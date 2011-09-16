package android.lokemon.screens;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.lokemon.G;
import android.lokemon.R;
import android.lokemon.G.Mode;
import android.lokemon.G.PlayerState;
import android.lokemon.game_objects.NetworkPlayer;
import android.lokemon.game_objects.Region;
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
        mapView.setMapFile("/sdcard/Lokemon/campus.map");
        
        mapController = mapView.getController();
        // UCT Upper Campus: (-33.957657, 18.46125)
        mapController.setCenter(new GeoPoint(-33.957657,18.46125));
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
        
        players = new ArrayItemizedOverlay(G.player_marker_busy,this) {
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
        // add region ways
        for (Region r:G.game.getRegions())
        	regions.addWay(r.getWay());
        
        // added in drawing order
        mapView.getOverlays().add(regions);
        //mapView.getOverlays().add(shadows_item);
        mapView.getOverlays().add(shadows_player);
        //mapView.getOverlays().add(items);
        mapView.getOverlays().add(players);
        
        // check if this is thread safe
        /*for (NetworkPlayer p:G.game.getAllPlayers())
        {
        	players.addItem(p.getMarker());
        	shadows_player.addItem(p.getShadow());
        }*/
        
        self = this;
        Log.i("Interface", "Map view created");
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
    
    public void onDestroy()
    {
    	super.onDestroy();
    	// have to set player states back to new so that they are re-added to view
    	Log.i("Interface", "Map view destroyed");
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
    		ConcurrentLinkedQueue<NetworkPlayer> new_players = G.game.getNewPlayers();
    		ConcurrentLinkedQueue<NetworkPlayer> old_players = G.game.getOldPlayers();
			while(new_players.size() > 0)
			{
				NetworkPlayer p = new_players.remove(); 
				players.addItem(p.getMarker());
				shadows_player.addItem(p.getShadow());
			}
			while(old_players.size() > 0)
			{
				NetworkPlayer p = old_players.remove();
				players.removeItem(p.getMarker());
				shadows_player.removeItem(p.getShadow());
			}
			Log.i("Interface", "Overlays updated (Markers: " + players.size() + ", Players: " + G.game.getAllPlayers().size() +  ")");
    	}
    }
}
