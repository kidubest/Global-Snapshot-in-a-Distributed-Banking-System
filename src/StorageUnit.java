import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class StorageUnit {
	/*
     * The following Methods do the creating/Storage/update of snapshot files of each BankNode and are not part of the snapshot 
     * algorithm itself.
     */
    
    /*
     * A method for Creating/updating each BankNodes snapshot into CSV file
     */    
    public static void writeToStorage(BankNode bankNode) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(getFileName(bankNode.getNodeID()), true)))) {
            DistributedSnapshot distributedSnapshot = bankNode.getDistributedSnapshot();
            writer.println(DistributedSnapshot.getSnapshotID() + "," + DistributedSnapshot.getLocalBalance() + "," + DistributedSnapshot.getMoneyInTransfer());
            System.out.println("Storage wrote a snapshot: " + distributedSnapshot);
        } catch (Exception e) {
            System.out.printf("Failed to write snapshot of node: " + bankNode, e);
        }
    }

    private static String getFileName(int nodeID) {
        return "storageFolder" + "/BankNode-" + nodeID + ".csv";
	}
    
    /*
     * Creates storage folder to save BankNode files in CSV format
     */
    public static void createDirectry() {
        try {
            Path path = Paths.get("storageFolder");
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (Exception e) {
            System.out.printf("Cannot create storageFolder", e);
        }
    }
    
    /*
     * Remove the nodes CSV file if it already exists.
     */
    public static void removeFile(int nodeID) {
        try {
            Path path = Paths.get(getFileName(nodeID));
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception e) {
            System.out.println("Unable to remove file for nodeID: " + nodeID + e);
        }
    }
    
    
   
}