package android.lokemon;

import android.lokemon.G.Mode;
import android.util.Log;

public class Battle {

	// current pokemon selected by player
	private Pokemon poke_player;
	// current pokemon selected by opponent
	private Pokemon poke_opp;
	
	// a reference to the screen that displays the battle
	private BattleScreen display;

	public Battle(BattleScreen screen)
	{
		display = screen;
		G.mode = Mode.BATTLE;
		switchPlayerPoke(G.player.pokemon.get(0));
		switchOppPoke(G.player.pokemon.get(2));
		G.battle = this;
	}
	
	public void switchPlayerPoke(Pokemon newPoke)
	{
		if (poke_player != null) poke_player.inBattle = false;
		newPoke.inBattle = true;
		poke_player = newPoke;
		display.switchPlayerPoke(poke_player);
	}
	
	public void switchOppPoke(Pokemon newPoke)
	{
		poke_opp = newPoke;
		display.switchOppPoke(poke_opp);
	}
}
