package android.lokemon;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.lokemon.game_objects.BasePokemon;
import android.lokemon.game_objects.ElemType;
import android.lokemon.game_objects.Move;
import android.lokemon.game_objects.Trainer;
import android.os.Environment;

/*
 * This class contains all variables & enums that need to be accessible throughout the game.
 */
public class G {
	// game variables
	public static BasePokemon [] base_pokemon;
	public static ArrayList<BasePokemon> [] pokemon_by_region = new ArrayList[7];
	public static Move [] moves;
	public static Trainer player;
	public static Game game;
	public static Mode mode;
	public static TestMode testMode = TestMode.CONTROL;
	public static ElemType [] types;
	public static Battle battle;
	public static int [] region_colours;
	public static Random random = new Random(System.currentTimeMillis());
	
	// icons used by map
	public static Drawable player_marker_busy;
	public static Drawable player_marker_available;
	// paint objects used to draw regions
	public static Paint [] region_outline;
	public static Paint [] region_fill;
	// path to map data
	public static String mapDir = Environment.getExternalStorageDirectory().getPath() + 
			"/Android/data/android.lokemon/files/";
	public static String mapFile = "campus.map";
	
	// enums
	public static enum BattleType {TRAINER, WILD};
	public static enum Types {bug, electric, fire, flying, grass, ground, normal, poison, psychic, rock, steel, water};
	public static enum Mode {BATTLE, MAP};
	public static enum TestMode {CONTROL, EXPERIMENT};
	public static enum Potions {HP, ATTACK, DEFENSE, SPECIAL, SPEED}
	public static enum Gender {FEMALE, MALE};
	public static enum PlayerState{BUSY, AVAILABLE};
	public static enum BattleMove {RUN, USE_ITEM, ATTACK, SWITCH_POKEMON, CATCH_POKEMON, GAME_OVER, DISCONNECTED};
	public static enum Regions {CAVE, FOREST, GRASSLAND, MOUNTAIN, ROUGH_TERRAIN, URBAN, WATER_EDGE, POKEMON_CENTER, POKEMART, NONE};
	public static enum Action {REQUEST_BATTLE, ACCEPT_BATTLE, REJECT_BATTLE, REJECT_ALL, BATTLE_MOVE};
}
