package serversync;

/**
 * @author quangdng
 * @date 1st May 2014
 */

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.io.*;

import org.kohsuke.args4j.*;

public class TCPServer {

	/*
	 * Instance variables represent command line arguments
	 */

	// -file argument
	@Option(name = "-file", required = true, 
			usage = "Specify the file to start sync", metaVar = "FILENAME")
	private String serverFileName;

	// -port argument
	@Option(name = "-port", required = true, 
			usage = "Specify which port of the host to connect to", metaVar = "PORT")
	private int port;

	// File to synchronize on server side
	private SynchronisedFile serverFile;

	// Store instructions to process
	private Queue<Instruction> instructionPool = new LinkedList<Instruction>();

	// Block size
	private int blockSize = 1024;

	// Direction
	private String direction = "pull";

	// Message from server
	private String serverMsg = "";

	// Message from client
	private String clientMsg = "";

	public void runServer() throws IOException {
		InstructionFactory instructFact = new InstructionFactory();
		Instruction inst;

		// Instruction that needs to be upgraded from CopyBlock to NewBlock
		Instruction instToUp = null;

		ServerSocket serverSocket;

		try {

			serverSocket = new ServerSocket(this.port);

			while (true) {
				System.out.println("Waiting for client on port "
						+ serverSocket.getLocalPort() + "...");
				Socket server = serverSocket.accept();
				System.out.println("Connected to "
						+ server.getRemoteSocketAddress());

				// Read message from client
				BufferedReader fromClient = new BufferedReader(
						new InputStreamReader(server.getInputStream()));

				if (this.direction.equals("pull")) {
					this.clientMsg = fromClient.readLine();
					System.out.println("Received => " + this.clientMsg);
				}

				// Send message to client
				PrintWriter toClient = new PrintWriter(
						server.getOutputStream(), true);

				/*
				 * Process initial client message
				 */
				if (this.clientMsg != null
						&& this.clientMsg.contains("BlockSize:")) {
					// Set up synchronization
					this.setupSync();
					
					// Reset client message
					this.clientMsg = "";
					
					// Return message
					this.serverMsg = "Finished setting up synchonization !";
					toClient.println(this.serverMsg);
				}

				/*
				 * Synchronise in "pull" direction of Server (Client as sender,
				 * Server as receiver)
				 */
				else if (this.direction.equals("pull")) {
					try {
						inst = instructFact.FromJSON(this.clientMsg);
						this.instructionPool.add(inst);
					} catch (NullPointerException e) {
						serverSocket.close();
						break;
					}

					if (this.instructionPool.size() != 0) {
						try {
							// The Server processes the instruction
							this.serverFile
									.ProcessInstruction(this.instructionPool
											.poll());
							this.serverMsg = "Successfully processed!";
							toClient.println(this.serverMsg);
						} catch (BlockUnavailableException e) {
							this.serverMsg = "BlockUnavailableException";
							toClient.println(this.serverMsg);
						}
					}
				}

				/*
				 * Synchronise in "push" direction of Server (Client as
				 * receiver, Server as sender)
				 */
				else if (this.direction.equals("push")) {

					// Upgrade instruction to NewBlock to handle
					// BlockUnavailableException
					if (this.clientMsg != null
							&& this.clientMsg
									.equals("BlockUnavailableException")) {
						System.out.println("Upgrading intruction...");
						instToUp = new NewBlockInstruction(
								(CopyBlockInstruction) instToUp);
						this.serverMsg = instToUp.ToJSON();
						toClient.println(this.serverMsg);
					}

					// Send next instruction if no exception throwed from server
					else {
						inst = this.serverFile.NextInstruction();
						instToUp = inst;
						this.serverMsg = inst.ToJSON();
						toClient.println(this.serverMsg);
					}

					// Read message from client
					this.clientMsg = fromClient.readLine();
					System.out.println("Received => " + this.clientMsg);
				}

				System.out.println("Sent => " + this.serverMsg);
				fromClient.close();
				toClient.close();

				System.out.println();
			}
		}

		catch (UnknownHostException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

		System.out.println();
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

		} catch (CmdLineException e) {
			// Print error message
			System.err.println(e.getMessage() + "\n");
			System.err
					.println("java -jar syncserver.jar [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// Print option sample
			System.err.println(" Example: java -jar syncserver.jar"
					+ parser.printExample(ALL));

			System.exit(-1);
		}
	}
	
	/**
	 * This method processes initial message from client including block size
	 * and direction of the synchronisation
	 */
	private void setupSync() {
		// Parse client message
		String[] temp = this.clientMsg.split(";");
		this.blockSize = Integer.parseInt(temp[0].split(":")[1]);
		this.direction = temp[1].split(":")[1];

		// Reverse direction for easy understanding
		if (this.direction.equals("push")) {
			this.direction = "pull";
		} else {
			this.direction = "push";
		}

		// Initiate synchonised file
		try {
			this.serverFile = new SynchronisedFile(this.serverFileName,
					this.blockSize);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		if (this.direction.equals("push")) {
			/*
			 * Start a thread to service the Instruction queue.
			 */
			Thread stt = new Thread(new SyncThreadServer(this.serverFile));
			stt.start();
		}
	}

	public static void main(String[] args) throws IOException {
		TCPServer srv = new TCPServer();
		srv.parseArgs(args);
		while (true) {
			srv.runServer();
		}
	}
}