package serversync;

/**
 * @author quangdng
 * @date 1st May 2014
 */

import java.io.*;

/*
 * The SyncThreadServer provides threads that consists of server connection, 
 * logic for passing and receiving messages back-and-forth from client.
 */
public class SyncThreadServer implements Runnable {

	// File to synchronize on server side
	private SynchronisedFile serverFile;

	/**
	 * SyncThreadServer constructor
	 * 
	 * @param file
	 *            File to synchronize on server side
	 */
	SyncThreadServer(SynchronisedFile file) {
		this.serverFile = file;
	}

	/*
	 * Main logic for server thread to communicate with client.
	 */
	@Override
	public void run() {
		while (true) {
			try {
				this.serverFile.CheckFileState();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
