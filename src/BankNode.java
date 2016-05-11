import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*This is a Serializable Remote class for representing the BankNodes.
 * Each BankNode has nodeID and nodeHost-->(hostName/IPAddress), it has getter methods to return this variables.
 */

public class BankNode implements Serializable {
	
	private final int nodeID;
	
	private final String nodeHost;
	
	private final Balance balance;	
	
    private final DistributedSnapshot distributedSnapshot;	
    
    /*
     * An Object that Maps a key to a value
     */      
	private final Map<Integer, String> bankNodes = new HashMap<>();
	
	
	public BankNode() {
		this(0, "");
	}
	
	public BankNode(int nodeID, String nodeHost) {
		this.nodeID = nodeID;
		this.nodeHost = nodeHost;		
		balance = new Balance(Balance.getInitialBalance());
		distributedSnapshot = new DistributedSnapshot();
		bankNodes.put(nodeID, nodeHost);
	}

	/**
	 * @return the nodeID
	 */
	public int getNodeID() {
		return nodeID;
	}

	/**
	 * @return the nodeHost
	 */
	public String getNodeHost() {
		return nodeHost;
	}

	/**
	 * @return the balance
	 */
	public Balance getBalance() {
		return balance;
	}

	/**
	 * @return the distributedSnapshot
	 */
	public DistributedSnapshot getDistributedSnapshot() {
		return distributedSnapshot;
	}

	/*
	 * @return the HashMap(bankNodes)
     * A HashMap which is used to store instance of BankNodes. Each BankNode is keyed by nodeID. 
     * It is implied that each BankNode with the same nodeID are equivalent. Hence, a bank
     * cannot contain duplicate keys, each key can Map to atmost one value.
     */	
	public Map<Integer, String> getBankNodes() {
		return Collections.unmodifiableMap(bankNodes);
	}
	
	public void putBankNodes(Map<Integer, String> bankNodes) {
		this.bankNodes.putAll(bankNodes);
	}
	
	public void putBankNode(int nodeID, String nodeHost) {
		bankNodes.put(nodeID, nodeHost);
	}
	
	public void startDistributedSnapshot() {
		distributedSnapshot.startDistributedSnapshot(nodeID, balance.getCurrentBalance(), bankNodes);
	}
	
	public void stopDistributedSnapshot() {
		StorageUnit.writeToStorage(this);
		distributedSnapshot.stopDistributedSnapshot();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 * Indicates whether some other object is "equal to" this one
	 * boolean BankNode.equals(Object o)
	 */
	
	public boolean equals(Object o) {
		if(this == o) return true;	
		
		if(o instanceof BankNode) {
			BankNode object = (BankNode) o;
			return Objects.equals(nodeID, object.nodeID);
		}
		
		return false;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * Overrides the various Object utility Methods
	 */
	
	public String toString() {
		return ("Node ID: " + nodeID) +("\nHost: " + nodeHost) + ("\nBalance: " + balance) +
				("\ndistributedSnapshot: " + distributedSnapshot) +
				("banknodes: " + Arrays.toString(bankNodes.entrySet().toArray())).toString();		
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 * Returns a hash code value for the object. This method is supported for the benefit of hash tables 
	 * such as those provided by java.util.HashMap.
	 */
	public int hashCode() {
		return Objects.hash(nodeID);
	}
	
		    
}



