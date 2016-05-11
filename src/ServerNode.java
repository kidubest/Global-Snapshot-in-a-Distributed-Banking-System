import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/*
 *Define the Remote Object that implements ServerNodeInterface 
 */
public class ServerNode extends UnicastRemoteObject implements ServerNodeInterface {

    public static Scanner userInput = new Scanner(System.in);
    
    private static NodeState nodeState = NodeState.DISCONNECTED;
    
    private static final String localhost = "127.0.0.1";    
    
    private static final Pattern INTEGER = Pattern.compile("^-?\\d+$");
    
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private static final String TIMEOUT_UNIT = "MILLISECONDS";

	private static final long TIMEOUT_FREQUENCY = 500;

	private static final int numberOfBankNodes = 4;
	
	
	
	/*
	 * ReentrantReadWriteLock for implementing concurrency using lock and unlock method.
	 * To Enable concurrent communication between the BankNodes, four ReentrantReadWriteLocks
	 * are defined for: writeLockAddBankNode, writeLockSendMoney, writeLockReceiveMoney and
	 * writeLockToken
	 */
    private final ReentrantReadWriteLock writeLockAddBankNode = new ReentrantReadWriteLock();
    
    private final ReentrantReadWriteLock writeLockSendMoney = new ReentrantReadWriteLock();
    
    private final ReentrantReadWriteLock writeLockReceiveMoney = new ReentrantReadWriteLock();
    
    private final ReentrantReadWriteLock writeLockToken = new ReentrantReadWriteLock();

	private static BankNode bankNode;

	private static int randomAmount;
	
	/*
	 * Default Constructor for the this class
	 */

	public ServerNode() throws RemoteException {
		super();
	}
	
	/*
	 * A constructor that returns parameter BankNode
	 */
	public ServerNode(BankNode bankNode) throws RemoteException {
		ServerNode.bankNode = bankNode;		
	}
	
	/*
	 * (non-Javadoc)
	 * @see ServerNodeInterface#getBankNode()
	 */
	@Override
	public BankNode getBankNode() throws RemoteException {
		
        System.out.println("BankNode is: " + bankNode);
		return bankNode;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see ServerNodeInterface#addBankNode(int, java.lang.String, int)
	 * A Remote Method that adds the new BankNode to the Existing topology  database in each BankNodes
	 */	
	@Override
	public void addBankNode(int nodeID, String nodeHost) throws RemoteException {
		
		writeLockAddBankNode.writeLock().lock();		
		try {
			System.out.println("Add nodeID/nodeHost: " + nodeID + "/" + nodeHost);
			bankNode.putBankNode(nodeID, nodeHost);
			System.out.println("current Banks in the Topology: "+ Arrays.toString(bankNode.getBankNodes().entrySet().toArray()));
			}
		finally {
			writeLockAddBankNode.writeLock().unlock();
		}
     	
	}

	/*
	 * (non-Javadoc)
	 * @see ServerNodeInterface#sendMoney(int, int)
	 * 
	 * This Remote Method withdraws money from the local balance, if isWithdraw true, sends money to the Remote BankNode and checks
	 * whether the remote branch received or not... If it received the money, displays the amount. If not, invokes the method restoreBalance
	 * from Balance class and displays the amount which is not transfered.
	 */
	@Override
	public void sendMoney(int receiverNodeID, int amount) throws RemoteException {
		
		writeLockSendMoney.writeLock().lock();		
		try {
			boolean isWithdraw = bankNode.getBalance().withdraw(amount);
            if (isWithdraw) {
                System.out.println("Sending money: " + amount + "\nTo ReceiverNode: " + receiverNodeID);
				boolean isReceived = ServerNodeReference.getServerNode(receiverNodeID, bankNode.getBankNodes().get(receiverNodeID)).receiveMoney(bankNode.getNodeID(), amount);
				
                if (isReceived) {
                    System.out.println("Transferred amount: " + amount + "\nTo RecieverNodeID: " + receiverNodeID);
                }              
                else {
                	bankNode.getBalance().restorCurrentBalance();
                    System.out.println("NOT Transferred amount: " + amount + "\nTo ReceiverNode: " + receiverNodeID);
                }
            } 
            
            else {
            	System.out.println("NOT Withdraw money amount: " + amount);
            }
		}
		finally {
			writeLockSendMoney.writeLock().unlock();
		}
                  
    }
	
	/*
	 * (non-Javadoc)
	 * @see ServerNodeInterface#receiveMoney(int, int)
	 * 
	 * This Remote Method checks whether the local BankNode received money from the Remote one, increments the localBalance 
	 * and displays the result
	 */

	@Override
	public boolean receiveMoney(int senderNodeID, int amount) throws RemoteException {
		
		writeLockReceiveMoney.writeLock().lock();
		try {	    	
        	System.out.println("Receiving money amount: " + amount + "\nFrom senderNodeId: " + senderNodeID);
        	bankNode.getDistributedSnapshot().incrementMoneyInTransfer(senderNodeID, amount);
        	bankNode.getBalance().diposit(amount);
            System.out.println("Received, new LocalBalance: " + bankNode.getBalance().getCurrentBalance());
            return true;
            }
		    finally {
		    	writeLockReceiveMoney.writeLock().unlock();
		    	}
		
	}	

	/*
	 * (non-Javadoc)
	 * @see ServerNodeInterface#receiveSnapshotToken(int)
	 * 
	 * This is a Remote Method for invoking snapshot Algorithm.
	 */
	@Override
	public void receiveSnapshotToken(int nodeID) throws RemoteException {       
		
		writeLockToken.writeLock().lock();
		writeLockReceiveMoney.writeLock().lock();
		writeLockSendMoney.writeLock().lock();
		try {
        System.out.println("Received Token from nodeID: " + nodeID);
        DistributedSnapshot distributedSnapshot = bankNode.getDistributedSnapshot();
        if (!distributedSnapshot.isRecording()) {
            bankNode.startDistributedSnapshot();
            System.out.println("Broadcasting Token to neighbours");
            ExecutorService executorService = Executors.newFixedThreadPool(bankNode.getBankNodes().size() - 1);
            bankNode.getBankNodes().entrySet().parallelStream().filter(n -> n.getKey() != bankNode.getNodeID()).forEach(entry -> {
                executorService.execute(() -> {
                    try {
                        ServerNodeReference.getServerNode(entry.getKey(), entry.getValue()).receiveSnapshotToken(bankNode.getNodeID());
                        System.out.println("Token sent to nodeID: " + entry.getKey());
                    } catch (RemoteException e) {
                    	System.out.printf("Failed to sent marker to nodeID: " + entry.getKey(), e);
                    }
                });
            });
        }
        distributedSnapshot.stopRecording(nodeID);
        if (!distributedSnapshot.isRecording()) {
        	System.out.println("Received all Tokens for snapshot on nodeID: " + nodeID);
        	bankNode.stopDistributedSnapshot();
        	}
		}
		finally {
			writeLockToken.writeLock().unlock();
			writeLockReceiveMoney.writeLock().unlock();
			writeLockSendMoney.writeLock().unlock();

		}
		
	}
	
	/*
	 * Main method to run the ServerNode and associated Methods for enabling RMI communication.
	 * 
	 */	
	public static void main(String[] args) {
		
		if(Balance.getMinumuntransferamount() >= Balance.getMaximumtransferamount() || 
				Balance.getMaximumtransferamount() >= Balance.getInitialBalance()) {
			System.out.println("MinumbumTransferAmount < MaximumTransferAmount < InitialBalance");
			return;
		}
		
        System.out.println("\nENTER COMMAND: MethodName NodeHost nodeID existingNodeHost existingNodeID");
        System.out.println("TO CREATE THE FIRST BANKNODE: createBankNode localhost 10");
        System.out.println("TO JOIN BANKNETWORK:          joinNetwork localhost 20 localhost 10");
        System.out.println("TO VIEW THE NETWORK:          viewTopology");
        System.out.println("TO VIEW DISTRIBUTED SNAPSHOT: dss");
        StorageUnit.createDirectry();
        printIPAddress();
        System.out.println("REQUEST THE COMMANDS YOU WANT TO USE...\n");        
        userInputStream(ServerNode.class.getName());
        
    }
	

	/*
	 * This method accepts userInput. If the user types improper commands it displays error and prompts the user
	 * to enter the correct command
	 */
	public static void userInputStream(String className) {
                
        while (userInput.hasNext()) {
            String[] input = userInput.nextLine().split(" ");
            Object[] parameters = new Object[input.length - 1];
            Class<?>[] methodParameterTypes = new Class<?>[input.length - 1];
            
            int i = 1;
            
            while(i < input.length) {
            
                int p = i - 1;
                if (INTEGER.matcher(input[i]).find()) {
                	parameters[p] = Integer.parseInt(input[i]);
                    methodParameterTypes[p] = int.class;
                } else {
                	parameters[p] = input[i];
                    methodParameterTypes[p] = String.class;
                }
                i++;
                
            }            
            
            System.out.println("INVOKING METHOD: " + input[0] + Arrays.toString(parameters));
            try {
                Class.forName(className).getMethod(input[0], methodParameterTypes).invoke(null, parameters);
            } catch (Exception e) {
                System.out.printf("Error: userInput error!", e);
            }
        }
    }
	
	/*
	 * Gets bankNodes with a given nodeID randomly among the set. It returns current BankNode if nodeID is the same, otherwise returns
	 * the remote BankNode.
	 */    
    private static BankNode getRandomNode() {
        int index = new Random().nextInt(bankNode.getBankNodes().size() - 1);
        int nodeID = bankNode.getBankNodes().keySet().parallelStream().filter(n -> n != bankNode.getNodeID()).collect(Collectors.toList()).get(index);
        return new BankNode(nodeID, bankNode.getBankNodes().get(nodeID));
    }
    
    /*
     * Choose BankNodes randomly @getRandomNode and starts transferring Random amount of money in the range
     * between 1 - 100, using the formula:(int)(Math.random() * (Bank_Max_Amount + 1)
     */    
    private static void startMoneyTransferring() {
    	
    	executor.scheduleAtFixedRate((Runnable) () ->  {    		
            try {    	            	                
				if (bankNode != null && bankNode.getBankNodes().size() >= numberOfBankNodes) {					
                    BankNode randomNode = getRandomNode();
                    randomAmount = (int) (Math.random() * (Balance.getMaximumtransferamount() + 1));
                    ServerNodeReference.getServerNode(bankNode).sendMoney(randomNode.getNodeID(), randomAmount);                    
                }
            	
            }
            
    		catch (RemoteException e) {
                System.out.printf("Failed to transfer Money to a random BankNode", e);
            }
        }, 0, TIMEOUT_FREQUENCY, TimeUnit.valueOf(TIMEOUT_UNIT));    	
    	
    }
    
    
    /*
     * A Method for creating RMI Registry on port 1099 with special exception handler
     */    
    private static void createRMIRegistry() {
        try {
            LocateRegistry.createRegistry(1099);
            System.out.println("java RMI Registry created on port: 1099");
        } catch (RemoteException e) {
        	/*
        	 * Do nothing, error means registry already created
        	 */
        	System.out.println("java RMI registry already exists");
        }
    }
    
	
	/*
	 * This Method prints the IP address of all the available interface, if interface is not available
	 * displays Error message.
	 */	    
	public static void printIPAddress() {
        
            System.out.print("\nPrint IPAddress: ");
            Enumeration<NetworkInterface> netInt;
			try {
				netInt = NetworkInterface.getNetworkInterfaces();
			
            while (netInt.hasMoreElements()) {
                Enumeration<InetAddress> inetAddresses = netInt.nextElement().getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    String IPAddress = inetAddresses.nextElement().getHostAddress();
                    if (IPAddress.contains(".") && !IPAddress.equals(localhost)) {
                    	System.out.println(IPAddress);
                    }
                }
            }
			} catch (SocketException e) {
				System.out.println("Error: Cannot get  IP Address!");
				e.printStackTrace();
			}
       
    }
	
	
	
	/*
	 * This Method signals the current BankNode to create the graph
     * nodeID   ID for new current BankNode.
     * nodeHost: host for new current BankNode.
	 */	
    public static void createBankNode(String nodeHost, int nodeID) throws Exception {
    	
    	while(nodeID <= 0) {
            System.out.println("Enter a positive Integer!");
            return;
        }
    	
        while(nodeState != NodeState.DISCONNECTED) {
            System.out.println("NodeState ( " + nodeState + " ) Must be disconnected to create a BankNode");
            return;
        }                
        
        createRMIRegistry();
        System.out.println("NodeID: " + nodeID + " : The first BankNode in the graph is created.");
        bankNode = registerBankNode(nodeID, nodeHost);
        System.out.println( bankNode);
        nodeState = NodeState.CONNECTED;
        startMoneyTransferring();       
    }   
    
    
    /*
     * this method enables the current BankNode to join the existing graph. It fetches existingNodeID and
     * existingNodeHost from the graph.
     * 1) Accumulate the graph structure of all available banks from the existing node
     * 2) BankNodes start randomly sending/accepting money transfers
     *  
     */    
    public static void joinNetwork(String nodeHost, int nodeID, String existingNodeHost, int existingNodeID) throws Exception {
    	
    	while(nodeID <= 0) {
            System.out.println("Enter a positive Integer.");
            return;
        }
    	
    	while(nodeState != NodeState.DISCONNECTED) {
            System.out.println("NodeState ( " + nodeState + " ) Must be disconnected to create a BankNode");
            return;
        }                	    	
        
    	createRMIRegistry();
        System.out.println("NodeID: " + nodeID + " connects to existing nodeID: " + existingNodeID);
        BankNode existingBankNode = ServerNodeReference.getServerNode(existingNodeID, existingNodeHost).getBankNode();        
        while(existingBankNode.getBankNodes().isEmpty()) {
        	System.out.println("Existing node must be operational!");
            return;
        }
        
        bankNode = registerBankNode(nodeID, nodeHost);
        bankNode.putBankNodes(existingBankNode.getBankNodes());
        broadcast();
        System.out.println("NodeID: " + nodeID + " connected as node: " + bankNode + " from existingNode: " + existingBankNode);
        nodeState = NodeState.CONNECTED;
        startMoneyTransferring();
    }
    
    /*
     * Used to bind the BankNodes to the registry using Naming.bind and makes itself available for Remote call.
     */    
    private static BankNode registerBankNode(int nodeID, String nodeHost) throws Exception {
        System.setProperty("java.rmi.server.hostname", nodeHost);
        BankNode bankNode = new BankNode(nodeID, nodeHost);
        Naming.bind("rmi://" + bankNode.getNodeHost() + "/ServerNode" + bankNode.getNodeID(), new ServerNode(bankNode));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
			public void run() {
                System.out.println("Auto-leaving process initiated...");
                try {
                    if (nodeState == NodeState.CONNECTED) {
                        leaveNetwork();
                    }
                } catch (Exception e) {
                    System.out.printf("Failed to leave bankNode", e);
                }
            }
        });
        
              
        return bankNode;
    }
    
    /*
     * Used to View the The topology of the the Network from current BankNode.
     */
    public static void viewTopology() throws RemoteException {
        if (nodeState != NodeState.CONNECTED) {
            System.out.println("Current NodeState is: " + nodeState);
            return;
        }
        System.out.println("Topology view from from BankNode: " + bankNode);
        bankNode.getBankNodes().entrySet().forEach(n -> {
            try {
                ServerNodeReference.getServerNode(n.getKey(), n.getValue()).getBankNode();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /*
     * Unbind itself from the registry using Naming.unbind and signals the disconnection from the Topology.
     */
    private static void leaveNetwork() throws Exception {
        System.out.println("NodeId: " + bankNode.getNodeID() + " is disconnecting from the graph...");
        Naming.unbind("rmi://" + bankNode.getNodeHost() + "/ServerNode" + bankNode.getNodeID());
        System.out.println("NodeID: " + bankNode.getNodeID() + " disconnected");
        bankNode = null;
        nodeState = NodeState.DISCONNECTED;
    }
    
    /*
     * Broadcasts/announces the joining of the network to all the existing BankNodes
     */    
    private static void broadcast() throws RemoteException {
        System.out.println("broadcasting join to bankNodes: " + Arrays.toString(bankNode.getBankNodes().entrySet().toArray()));
        bankNode.getBankNodes().entrySet().parallelStream().filter(n -> n.getKey() != bankNode.getNodeID()).forEach(n -> {
            try {
                ServerNodeReference.getServerNode(n.getKey(), n.getValue()).addBankNode(bankNode.getNodeID(), bankNode.getNodeHost());
                System.out.println("Broadcasted join to nodeID: " + n.getKey());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /*
     * A method that initiates DistributedSnapshot Algorithm 
     */    
    public static void dss() throws RemoteException {
        if (nodeState != NodeState.CONNECTED) {
            System.out.println("Must be Connected to start DistributedSnapshot. Current NodeState is: " + nodeState);
            return;
        }
        System.out.println("Starting DistributedSnapshot @BankNode: " + bankNode);
        ServerNodeReference.getServerNode(bankNode).receiveSnapshotToken(bankNode.getNodeID());
    }  
	


		
}	
	
	
	
	



