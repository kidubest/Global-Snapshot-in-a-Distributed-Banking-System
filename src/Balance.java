import java.io.Serializable;
import java.util.Objects;

/**
 * Serializable class for computing balance. When this class is invoked, its value will be Serialized and transported 
 * to the caller by value instead of by reference. It has getter and setter methods to access private local variables. 
 *  In addition it has methods to decreaseBalance, increase balance, restore balance based
 *  on the withdrawAmount or the received money amount
 */

public class Balance implements Serializable {

	
	private int currentBalance;
	
	private static final int initialBalance = 1000000;
	
	private static final int maximumTransferAmount = 100;
	
	private static final int minumunTransferAmount = 1;
	
	private int withdrawAmount;
	
	/*
	 * A constructor for the class.
	 */	
	public Balance(int localBalance) {
		this.currentBalance = localBalance;
	}

	/**
	 * @return the balance
	 */
	public int getCurrentBalance() {
		return currentBalance;
	}

	public void restorCurrentBalance() {
		this.currentBalance += withdrawAmount;
		withdrawAmount = 0;
	}
	
	public void diposit(int amount) {
		currentBalance += amount;
	}
	
	public boolean withdraw(int amount) {
		if(currentBalance >= amount) {
			currentBalance -= amount;
			withdrawAmount = amount;
			return true;		
		}
		else
			return false;
		
	}

	/*
	 * return the initialBalance
	 */
	public static int getInitialBalance() {
		return initialBalance;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 * Indicates whether some other object is "equal to" this one
	 * boolean Balance.equals(Object o)
	 */
	 public boolean equals(Object o) {
	        if (this == o) return true;
	        
	        if (o instanceof Balance) {
	            Balance object = (Balance) o;
	            return Objects.equals(currentBalance, object.currentBalance);
	        }
	        
	        return false;
	    }
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * Overrides the various Object utility Methods.
	 */
	public String toString() {
		return ("balance " + currentBalance).toString();
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 * Returns a hash code value for the object. This method is supported for the benefit of 
	 * hash tables such as those provided by java.util.HashMap.
	 */
	public int hashCode() {
		return Objects.hash(currentBalance);
	}

	/**
	 * @return the maximumtransferamount
	 */
	public static int getMaximumtransferamount() {
		return maximumTransferAmount;
	}

	/**
	 * @return the minumuntransferamount
	 */
	public static int getMinumuntransferamount() {
		return minumunTransferAmount;
	}
	
	
}




