import java.rmi.Remote;
import java.rmi.RemoteException;

/*
 * Remote interface which is the "Heart of RMI" Communications. It defines the methods to be implemented by the ServerNode Class.
 * Here, only the definition of the Remote Methods is provided and the actual implementations of the methods is provided in
 * the ServerNode class that implements this interface. This will be accessed remotely by the Remote caller through RMI and 
 * the Service will be provided by the class that implements this Object. 
 */
public interface ServerNodeInterface extends Remote {
	
	/*
	 * This Remote Method is used to get access to the BankNode class.
	 * See class @ BankNode
	 */	
	public BankNode getBankNode() throws RemoteException;
	
	/*
	 * This Remote method is used to add BankNode with specified nodeID and nodeHost. 
	 */	
	public void addBankNode(int nodeID, String nodeHost) throws RemoteException;
	
	/*
	 * This boolean Remote method displays Received money from the Remote BankNode with specified senderNodeID
	 * and amount of money received. It then calls the method increaseBalance()@ BankNode class and returns
	 * true.
	 */	
	public boolean receiveMoney (int senderNodeID, int amount) throws RemoteException;
	
	/*
	 * This Remote method transfers money from the local BankNode to the Remote node. It first Withdraws money from
	 * the local Branch by calling method decreaseBalance @BankNode. If The money is Accepted by the Remote node, the 
	 * local balance will be deducted, other wise it Restores the local Balance by calling restoreBalance @BankNode.
	 */
	public void sendMoney (int receiverNodeID, int amount ) throws RemoteException;
	
	/*
	 * This Remote method is used in Distributed Snapshot algorithm to initiate, process and send Token/Marker on 
	 * the outgoing links.
	 */

	public void receiveSnapshotToken(int nodeID) throws RemoteException;

	
	
    
	
 }