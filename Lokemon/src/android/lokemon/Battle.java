package android.lokemon;

import android.content.Intent;
import android.lokemon.G.Mode;
import android.util.Log;

public class Battle {

	// current pokemon selected by player
	private Pokemon poke_player;
	// current pokemon selected by opponent
	private Pokemon poke_opp;
	// how many pokemon are able to battle
	int pokeCount;
	
	// a reference to the screen that displays the battle
	private BattleScreen display;

	public Battle(BattleScreen screen)
	{
		display = screen;
		pokeCount = 0;
		G.mode = Mode.BATTLE;
		switchPlayerPoke(G.player.pokemon.get(0));
		switchOppPoke(G.player.pokemon.get(2));
		for (int i = 0; i < G.player.pokemon.size(); i++)
			if (G.player.pokemon.get(i).getHP() > 0)
				pokeCount++;
		display.setNumPokemon(pokeCount);
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
	
	public void selectMove(int moveIndex)
	{
		// battle logic here
	}
	
	public Pokemon getSelectedPokemon() {return poke_player;}
}
