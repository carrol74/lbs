package backEnd;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class Wallet {
    /**
     * The RandomAccessFile of the wallet file
     */  
    private RandomAccessFile file;

    /**
     * Creates a Wallet object
     *
     * A Wallet object interfaces with the wallet RandomAccessFile
     */
    public Wallet () throws Exception {
	this.file = new RandomAccessFile(new File("backEnd/wallet.txt"), "rw");
    }

    /**
     * Gets the wallet balance. 
     *
     * @return                   The content of the wallet file as an integer
     */
    public int getBalance() throws IOException {
	this.file.seek(0);
	return Integer.parseInt(this.file.readLine());
    }

    /**
     * Sets a new balance in the wallet
     *
     * @param  newBalance          new balance to write in the wallet
     */
    public void setBalance(int newBalance) throws Exception {
	this.file.setLength(0);
	String str = Integer.valueOf(newBalance).toString()+'\n'; 
	this.file.writeBytes(str); 
    }

    /**
     * Closes the RandomAccessFile in this.file
     */
    public void close() throws Exception {
	this.file.close();
    }

    /**
     * Safely withdraws money from the wallet using file locking
     * to prevent race conditions
     * @param valueToWithdraw amount to with draw
     * @return
     * @throws
     */
    public boolean safeWithdraw(int valueToWithdraw) throws Exception {
        FileLock lock = null;
        boolean success = false;

        try {
            lock = this.file.getChannel().lock();
            if (lock != null) {
                int currentBalance = getBalance();
                if (currentBalance >= valueToWithdraw) {
                    success = true;
                    setBalance(currentBalance - valueToWithdraw);
                }
            } else {
                throw new IllegalStateException("Wallet is being used");
            }
            return success;
        } finally {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        }
    }
}