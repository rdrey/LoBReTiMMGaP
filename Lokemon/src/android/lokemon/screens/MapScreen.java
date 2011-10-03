package android.lokemon.screens;

import java.util.ArrayList;
import android.location.Location;
import android.lokemon.G;
import android.lokemon.G.TestMode;
import android.lokemon.Game;
import android.lokemon.R;
import android.lokemon.G.Mode;
import android.lokemon.Util;
import android.lokemon.game_objects.*;
import android.lokemon.game_objects.Region;
import android.lokemon.popups.BagPopup;
import android.lokemon.popups.PokemonPopup;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.lbg.*;
import networkTransferObjects.NetworkMessageMedium;

import org.mapsforge.android.maps.*;
import com.example.android.apis.graphics.AnimateDrawable;

public class MapScreen extends MapActivity implements View.OnClickListener{
    
	private MapController mapController; // used to zoom in/out and pan
	private MapView mapView;
	
	// buttons
	private Button poke_button;
	private Button bag_button;
	private TextView coins;
	private TextView rank;
	private ViewGroup hud;
	private TextView persistent_alert;
	
	// dialogs
	private AlertDialog battleAlert;
	private ProgressDialog progressDialog;
	private Toast toast;
	
	// map overlays (declared in drawing order)
	private ArrayWayOverlay regions;
	private ArrayItemizedOverlay trainer_aura;
	private ArrayCircleOverlay trainer_circle;
	private ArrayItemizedOverlay shadows_player;
	private ArrayItemizedOverlay players;
	private ArrayItemizedOverlay items;
	
	// corresponding lists for ids (fuck fuck fuck)
	private ArrayList<Integer> playerIDs;
	private ArrayList<Integer> itemIDs;
	
	// map animation variables
	private GeoPoint start;
	private GeoPoint end;
	private Location end_loc;
	private long lastTime;
	private Runnable animator;
	private Handler animHandler;
	private Runnable redraw;
	private Handler redrawHandler;
	
	// GPS variables
	LBGLocationAdapter location_adapter;
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setKeepScreenOn(true);
        mapView.setZoomMax((byte)19);
        mapView.setZoomMin((byte)19);
        mapView.setClickable(true);
        mapView.setMapFile(G.mapDir + G.mapFile);
        // animation & redraw runnables
        animator = new Runnable(){
			public void run() {
				double timeStep = (System.currentTimeMillis() - lastTime) * 0.003;
				lastTime = System.currentTimeMillis();
				if (end != null)
				{
					double newLat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * timeStep;
					double newLon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * timeStep;
					start = new GeoPoint(newLat, newLon);
					Location loc = Util.fromGeoPoint(start);
					if (loc.distanceTo(end_loc) < 1)
					{
						G.player.setLocation (end_loc);
						mapController.setCenter(end);
						end = null;
						Log.i("Interface", "Animation done");
					}
					else
					{
						G.player.setLocation(loc);
						mapController.setCenter(start);
					}
				}
				trainer_aura.requestRedraw();
				animHandler.postDelayed(animator, 50);
			}        	
        };
        
        redraw = new Runnable(){
        	public void run(){
        		players.requestRedraw();
        		shadows_player.requestRedraw();
        		redrawHandler.postDelayed(redraw, 1000);
        	}
        };
        animHandler = new Handler();
        redrawHandler = new Handler();
        
        // we want to disable panning and zooming using gestures (this is the only way with SDK version 2.2)
        mapView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getAction() == MotionEvent.ACTION_MOVE)
		    		return true;
				else if (e.getAction() == MotionEvent.ACTION_UP)
				{
					if (G.testMode == TestMode.CONTROL)
					{
						if (SystemClock.uptimeMillis() - e.getDownTime() > 500)
							return false;
						else
						{			
							GeoPoint point = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
							G.game.onLocationChanged(Util.fromGeoPoint(point));
							// if framerate becomes a big problem don't animate
							/*G.player.setLocation (end_loc);
							mapController.setCenter(end);*/
							return true;
						}
					}
					else
						return false;
				}
				else
					return false;
			}
		});
        
        mapController = mapView.getController();
        // UCT Upper Campus: (-33.957657, 18.46125)
        mapController.setCenter(new GeoPoint(-33.957657,18.46125));
        start = new GeoPoint(-33.957657,18.46125);
        mapController.setZoom(19);
        
        bag_button = (Button)findViewById(R.id.bag_button);
        bag_button.setOnClickListener(this);
        poke_button = (Button)findViewById(R.id.poke_button);
        poke_button.setOnClickListener(this);
        coins = (TextView)findViewById(R.id.coins_label);
        coins.setText(G.player.coins + "");
        rank = (TextView)findViewById(R.id.rank_label);
        rank.setText("?");
        persistent_alert = (TextView)findViewById(R.id.persistent_alert);
        persistent_alert.setText("Your Pokémon need to be\nhealed at a Pokémon Center!");
        persistent_alert.setVisibility(View.INVISIBLE);
        
        hud = (ViewGroup)findViewById(R.id.hud);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {dialog.cancel();}});
        battleAlert = builder.create();
        
        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
    	progressDialog.setCancelable(false);
    	toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
    	playerIDs = new ArrayList<Integer>();
    	itemIDs = new ArrayList<Integer>();
    	
        players = new ArrayItemizedOverlay(G.player_marker_available,this) {
        	public boolean onTap(int index) 
        	{
        		G.game.requestPlayer(playerIDs.get(index));
        		return true;
        	}
        };
        ItemizedOverlay.boundCenterBottom(G.player_marker_busy);
        
        shadows_player = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.marker_shadow), this);
        items = new ArrayItemizedOverlay(getResources().getDrawable(R.drawable.marker_item),this) {
        	public boolean onTap(int index) 
        	{
        		G.game.requestItem(itemIDs.get(index));
        		return true;
        	}
        };
        ItemizedOverlay.boundCenter(getResources().getDrawable(R.drawable.marker_item));
        
        regions = new ArrayWayOverlay(null, null);
        setupTrainerAura();
        setupTrainerCircle();
        
        // add trainer aura
        trainer_aura.addItem(G.player.aura);
        trainer_circle.addCircle(G.player.circle);
        
        // added in drawing order
        mapView.getOverlays().add(regions);
        mapView.getOverlays().add(trainer_circle);
        mapView.getOverlays().add(trainer_aura);
        mapView.getOverlays().add(shadows_player);
        mapView.getOverlays().add(players);
        mapView.getOverlays().add(items);
        
        new Game(this);
        
        // only use GPS for experimental group
        if (G.testMode == TestMode.EXPERIMENT)
        	location_adapter = new LBGLocationAdapter(this, LBGLocationAdapter.GPS_LOCATION_ONLY, 0, 2, G.game);
        
        G.game.createConnection();
    }
    
    protected void onResume()
    {
    	super.onResume();
    	G.mode = Mode.MAP;
    	hud.setVisibility(View.VISIBLE);
    	
    	if (G.testMode == TestMode.EXPERIMENT)
    		location_adapter.startTracking();
    	
    	lastTime = System.currentTimeMillis();
    	animHandler.postDelayed(animator, 50);
    	redrawHandler.postDelayed(redraw, 1000);
    }
    
    protected void onPause()
    {
    	super.onPause();
    	hud.setVisibility(View.INVISIBLE);
    	
    	if (G.testMode == TestMode.EXPERIMENT)
    		location_adapter.stopTracking();
    	
    	animHandler.removeCallbacks(animator);
    	redrawHandler.removeCallbacks(redraw);
    }
    
    public void onBackPressed()
    {
    	Trainer.saveTrainer(this);
    	Log.i("Data save", "Map screen stopped and data saved.");
    	finish();
    }
    
    public void onStop()
    {
    	super.onStop();
    	Trainer.saveTrainer(this);
    	Log.i("Data save", "Map screen stopped and data saved.");
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
    	G.game.finalizeBattle();
    }
    
    public void switchToBattle(NetworkMessageMedium battleInitMessage, int seed)
    {
    	if (progressDialog.isShowing())
    		progressDialog.dismiss();
    	Intent intent = new Intent(this, BattleScreen.class);
    	if (battleInitMessage != null)
    	{
    		intent.putExtra("battle","trainer");
    		int [] stats = new int[5];
    		for (int i = 0; i < 5; i++)
    			stats[i] = battleInitMessage.integers.get(6+i);
	    	intent.putExtra("stats", stats);
	    	intent.putExtra("index", battleInitMessage.integers.get(3));
	    	intent.putExtra("level", battleInitMessage.integers.get(4));
	    	intent.putExtra("hp", battleInitMessage.integers.get(5));
	    	intent.putExtra("seed", seed);
	    	intent.putExtra("nick", battleInitMessage.strings.get(0));
	    	intent.putExtra("gender", battleInitMessage.integers.get(2));
    	}
    	else
    		intent.putExtra("battle","wild");
    	startActivityForResult(intent, 1);  	
    }
    
    // create alert dialog to ask for outgoing battle confirmation
    public void showBattleOutgoingDialog(String playerName)
    {
    	battleAlert.setMessage(Html.fromHtml("Do you want to ask <i>" + playerName + "</i> to battle?"));
    	battleAlert.setButton("Send request", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){G.game.requestBattle();}});
    	battleAlert.setButton2("Don't send", new DialogInterface.OnClickListener() 
			{public void onClick(DialogInterface dialog, int id){G.game.rejectBattle(false);dialog.cancel();}});
		battleAlert.show();
    }
    
    // create alert dialog to ask for incoming battle confirmation
    public void showBattleIncomingDialog(String playerName)
    {
    	battleAlert.setMessage(Html.fromHtml("Do you want to battle <i>" + playerName + "</i>?"));
		battleAlert.setButton("Battle!", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id){G.game.acceptBattle();}});
		battleAlert.setButton2("Don't battle", new DialogInterface.OnClickListener() 
		{public void onClick(DialogInterface dialog, int id){G.game.rejectBattle(true);dialog.cancel();}});
		battleAlert.show();
    }
    
    // create alert dialog to notify the user of important events
    public void showToast(String message) 
    {
    	toast.setText(message);
    	toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
    	toast.show();
    }
    
    public void showProgressDialog(String message)
    {
    	progressDialog = ProgressDialog.show(this, "", message, true, false);
    	progressDialog.show();
    }
    
    public void cancelProgressDialog() {progressDialog.cancel();}
    
    public void cancelBattleAlert() {battleAlert.dismiss();}
    
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
        
        trainer_aura = new ArrayItemizedOverlay(animAura, this);
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
    	playerIDs.add(p.getID());
    	players.addItem(p.getMarker());
		shadows_player.addItem(p.getShadow());
    }
    
    public void removePlayer(NetworkPlayer p)
    {
    	playerIDs.remove(p.getID());
    	players.removeItem(p.getMarker());
		shadows_player.removeItem(p.getShadow());
    }
    
    public void addItem(WorldPotion p)
    {
    	itemIDs.add(p.getID());
    	items.addItem(p.getMarker());
    }
    
    public void removeItem(WorldPotion p)
    {
    	itemIDs.remove(p.getID());
    	items.removeItem(p.getMarker());
    }
    
    public void addRegion(Region region)
    {
    	regions.addWay(region.getWay());
    }
    
    public void removeRegion(Region region)
    {
    	regions.removeWay(region.getWay());
    }
	
	public void updateLocation(Location location) 
	{
		end = Util.fromLocation(location);
		end_loc = location;
	}
	
	public void showNoPokemonAlert(boolean show)
	{persistent_alert.setVisibility(show?View.VISIBLE:View.INVISIBLE);}
	
	public void updateCoins() {coins.setText(G.player.coins + "");}
}
