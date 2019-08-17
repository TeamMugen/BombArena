package mc.euro.demolition.debug;

import java.util.Set;
import mc.alk.arena.objects.ArenaPlayer;
import org.bukkit.entity.Player;

/**
 * This class is the interface between DebugOn() and DebugOff().
 * 
 * It is used to print information about the state of objects and fields.
 * To see if they're within expected ranges.
 * 
 * As you can see, Debug can be toggled on or off at runtime.
 * 
 * Examples:
 * <pre>
 * DebugInteraface debug = new DebugOn();
 * debug = new DebugOff();
 * debug = new DebugOn();
 * 
 * debug.log("The value of x = " + x);
 * debug.messagePlayer("Debug msg here.");
 * deubg.messagePlayers(getMatch().getPlayers(), "Sends msg to each player");
 * </pre>
 * 
 * @author Nikolai
 * 
 */
public interface DebugInterface {
    
    public void log(String msg);
    public void sendMessage(Player p, String msg);
    public void msgArenaPlayers(Set<ArenaPlayer> players, String msg);
    
}
