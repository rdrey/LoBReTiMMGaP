package android.lokemon;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.lokemon.game_objects.BasePokemon;
import android.lokemon.game_objects.ElemType;
import android.lokemon.game_objects.Move;
import android.lokemon.game_objects.Trainer;

/*
 * This class contains all variables & enums that need to be accessible throughout the game.
 */
public class G {
	// game variables
	public static BasePokemon [] base_pokemon;
	public static Move [] moves;
	public static Trainer player;
	public static Game game;
	public static Mode mode;
	public static TestMode testMode = TestMode.EXPERIMENT;
	public static ElemType [] types;
	public static Battle battle;
	public static int [] region_colours;
	
	// icons used by map
	public static Drawable player_marker_busy;
	public static Drawable player_marker_available;
	// paint objects used to draw regions
	public static Paint [] region_outline;
	public static Paint [] region_fill;
	
	// enums
	public static enum Types {bug, electric, fire, flying, grass, ground, normal, poison, psychic, rock, steel, water};
	public static enum Mode {BATTLE, MAP};
	public static enum TestMode {CONTROL, EXPERIMENT};
	public static enum Potions {HP, ATTACK, DEFENSE, SPECIAL, SPEED}
	public static enum Gender {FEMALE, MALE};
	public static enum PlayerState{BUSY, AVAILABLE};
	public static enum Regions {CAVE, FOREST, GRASSLAND, MOUNTAIN, ROUGH_TERRAIN, URBAN, WATER_EDGE, POKEMON_CENTER, POKEMART};
}
