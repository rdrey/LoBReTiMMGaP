package android.lokemon.game_objects;

public abstract class BagItem {
	private int max_count;
	private String name;
	private int count;
	protected int spriteID;
	private String description;
	
	public BagItem (String name, int count, int max_count, String desc)
	{
		this.name = name;
		this.count = count;
		this.max_count = max_count;
		this.description = desc;
	}
	
	public String getName () {return name;}
	public int getCount() {return count;}	
	public int getSprite() {return spriteID;}
	public int getMax() {return max_count;}
	public String getDescription () {return description;}
	
	public void decrement() {if (count > 0) count--;}
	
	public void increment() throws MaxItemCountException 
	{
		if (count < max_count) 
			count++;
		else throw new MaxItemCountException();
	}
	
	public class MaxItemCountException extends Exception
	{
		private static final long serialVersionUID = -2335643164296341875L;
		public MaxItemCountException () {super("You cannot have more of that item.");}
	}
}
