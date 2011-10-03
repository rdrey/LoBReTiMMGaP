package android.lokemon;

import java.util.ArrayList;
import java.util.Random;

import android.lokemon.G.BattleMove;
import android.lokemon.G.BattleType;
import android.lokemon.G.Gender;
import android.lokemon.G.Mode;
import android.lokemon.game_objects.BagItem;
import android.lokemon.game_objects.Move;
import android.lokemon.game_objects.Pokemon;
import android.lokemon.screens.BattleScreen;
import android.util.Log;

public class Battle {
	
	private static float [] stage_mod = {0.25f,0.29f,0.33f,0.40f,0.50f,0.67f,1.0f,1.5f,2.0f,2.5f,3.0f,3.5f,4.0f};
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
	public int pokeCount;
	// how many usable items are there
	private int itemCount;
	// indicates which Pokemon were used in battle (and did not feint)
	private boolean [] battled;
	private boolean [] defeated;
	private ArrayList<Integer> opp_defeated; // tuples (index, level)
	
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
		Log.i("Battle", seed + "");
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
		{
			int index = 0;
			int max = 0;
			for (int i = 0; i < moves.size(); i+=2)
			{
				if (G.moves[moves.get(i)[0]].power > max)
				{
					index = i;
					max = G.moves[moves.get(i)[0]].power;
				}
			}
			opponent_move_index = moves.get(index)[0];
		}
		else opponent_move_index = -1;
	}
	
	private void executeTurn()
	{
		float speed_player = poke_player.getSpeed() * stage_mod[player_stages[4]+6];
		float speed_opp = poke_opp.getSpeed() * stage_mod[opp_stages[4]+6];
		if (speed_player > speed_opp)
			meFirst();
		else if (speed_player < speed_opp)
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
			opp_defeated.add(poke_opp.getLevel());
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
				resultMessage = "You defeated the wild " + poke_opp.getName() + "!\n" + calculateExperience();
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
					if (battleType == BattleType.TRAINER)
						G.game.sendSimpleBattleMessage(BattleMove.GAME_OVER, -1);
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
				if (battleType == BattleType.TRAINER)
					G.game.sendSimpleBattleMessage(BattleMove.GAME_OVER, -1);
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
				opp_defeated.add(poke_opp.getLevel());
				if (battleType == BattleType.TRAINER)
				{
					waitingForNewPoke = true;
					display.showProgressDialog("Waiting for opponent...");
				}
				else
				{
					resultMessage = "You defeated the wild " + poke_opp.getName() + "!\n" + calculateExperience();
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
					if (player_stages[index - 1] < 6) player_stages[index - 1]++;
			}
			else
			{
				if (index == 1)
				{
					poke_opp.setHP(poke_opp.getTotalHP());
					display.setOppPokeDetails(poke_opp);
				}
				else
					if (opp_stages[index - 1] < 6) opp_stages[index - 1]++;
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
		if (moveIndex != -1)
		{
			MoveResult result;
			Move move = G.moves[moveIndex];
			if (playerName == null)
				resultMessage += "The wild " + source.getName() + " used " + move.name;
			else
				resultMessage += (source==poke_player?"Your ":(playerName + "'s ")) + source.getName() + " used " + move.name;
			
			if (random.nextDouble() < move.accuracy/100.0)
			{
				if (source == poke_player)
					poke_player.decreasePP(moveIndex);
				
				// check if this is a damage dealing move
				if (move.power > 0)
				{
					// determine stats with stage taken into account
					float attack;
					float defense;
					float specialSource;
					float specialTarget;
					if (source == poke_player)
					{
						attack = source.getAttack() * stage_mod[player_stages[1] + 6];
						defense = target.getAttack() * stage_mod[opp_stages[2] + 6];
						specialSource = source.getSpecial() * stage_mod[player_stages[3] + 6];
						specialTarget = target.getSpecial() * stage_mod[opp_stages[3] + 6];
					}
					else
					{
						attack = source.getAttack() * stage_mod[opp_stages[1] + 6];
						defense = target.getAttack() * stage_mod[player_stages[2] + 6];
						specialSource = source.getSpecial() * stage_mod[opp_stages[3] + 6];
						specialTarget = target.getSpecial() * stage_mod[player_stages[3] + 6];
					}
					
					// determine if critical hit was landed
					int critical_mod = 1;
					double val = random.nextDouble();
					if (move.critical)
						if (val<(source.getSpeed()/64.0f))
							critical_mod = 2;
					else if (val<(source.getSpeed()/512.0f))
						critical_mod = 2;
					
					// determine if same-type attack bonus is applicable
					float stab_mod = 1;
					if (source.getType1() == move.type || (source.getType2()!=null && source.getType2() == move.type))
						stab_mod = 1.5f;
					
					// calculate type effectiveness
					float type_mod = G.type_modifiers[move.type.ordinal][target.getType1().ordinal];
					if (target.getType2() != null)
						type_mod *= G.type_modifiers[move.type.ordinal][target.getType2().ordinal];
					
					float modifier = critical_mod * stab_mod * type_mod * (float)(random.nextDouble() * 0.15 + 0.85);
					
					// THE DAMAGE FORMULA - awesome shit!
					int damage;
					if (move.special)
						damage = (int)(((2 * source.getLevel() + 10)/250.0 * specialSource/specialTarget * move.power + 2) * modifier);
					else
						damage = (int)(((2 * source.getLevel() + 10)/250.0 * attack/defense * move.power + 2) * modifier);
					
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
					
					 // build result string
					 resultMessage += " and inflicted " + damage + " damage";
					 String type_bonus = "";
					 if (type_mod == 0.5)
						 type_bonus = "MOVE INEFFECTIVE";
					 else if (type_mod == 2.0)
						 type_bonus = "SUPER EFFECTIVE MOVE";
					 else if (type_mod == 0.0)
						 type_bonus = "TARGET IMMUNE TO MOVE";
					 String bonuses = type_bonus.length()==0?"":" ("+type_bonus;
					 if (critical_mod > 1)
						 bonuses = bonuses.length()==0?" (CRITICAL HIT":bonuses+", CRITICAL HIT";
					 if (stab_mod > 1)
						 bonuses = bonuses.length()==0?" (STAB BONUS":bonuses+", STAB BONUS";
					 if (bonuses.length() == 0)
						 resultMessage += ".\n";
					 else
						 resultMessage += bonuses + ").\n";			 
				}
				else
				{
					resultMessage += ".\n";
					result = MoveResult.NONE;
				}
				boolean stage_effect = false;
				for (int i = 1; i < move.stages.length; i++)
				{
					if (move.stages[i] != 0)
					{
						stage_effect = true;
						if (move.stages[i] < 0)
						{
							if (source == poke_player)
								if (opp_stages[i] > -6) opp_stages[i]--;
							else
								if (player_stages[i] > -6) player_stages[i]--;
						}
						else
						{
							if (source == poke_player)
								if (player_stages[i] < 6) player_stages[i]++;
							else
								if (opp_stages[i] < 6) opp_stages[i]++;
						}
					}
				}
				return result;
			}
			else
			{
				resultMessage += " and missed.\n";
				return MoveResult.NONE;
			}
		}
		else
		{
			resultMessage += "No moves exist...\n";
			return MoveResult.NONE;
		}
	}
	
	public Pokemon getSelectedPokemon() {return poke_player;}
	
	public String calculateExperience()
	{
		int totalXP = 0;
		for (int i = 0; i < opp_defeated.size(); i+=2)
			totalXP += G.base_pokemon[opp_defeated.get(i)].getExperienceYield() * opp_defeated.get(i+1);
		ArrayList<Pokemon> contributors = new ArrayList<Pokemon>();
		for (int i = 0; i < battled.length; i++)
			if (battled[i] && !defeated[i])
				contributors.add(G.player.pokemon.get(i));
		int ind_share = totalXP/contributors.size();
		String result = "";
		for (Pokemon poke:contributors)
		{
			int lv = poke.getLevel();
			String name = poke.getName();
			int r = poke.addExperience(ind_share);
			if (r == 1)
			{
				int dif = poke.getLevel() - lv;
				result += name + " leveled up " + dif + (dif==1?" level.\n":" levels.\n");
			}
			else if (r == 2)
				result += name + " evolved into " + poke.getName() + "!\n";
		}
		
		// requires better calculation, but for now this will do
		int coin = totalXP / 100;
		if (coin == 0) coin = 1;
		G.player.coins += coin;
		result += "You earned " + coin + (coin==1?" coin.\n":" coins.\n");
		
		return result;
	}
	
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
	public void handleOpponentDefeated()
	{
		display.showToast("You defeated " + display.getOppNick() + "!\n" + calculateExperience());
		display.endBattle();
	}
	
	// this method is called when the player loses connection to game server
	public void handlePlayerDisconnected()
	{
		display.showToast("You have been disconnected");
		display.endBattle();
	}
	
	// this method is called when an opponent is disconnected during battle
	public synchronized void handleOpponentDisconnected()
	{
		// you get experience points if the opponent disconnects
		display.showToast(display.getOppNick() + " has been disconnected.\n" + calculateExperience());
		display.endBattle();
	}
}
