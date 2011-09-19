package android.lokemon.screens;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.location.Location;
import android.lokemon.G;
import android.lokemon.G.TestMode;
import android.lokemon.Game;
import android.lokemon.R;
import android.lokemon.G.Mode;
import android.lokemon.game_objects.*;
import android.lokemon.game_objects.Region;
import android.lokemon.popups.BagPopup;
import android.lokemon.popups.PokemonPopup;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import org.mapsforge.android.maps.*;

import com.example.android.apis.graphics.AnimateDrawable;

public class MapScreen extends MapActivity implements View.OnClickListener{
    
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
	private ArrayItemizedOverlay trainer_aura;
	private ArrayCircleOverlay trainer_circle;
	private ArrayItemizedOverlay shadows_player;
	private ArrayItemizedOverlay players;
	private ArrayItemizedOverlay items;
	
	// map animation variables
	private GeoPoint start;
	private GeoPoint end;
	private Location end_loc;
	private Timer animTimer;
	private long lastTime;
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setKeepScreenOn(true);
        mapView.setZoomMax((byte)19);
        mapView.setZoomMin((byte)19);
        mapView.setClickable(true);
        
        animTimer = new Timer();
        
        // we want to disable panning and zooming using gestures (this is the only way with SDK version 2.2)
        mapView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getAction() == MotionEvent.ACTION_MOVE)
		    		return true;
				else if (G.testMode == TestMode.CONTROL && SystemClock.uptimeMillis() - e.getDownTime() > 500)
				{
					animTimer.cancel();					
					GeoPoint point = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
					end = point;		
					end_loc = new Location("");
					end_loc.setLatitude(end.getLatitude());
					end_loc.setLongitude(end.getLatitude());
					animTimer = new Timer();
					lastTime = SystemClock.uptimeMillis();
					animTimer.scheduleAtFixedRate(new TimerTask(){
						public void run()
						{
							double timeStep = (SystemClock.uptimeMillis() - lastTime) / 333.0f;
							lastTime = SystemClock.uptimeMillis();
							double newLat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * timeStep;
							double newLon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * timeStep;
							start = new GeoPoint(newLat, newLon);
							Location loc = new Location("");
							loc.setLatitude(start.getLatitude());
							loc.setLongitude(start.getLongitude());
							if (loc.distanceTo(end_loc) < 5)
							{
								G.player.setLocation (end_loc);
								mapController.setCenter(end);
								cancel();
							}
							else
							{
								G.player.setLocation(loc);
								mapController.setCenter(start);
							}
						}
					}, 60, 60);
					// if framerate becomes a big problem don't animate
					/*G.player.setLocation (end_loc);
					mapController.setCenter(end);*/
					return true;
				}
				else
		    		return false;
			}
		});
        mapView.setMapFile("/sdcard/Lokemon/campus.map");
        
        mapController = mapView.getController();
        // UCT Upper Campus: (-33.957657, 18.46125)
        mapController.setCenter(new GeoPoint(-33.957657,18.46125));
        start = new GeoPoint(-33.957657,18.46125);
        mapController.setZoom(19);
        
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
        		G.game.requestBattle(index);
        		return true;
        	}
        };
        ItemizedOverlay.boundCenterBottom(G.player_marker_busy);
        
        shadows_player = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.marker_shadow), this);
        items = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.marker_item),this);
        regions = new ArrayWayOverlay(null, null);
        //setupTrainerAura();
        setupTrainerCircle();
        
        // add trainer aura
        //trainer_aura.addItem(G.player.aura);
        trainer_circle.addCircle(G.player.circle);
        
        // added in drawing order
        mapView.getOverlays().add(regions);
        //mapView.getOverlays().add(trainer_aura);
        mapView.getOverlays().add(trainer_circle);
        mapView.getOverlays().add(shadows_player);
        mapView.getOverlays().add(players);
        mapView.getOverlays().add(items);
        
        // check if this is thread safe
        /*for (NetworkPlayer p:G.game.getAllPlayers())
        {
        	players.addItem(p.getMarker());
        	shadows_player.addItem(p.getShadow());
        }*/
        
        Log.i("Interface", "Map view created");
        
        new Game(this);
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
    
    public void onDestroy()
    {
    	super.onDestroy();
    	Log.i("Interface", "Map view destroyed");
    }
    
    // create alert dialog to ask for outgoing battle confirmation
    public void showBattleOutgoingDialog(String playerName)
    {
    	battleAlert.setMessage(Html.fromHtml("Do you want to ask <i>" + playerName + "</i> to battle?"));
    	battleAlert.setButton("Send request", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){}});
    	battleAlert.setButton2("Don't send", new DialogInterface.OnClickListener() 
			{public void onClick(DialogInterface dialog, int id){dialog.cancel();}});
		battleAlert.show();
    }
    
    // create alert dialog to ask for incoming battle confirmation
    public void showBattleIncomingDialog(String playerName)
    {
    	battleAlert.setMessage(Html.fromHtml("Do you want to battle <i>" + playerName + "</i>?"));
		battleAlert.setButton("Battle!", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){}});
		battleAlert.setButton2("Don't battle", new DialogInterface.OnClickListener() 
		{public void onClick(DialogInterface dialog, int id){dialog.cancel();}});
		battleAlert.show();
    }
    
    // create alert dialog to notify the user of important events
    public void showToast(String message) 
    {
    	Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
    	toast.show();
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
    
    private void setupTrainerAura()
    {
    	BitmapDrawable aura = (BitmapDrawable)getResources().getDrawable(R.drawable.aura);
        aura.setAntiAlias(true);
        aura.setDither(true);
        Display display = getWindowManager().getDefaultDisplay();
        int left = (display.getWidth()-aura.getIntrinsicWidth())/2;
        int top = (display.getHeight()-aura.getIntrinsicHeight())/2;
        aura.setBounds(left, top, left+aura.getIntrinsicWidth(), top+aura.getIntrinsicHeight());
        RotateAnimation anim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_PARENT,0.5f,Animation.RELATIVE_TO_PARENT, 0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(4000);
        anim.setRepeatCount(Animation.INFINITE);
        anim.setRepeatMode(Animation.RESTART);
        anim.initialize(aura.getIntrinsicWidth(), aura.getIntrinsicHeight(), display.getWidth(), display.getHeight());
        AnimateDrawable animAura = new AnimateDrawable(aura, anim);
        
        trainer_aura = new ArrayItemizedOverlay(animAura, this) {
        	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel)
        	{
        		super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
        		requestRedraw();
        	}
        };
        ItemizedOverlay.boundCenter(animAura);
        anim.startNow();
    }
    
    private void setupTrainerCircle()
    {
    	Paint fill = new Paint();
    	fill.setStyle(Style.FILL);
    	fill.setColor(Color.MAGENTA);
    	fill.setAlpha(32);
    	fill.setAntiAlias(true);
    	Paint outline = new Paint();
    	outline.setStyle(Style.STROKE);
    	outline.setStrokeWidth(2);
    	outline.setColor(Color.MAGENTA);
    	outline.setAntiAlias(true);
    	trainer_circle = new ArrayCircleOverlay(fill,outline,this);	
    }
    
    public void addPlayer(NetworkPlayer p)
    {
    	players.addItem(p.getMarker());
		shadows_player.addItem(p.getShadow());
    }
    
    public void removePlayer(NetworkPlayer p)
    {
    	players.removeItem(p.getMarker());
		shadows_player.removeItem(p.getShadow());
    }
    
    public void addItem(WorldPotion p)
    {
    	items.addItem(p.getMarker());
    }
    
    public void removeItem(WorldPotion p)
    {
    	items.removeItem(p.getMarker());
    }
    
    // add region ways
    public void addRegions(List<Region> regions)
    {
        for (Region r:regions)
        	this.regions.addWay(r.getWay());
    }
}
