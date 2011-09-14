package android.lokemon;

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
	public static BasePokemon [] basePokemon;
	public static Move [] moves;
	public static Trainer player;
	public static Game game;
	public static Mode mode;
	public static ElemType [] types;
	public static Battle battle;
	
	// icons used by map
	public static Drawable player_marker_busy;
	public static Drawable player_marker_available;
	
	// enums
	public static enum Types {bug, electric, fire, flying, grass, ground, normal, poison, psychic, rock, steel, water};
	public static enum Mode {BATTLE, MAP};
	public static enum Potions {HP, ATTACK, DEFENSE, SPECIAL, SPEED}
	public static enum Gender {FEMALE, MALE};
	public static enum MapObjectState{NEW, ONLINE, OFFLINE};
	public static enum PlayerState{BUSY, AVAILABLE};
}
