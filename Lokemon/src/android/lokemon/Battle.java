package android.lokemon;

import java.util.ArrayList;
import java.util.Random;

import android.lokemon.G.BattleMove;
import android.lokemon.G.BattleType;
import android.lokemon.G.Gender;
import android.lokemon.G.Mode;
import android.lokemon.game_objects.BagItem;
import android.lokemon.game_objects.Pokemon;
import android.lokemon.screens.BattleScreen;
import android.util.Log;

public class Battle {
	
	private enum MoveResult {RAN_AWAY, VICTORY, NONE, CAUGHT_POKEMON};
	public BattleType battleType;
	
	// battle variables
	private Random random;
	private boolean waitingForOpponent;
	private boolean waitingForPlayer;
	private boolean waitingForNewPoke;
	private String resultMessage;
	
	// move variables
	private BattleMove player_move;
	private int player_move_index;
	private BattleMove opponent_move;
	private int opponent_move_index;
	private Pokemon player_next_poke;
	private Pokemon opponent_next_poke;
	
	// current pokemon selected by player
	private Pokemon poke_player;
	private int poke_player_index;
	private int[] player_stages;
	// current pokemon selected by opponent
	private Pokemon poke_opp;
	private int[] opp_stages;
	// how many pokemon are able to battle
	private int pokeCount;
	// how many usable items are there
	private int itemCount;
	// indicates which Pokemon were used in battle (and did not feint)
	private boolean [] battled;
	private boolean [] defeated;
	private ArrayList<Integer> opp_defeated;
	
	// a reference to the screen that displays the battle
	private BattleScreen display;

	private Battle(BattleScreen screen, int player_start_index, Pokemon opponent_start)
	{
		G.mode = Mode.BATTLE;
		
		display = screen;
		pokeCount = 0;
		itemCount = 0;
		
		for (int i = 0; i < G.player.pokemon.size(); i++)
		{
			if (G.player.pokemon.get(i).getHP() > 0)
				pokeCount++;
			G.player.pokemon.get(i).inBattle = false;
		}
		display.setNumPokemon(pokeCount);
		if (pokeCount < 2)
			display.disableSwitch();
		
		for (BagItem i:G.player.items)
			itemCount += i.getCount();
		if (itemCount == 0)
			display.disableBag();
				
		battled = new boolean[G.player.pokemon.size()];
		defeated = new boolean[G.player.pokemon.size()];
		opp_defeated = new ArrayList<Integer>();
		
		// this selection assumes the player has at least one battle-ready pokemon
		int i = 0;
		while (G.player.pokemon.get(i).getHP() <= 0)
			i++;
		
		poke_player_index = i;
		poke_player = G.player.pokemon.get(i);
		battled[i] = true;
		poke_player.inBattle = true;
		display.switchPlayerPoke(poke_player);
		poke_opp = opponent_start;
		display.switchOppPoke(poke_opp);
		
		player_stages = new int[5];
		opp_stages = new int[5];
		
		resetTurn();
		
		G.battle = this;
	}
	
	public Battle(BattleScreen screen)
	{
		this(screen, 0, G.game.genPokemon);
		battleType = BattleType.WILD;
		random = new Random();
	}
	
	public Battle(BattleScreen screen, Pokemon opponent_start, int seed)
	{
		this(screen, 0, opponent_start);
		battleType = BattleType.TRAINER;
		itemCount -= G.player.items[0].getCount();
		random = new Random(seed);
	}
	
	public void switchPlayerPoke(int index)
	{
		player_next_poke = G.player.pokemon.get(index);
		player_move = BattleMove.SWITCH_POKEMON;
		finalizePlayerTurn();
	}
	
	public void selectMove(int moveIndex)
	{
		player_move = BattleMove.ATTACK;
		player_move_index = moveIndex;
		finalizePlayerTurn();
	}
	
	public void useItem(int itemIndex)
	{
		if (itemIndex == 0)
			player_move = BattleMove.CATCH_POKEMON;
		else
			player_move = BattleMove.USE_ITEM;
		player_move_index = itemIndex;
		finalizePlayerTurn();
	}
	
	public void run()
	{
		player_move = BattleMove.RUN;
		finalizePlayerTurn();
	}
	
	private synchronized void finalizePlayerTurn()
	{
		Log.i("Battle", waitingForOpponent + " " + player_move.toString());
		
		if (player_move == BattleMove.SWITCH_POKEMON)
			G.game.sendSwitchBattleMessage(player_next_poke);
		else
			G.game.sendSimpleBattleMessage(player_move, player_move_index);
		
		waitingForPlayer = false;
		if (battleType == BattleType.TRAINER)
		{
			if (!waitingForOpponent)
				executeTurn();
			else
				display.showProgressDialog("Waiting for opponent...");
		}	
		else
		{
			performAI();
			executeTurn();
		}
	}
	
	private void finalizedOppTurn()
	{
		Log.i("Battle", waitingForPlayer + " " + opponent_move.toString());
		waitingForOpponent = false;
		if (!waitingForPlayer)
		{
			display.cancelProgressDialog();
			executeTurn();
		}
	}
	
	private void resetTurn()
	{
		waitingForPlayer = true;
		waitingForOpponent = true;
		resultMessage = "";
	}
	
	private void performAI()
	{
		ArrayList<int[]> moves = poke_opp.getMovesAndPP();
		opponent_move = BattleMove.ATTACK;
		if (moves.size() > 0)
			opponent_move_index = moves.get(random.nextInt(moves.size()))[0];
		else opponent_move_index = -1;
	}
	
	private void executeTurn()
	{
		if (poke_player.getSpeed() > poke_opp.getSpeed())
			meFirst();
		else if (poke_player.getSpeed() < poke_opp.getSpeed())
			youFirst();
		else if (G.player.id < G.game.opponentID)
			meFirst();
		else
			youFirst();
		
		display.showToast(resultMessage);
		resetTurn();
	}
	
	private void meFirst()
	{
		MoveResult result = executeMove(player_move, player_move_index, poke_player, poke_opp,player_next_poke);
		boolean finished = false;
		switch(result)
		{
		case NONE:
			result = executeMove(opponent_move, opponent_move_index, poke_opp, poke_player,opponent_next_poke);
			break;
		case VICTORY:
			opp_defeated.add(poke_opp.index);
			if (battleType == BattleType.TRAINER)
			{
				if (opponent_move == BattleMove.SWITCH_POKEMON)
					executeMove(opponent_move, opponent_move_index, poke_opp, poke_player,opponent_next_poke);
				else
				{
					waitingForNewPoke = true;
					display.showProgressDialog("Waiting for opponent...");
				}
			}
			else
			{
				resultMessage = "You defeated the wild " + poke_opp.getName() + "!";
				// get experience from defeating wild pokemon
				display.endBattle();
			}
			finished = true;
			break;
		case CAUGHT_POKEMON:
			G.player.pokemon.add(poke_opp);
			display.endBattle();
			finished = true;
			break;
		case RAN_AWAY:
			display.endBattle();
			finished = true;
			break;
		}
		if (!finished)
		{
			switch(result)
			{
			case VICTORY:
				pokeCount--;
				display.setNumPokemon(pokeCount);
				defeated[poke_player_index] = true;
				
				if (pokeCount == 0)
				{
					resultMessage = "You were defeated by " + (battleType == BattleType.TRAINER?display.getOppNick():"the wild " + poke_opp.getName() + "...");
					// send defeated message
					// no experience gained
					display.endBattle();
				}
				else
				{
					if (pokeCount == 1)
						display.disableSwitch();
					
					Pokemon new_poke = null;
					int index = 0;
					for (Pokemon p:G.player.pokemon)
					{
						if (p.getHP() > 0)
						{
							new_poke = p;
							break;
						}
						index++;
					}
					
					battled[index] = true;
					poke_player_index = index;
					
					if (battleType == BattleType.TRAINER)
						G.game.sendSwitchBattleMessage(new_poke);
							
					for (int i = 0; i < 5; i++)
						player_stages[i] = 0;
					poke_player.inBattle = false;
					resultMessage += "Your " + poke_player.getName() + " has feinted.\n";
					poke_player = new_poke;
					poke_player.inBattle = true;
					display.switchPlayerPoke(poke_player);
				}
				break;
			case RAN_AWAY:
				// send ran away message
				// no experience gained (but opponent does)
				display.endBattle();
				break;
			}
		}
	}
	
	private void youFirst()
	{
		MoveResult result = executeMove(opponent_move, opponent_move_index, poke_opp, poke_player,opponent_next_poke);
		boolean finished = false;
		switch(result)
		{
		case NONE:
			result = executeMove(player_move, player_move_index, poke_player, poke_opp,player_next_poke);
			break;
		case VICTORY:
			pokeCount--;
			display.setNumPokemon(pokeCount);
			defeated[poke_player_index] = true;
			
			if (pokeCount == 0)
			{
				resultMessage = "You were defeated by " + (battleType == BattleType.TRAINER?display.getOppNick():"the wild " + poke_opp.getName() + "...");
				// send defeated message
				// no experience gained
				display.endBattle();
			}
			else
			{
				resultMessage += "Your " + poke_player.getName() + " has feinted.\n";
				
				if (pokeCount == 1)
					display.disableSwitch();
				
				if (player_move == BattleMove.SWITCH_POKEMON)
					executeMove(player_move, player_move_index, poke_player, poke_opp,player_next_poke);
				else
				{
					Pokemon new_poke = null;
					int index = 0;
					for (Pokemon p:G.player.pokemon)
					{
						if (p.getHP() > 0)
						{
							new_poke = p;
							break;
						}
						index++;
					}
					
					battled[index] = true;
					poke_player_index = index;
					
					if (battleType == BattleType.TRAINER)
						G.game.sendSwitchBattleMessage(new_poke);
							
					for (int i = 0; i < 5; i++)
						player_stages[i] = 0;
					poke_player.inBattle = false;
					poke_player = new_poke;
					poke_player.inBattle = true;
					display.switchPlayerPoke(poke_player);
				}
			}
			finished = true;
			break;
		case RAN_AWAY:
			// gain experience from opponent running away
			finished = true;
			display.endBattle();
			break;
		}
		if (!finished)
		{
			switch(result)
			{
			case VICTORY:
				opp_defeated.add(poke_opp.index);
				if (battleType == BattleType.TRAINER)
				{
					waitingForNewPoke = true;
					display.showProgressDialog("Waiting for opponent...");
				}
				else
				{
					resultMessage = "You defeated the wild " + poke_opp.getName() + "!";
					// get experience from defeating wild pokemon
					display.endBattle();
				}
				break;
			case CAUGHT_POKEMON:
				G.player.pokemon.add(poke_opp);
				display.endBattle();
				break;
			case RAN_AWAY:
				display.endBattle();
				break;
			}
		}
	}
	
	private MoveResult executeMove(BattleMove move, int index, Pokemon source, Pokemon target, Pokemon new_poke)
	{
		//RUN, USE_ITEM, ATTACK, SWITCH_POKEMON, CATCH_POKEMON
		String player = source==poke_player?"You":display.getOppNick();
		String pronoun = source==poke_player?"your":(display.getOppGender()==Gender.FEMALE?"her":"his");
		switch(move)
		{
		case RUN:
			if (battleType == BattleType.TRAINER)
			{
				if (source.getSpeed() > target.getSpeed())
				{
					resultMessage += player + " managed to run away!\n";
					return MoveResult.RAN_AWAY;
				}
				else
				{
					resultMessage += player + " failed to run away.\n";
					return MoveResult.NONE;
				}
			}
			else
			{
				resultMessage += player + " managed to run away!\n";
				return MoveResult.RAN_AWAY;
			}
		case USE_ITEM:
			if (source == poke_player)
			{
				BagItem item = G.player.items[index];
				item.decrement();
				itemCount--;
				if (itemCount == 0)
					display.disableBag();
				if (index == 1)
				{
					poke_player.setHP(poke_player.getTotalHP());
					display.setPlayerPokeDetails(poke_player);
				}
				else
					player_stages[index - 1]++;
			}
			else
			{
				if (index == 1)
				{
					poke_opp.setHP(poke_opp.getTotalHP());
					display.setOppPokeDetails(poke_opp);
				}
				else
					opp_stages[index - 1]++;
			}		
			resultMessage += player + " used a " + G.player.items[index].getName() + ".\n";
			return MoveResult.NONE;
		case ATTACK:
			return executeAttack(index, source, target,player);
		case SWITCH_POKEMON:
			if (source == poke_player)
			{
				for (int i = 0; i < 5; i++)
					player_stages[i] = 0;
				poke_player.inBattle = false;
				poke_player = new_poke;
				poke_player.inBattle = true;
				display.switchPlayerPoke(poke_player);
				resultMessage += player + " swapped in " + pronoun + " " + poke_player.getName() + ".\n";
				
				// pokemon has now contributed to battle (unless it feints before the end)
				poke_player_index = G.player.pokemon.indexOf(poke_player);
				battled[poke_player_index] = true;
			}
			else
			{
				for (int i = 0; i < 5; i++)
					opp_stages[i] = 0;
				poke_opp = new_poke;
				display.switchOppPoke(poke_opp);
				resultMessage += player + " swapped in " + pronoun + " " + poke_opp.getName() + ".\n";
			}
			return MoveResult.NONE;
		case CATCH_POKEMON:
			BagItem item = G.player.items[0];
			item.decrement();
			itemCount--;
			if (itemCount == 0)
				display.disableBag();
			if (target.getHP()/(float)target.getTotalHP() < 0.3)
			{
				resultMessage += "You caught the " + poke_opp.getName() + "!\n";
				return MoveResult.CAUGHT_POKEMON;
			}
			else
			{
				resultMessage += "You failed to catch the " + poke_opp.getName() + ".\n";
				return MoveResult.NONE;
			}
		default:
			return MoveResult.NONE;
		}
	}
	
	private MoveResult executeAttack(int moveIndex, Pokemon source, Pokemon target, String playerName)
	{
		MoveResult result;
		int damage = (int)(source.getAttack()/(float)target.getDefense() * 2);
		if (playerName == null)
			resultMessage += "The wild " + source.getName() + " inflicted " + damage + " damage.\n";
		else
			resultMessage += playerName + "'s " + source.getName() + " inflicted " + damage + " damage.\n";
		target.setHP(target.getHP()-damage);
		if (target.getHP() <= 0)
		{
			target.setHP(0);
			result = MoveResult.VICTORY;
		}
		else
			result = MoveResult.NONE;
		if (target == poke_opp)
			display.setOppPokeDetails(poke_opp);
		else
			display.setPlayerPokeDetails(poke_player);
		return result;
	}
	
	public Pokemon getSelectedPokemon() {return poke_player;}
	
	/*
	 * Network handling methods
	 */
	
	// this method is called when a simple BATTLE_MOVE message is received
	public synchronized void handleSimpleBattleMove(BattleMove move, int index)
	{
		opponent_move = move;
		opponent_move_index = index;
		finalizedOppTurn();
	}
	
	// this method is called when a SWITCH_POKEMON battle move message is received
	public synchronized void handleSwitchBattleMove(Pokemon new_poke)
	{
		if (waitingForNewPoke)
		{
			for (int i = 0; i < 5; i++)
				opp_stages[i] = 0;
			display.showToast(display.getOppNick() + "'s " + poke_opp.getName() + " has feinted.\n");
			poke_opp = new_poke;
			display.switchOppPoke(poke_opp);
			display.cancelProgressDialog();
			waitingForNewPoke = false;
		}
		else
		{
			opponent_move = BattleMove.SWITCH_POKEMON;
			opponent_next_poke = new_poke;
			finalizedOppTurn();
		}
	}
	
	// this method is called when the opponent has been defeated
	public void handleOpponentDefeated(int [] defeatedPokes)
	{
		
	}
	
	public void handlePlayerDisconnected()
	{
		
	}
	
	// this method is called when an opponent is disconnected during battle
	public void handleOpponentDisconnected()
	{
		if (waitingForOpponent || waitingForNewPoke)
			display.cancelProgressDialog();
		display.showToast(display.getOppNick() + " has been disconnected");
		display.endBattle();
	}
}
