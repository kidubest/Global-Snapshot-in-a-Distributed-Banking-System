import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/*
 * This class deals with RMI for BankNodes while invoking methods between them.
 */
public abstract class ServerNodeReference {

   
    public ServerNodeReference() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}
    

    /*
     * A method is used to return reference to the Remote BankNode with Return Parameter of node.
     */
	public static ServerNodeInterface getServerNode(BankNode bankNode) {
        return getServerNode(bankNode.getNodeID(), bankNode.getNodeHost());
    }

    /*
     * A method used to return reference to the Remote BankNode with return parameter of nodeID and nodeHost. 
     */
    
    public static ServerNodeInterface getServerNode(int nodeID, String nodeHost) {
        
            try {
				return (ServerNodeInterface) Naming.lookup("rmi://" + nodeHost + "/ServerNode" + nodeID);
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				System.out.println("Cannot get the Remote Node: " + nodeID);
				e.printStackTrace();
			}
			return null;
       
    }
}
