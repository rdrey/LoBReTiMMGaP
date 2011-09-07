package android.lokemon;

/*
 * This class contains all variables that need to be accessible throughout the game.
 */
public class G {
	public static BasePokemon [] basePokemon;
	public static Move [] moves;
	public static Trainer player;
	public static Game game;
	public static enum Mode {BATTLE, MAP};
	public static Mode mode;
	public static enum Types {bug, electric, fire, flying, grass, ground, normal, poison, psychic, rock, steel, water};
	public static ElemType [] types;
	public static Battle battle;
}
