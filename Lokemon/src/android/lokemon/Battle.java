package android.lokemon;

import android.lokemon.G.Mode;
import android.lokemon.game_objects.BagItem;
import android.lokemon.game_objects.Pokemon;
import android.lokemon.screens.BattleScreen;
import android.util.Log;

public class Battle {

	// current pokemon selected by player
	private Pokemon poke_player;
	// current pokemon selected by opponent
	private Pokemon poke_opp;
	// how many pokemon are able to battle
	int pokeCount;
	// how many usable items are there
	int itemCount;
	
	// a reference to the screen that displays the battle
	private BattleScreen display;

	public Battle(BattleScreen screen)
	{
		display = screen;
		pokeCount = 0;
		itemCount = 0;
		G.mode = Mode.BATTLE;
		
		switchPlayerPoke(G.player.pokemon.get(0));
		switchOppPoke(G.player.pokemon.get(2));
		
		for (int i = 0; i < G.player.pokemon.size(); i++)
			if (G.player.pokemon.get(i).getHP() > 0)
				pokeCount++;
		display.setNumPokemon(pokeCount);
		if (pokeCount < 2)
			display.disableSwitch();
		
		for (BagItem i:G.player.items)
			itemCount += i.getCount();
		if (itemCount == 0)
			display.disableBag();
		
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
	
	public void useItem(BagItem item)
	{
		item.decrement();
		itemCount--;
		if (itemCount == 0)
			display.disableBag();
		// battle logic here
	}
	
	// decide whether player can run away from battle (forfeits turn)
	public boolean run()
	{
		return true;
	}
	
	public Pokemon getSelectedPokemon() {return poke_player;}
}
