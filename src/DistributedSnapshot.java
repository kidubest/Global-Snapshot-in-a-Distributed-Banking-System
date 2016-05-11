import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DistributedSnapshot class Associated with each BankNodes. 
 * 1) The BankNode initiating the snapshot, saves its local state and Sends a snapshot request message 
 *     bearing a snapshot Token to all other processes before sending any other message.
 * 2) The BankNode upon receiving the Token for the first time Saves its local State and propagates the 
 *    snapshot Token to all outgoing channels, to help propagate the token.
 * 3) Should a process that has already received the snapshot token receive a message that does not bear 
 *    the snapshot token, this process will record the state of the channel as the sequence of messages received along that channel
 */
public final class DistributedSnapshot implements Serializable {

   
	private static int snapshotID;
	
	private static int MoneyInTransfer;

    private static int localBalance;
    
   
    private final Set<Integer> unrecorded = new HashSet<>();
    
    
    public static int getSnapshotID() {
    	return snapshotID;
    }
    
    public static int getLocalBalance() {
    	return localBalance;
    }
    
    public static int getMoneyInTransfer() {
    	return MoneyInTransfer;
    }
    
    public void startDistributedSnapshot(int nodeID, int currentBalance, Map<Integer, String> bankNodes) {
    	snapshotID++;
        localBalance = currentBalance;
        MoneyInTransfer = 0;
        unrecorded.addAll(bankNodes.entrySet().parallelStream().filter(n -> n.getKey() != nodeID).map(Map.Entry::getKey).collect(Collectors.toSet()));
    }

    public void stopDistributedSnapshot() {
        localBalance = 0;
        MoneyInTransfer = 0;
        unrecorded.clear();
    }

   
    public void incrementMoneyInTransfer(int receiverNodeID, int amount) {
        if (unrecorded.contains(receiverNodeID)) {
        	MoneyInTransfer += amount;
        }
    }

    public void stopRecording(int nodeID) {
    	unrecorded.remove(nodeID);
    }

    public boolean isRecording() {
        return unrecorded.size() != 0;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     * Indicates whether some other object is "equal to" this one.
     * boolean DistributedSnapshot.equals(Object o)
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        
        if (o instanceof DistributedSnapshot) {
        	DistributedSnapshot object = (DistributedSnapshot) o;
            return Objects.equals(snapshotID, DistributedSnapshot.snapshotID) &&
                    Objects.equals(localBalance, DistributedSnapshot.localBalance) &&
                    Objects.equals(MoneyInTransfer, DistributedSnapshot.MoneyInTransfer);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     * Overrides the various Object utility Methods
     */
    public String toString() {
    	 return ("ID: " + snapshotID + "\nlocalBalance: " + localBalance +"\nmoneyInTransfer: " + MoneyInTransfer +
    	 "\nunrecordedChannel: " + Arrays.toString(unrecorded.toArray())).toString();
    }
     
     /*
      * (non-Javadoc)
      * @see java.lang.Object#hashCode()
      * Returns a hash code value for the object. This method is supported for the benefit of 
      * hash tables such as those provided by java.util.HashMap.
      */
     public int hashCode() {
         return Objects.hash(snapshotID, localBalance, MoneyInTransfer);
     }

   
}
