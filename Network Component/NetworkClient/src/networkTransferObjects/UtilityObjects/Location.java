/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkTransferObjects.UtilityObjects;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class Location{

    private double x, y;
    public Location(double x, double y)
    {
        setLocation(x, y);
    }
    
    public Location()
    {
    	x = 0;
    	y = 0;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getDistanceFrom(Location p)
    {
        return Math.sqrt(Math.pow((x - p.getX()), 2) + Math.pow((y - p.getY()), 2));
    }

}
