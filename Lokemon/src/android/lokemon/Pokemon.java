package android.lokemon;

import java.util.Arrays;

import org.json.*;

import android.util.Log;

public class Pokemon {
	
	// public attributes
	public int index;
	// battle variables aren't saved and only persist for a single battle
	public boolean inBattle;
	public float accuracy;
	public float evasion;
	
	// private attributes
	private BasePokemon base;
	private int hp_current;
	private int attack;
	private int speed;
	private int defense;
	private int special;
	private int hp_total;
	private int [] IV; // individual values
	private int [] EV; // effort values
	private int level;
	private int xp;
	private int xp_to_level;
	
	// creates new Pokemon at level 1
	public Pokemon(int pokeIndex, int level)
	{
		index = pokeIndex;
		inBattle = false;
		base = G.basePokemon[index];
		hp_current = base.hp;
		xp = (int)Math.pow(level, 3);
		xp_to_level = (int)Math.pow(level+1, 3);
		IV = new int[5]; // attack = 0, defense = 1, speed = 2, special = 3, hp = 4
		for (int i = 0; i < IV.length; i++) IV[i] = (int)(Math.random() * 16);
		EV = new int[5];
		this.level = level - 1;
		levelUp();
	}
	
	// recreates existing Pokemon with specific hp, level, IVs and EVs
	public Pokemon(int pokeIndex, int hp, int xp, int level, int [] IVs, int [] EVs, int [] stats)
	{
		index = pokeIndex;
		base = G.basePokemon[index];
		this.level = level;
		this.xp = xp;
		xp_to_level = (int)Math.pow(level+1, 3);
		inBattle = false;
		hp_current = hp;
		IV = IVs;
		EV = EVs;
		attack = stats[0];
		defense = stats[1];
		speed = stats[2];
		special = stats[3];
		hp_total = stats[4];
	}
	
	// recreates existing Pokemon from JSON
	public Pokemon (JSONObject object) throws JSONException
	{
		this(object.getInt("index"), object.getInt("hp"), object.getInt("xp"),
				object.getInt("level"), G.getIntArray(object.getJSONArray("iv")),
				G.getIntArray(object.getJSONArray("ev")), G.getIntArray(object.getJSONArray("stats")));
	}
	
	private void levelUp()
	{
		if (level + 1 >= base.evolution[1]) evolve();
		else
		{
			level++;
			attack = calculateStat(base.attack, EV[0], IV[0]);
			defense = calculateStat(base.defense, EV[1], IV[1]);
			speed = calculateStat(base.speed, EV[2], IV[2]);
			special = calculateStat(base.special, EV[3], IV[3]);
			hp_total = (int)((IV[4] + base.hp + Math.sqrt(EV[4])/8.0 + 50) * level / 50.0 + 10);
			hp_current = hp_total;
			xp_to_level = (int)Math.pow(level+1, 3);
		}
	}
	
	private void evolve()
	{
		index = base.evolution[0];
		base = G.basePokemon[index];
		levelUp();
	}
	
	public void addExperience(int xp)
	{
		this.xp += xp;
		if (this.xp >= xp_to_level) levelUp();
	}
	
	public BasePokemon getBase() {return base;}
	
	private int calculateStat(int baseStat, int ev, int iv)
	{
		return (int)((iv + baseStat + Math.sqrt(ev)/8.0) * level / 50.0 + 5);
	}
	
	public int getTotalHP() {return hp_total;}
	
	public int getHP() {return hp_current;}
	
	public int getSpeed() {return speed;}
	
	public int getSpecial() {return special;}
	
	public int getDefense() {return defense;}
	
	public int getAttack() {return attack;}
	
	public int getLevel() {return level;}
	
	public String getName() {return base.name;}
	
	public String getType1() {return base.type[0];}
	
	public String getType2() {if (base.type.length==2) return base.type[1]; else return null;}
	
	public int getSpriteNormal() {return base.spriteID;}
	
	public int getSpriteAttack() {return base.spriteID_attack;}
	
	public int getExperienceYield()
	{
		float baseTotal = base.attack + base.speed + base.special * 2 + base.defense + base.hp;
		return (int)(0.64*baseTotal - 113); // formula based on linear regression
	}
	
	public String getJSON() throws JSONException
	{
		JSONObject obj = new JSONObject();
		JSONArray stats = new JSONArray();
		stats.put(attack);
		stats.put(defense);
		stats.put(speed);
		stats.put(special);
		stats.put(hp_total);
		obj.accumulate("index", index);
		obj.accumulate("xp", xp);
		obj.accumulate("hp", hp_current);
		obj.accumulate("level", level);
		obj.accumulate("stats", stats);
		obj.accumulate("ev", G.getIntJSONArray(EV));
		obj.accumulate("iv", G.getIntJSONArray(IV));
		return obj.toString();
	}
}
