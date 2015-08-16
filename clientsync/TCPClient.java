package clientsync;

/**
 * @author quangdng
 * @date 1st May 2014
 */

import java.io.*;

import org.kohsuke.args4j.*;

import static org.kohsuke.args4j.ExampleMode.ALL;

public class TCPClient {

	/*
	 * Instance variables represent command line arguments
	 */

	// -file argument
	@Option(name = "-file", required = true, usage = "Specify the file to start sync", 
			metaVar = "FILENAME")
	private String clientFileName;

	// -host argument
	@Option(name = "-host", required = true, 
			usage = "Specify the hostname of server to connect to", metaVar = "HOST")
	private String host;

	// -port argument
	@Option(name = "-port", required = true, 
			usage = "Specify which port of the host to connect to", metaVar = "PORT")
	private int port;

	// -blocksize argument
	@Option(name = "-blocksize", usage = "Specify the block size of file's blocks. "
			+ "Default is 1024 bytes and custom size should be larger than 32 bytes", 
			metaVar = "SIZE")
	private int blockSize = 1024;

	// -blocksize argument
	@Option(name = "-direction", 
			usage = "Specify the direction to synchorize file (push or pull). "
					+ "Default is push (from client to server).", 
					metaVar = "DIRECTION")
	private String direction = "push";

	/**
	 * This method initiates new thread for synchronisation and continiously 
	 * check for synchronised file state if it is operating in "push" direction
	 */
	public void sync() {
		// Synchronised file
		SynchronisedFile clientFile = null;
		try {
			clientFile = new SynchronisedFile(this.clientFileName,
					this.blockSize);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		/*
		 * Create new thread to communicate with server
		 */
		Thread stt = new Thread(new SyncThreadClient(clientFile, this.host,
				this.port, this.blockSize, this.direction));
		stt.start();

		/*
		 * Continue to check client file state for every 5 seconds if the input
		 * direction is "push"
		 */
		if (this.direction.equals("push")) {
			while (true) {
				try {
					clientFile.CheckFileState();
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

	/**
	 * This method provides convenient way to parse command line arguments
	 * and validate them.
	 * @param args Command line arguments array
	 * @throws IOException
	 */
	public void parseArgs(String[] args) throws IOException {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			// Validate arguments
			if (this.blockSize <= 32
					|| (!this.direction.equals("push") && !this.direction
							.equals("pull"))) {
				String errMsg = "";
				if (this.blockSize <= 32) {
					errMsg += "Block size should be larger than 32 bytes.\n";
				}

				if (!this.direction.equals("push")
						&& !this.direction.equals("pull")) {
					errMsg += "Direction must be \"push\" or \"pull\"";
				}
				throw new CmdLineException(parser, errMsg);
			}
		} catch (CmdLineException e) {
			// print error message
			System.err.println(e.getMessage() + "\n");
			System.err
					.println("java -jar syncclient.jar [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// print option sample
			System.err.println(" Example: java -jar syncclient.jar"
					+ parser.printExample(ALL));

			System.exit(-1);
		}
	}

	public static void main(String[] args) throws IOException {
		TCPClient client = new TCPClient();
		client.parseArgs(args);
		client.sync();
	}
}