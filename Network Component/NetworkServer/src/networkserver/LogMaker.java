/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package networkserver;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Experiment 2
 */
public class LogMaker {
    
    public static void println(String message, int playerId)
    {
        Date current = new Date(System.currentTimeMillis());        
        SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM HH:mm:ss");
        System.out.println(formatter.format(current)+": ID "+playerId+": "+message);
    }
    
    public static void errorPrintln(String message, int playerId)
    {
        println("ERROR: "+message, playerId);
    }
    
}
