package clientsync;

/**
 * @author quangdng
 * @date 1st May 2014
 */

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

/*
 * The SyncThreadClient provides threads that consists of server connection, 
 * logic for passing and receiving messages back-and-forth from server.
 */

public class SyncThreadClient implements Runnable {

	// File to synchronize on client side
	private SynchronisedFile clientFile;

	// Host & port of that host
	private String host;
	private int port;

	// Block size
	private int blockSize;
	
	// Direction
	private String direction;

	// Message from server
	private String serverMsg = "";

	// Message from client
	private String clientMsg = "";
	
	// Is first request from client
	private boolean isFirstReg = true;
	
	// Instruction that needs to be upgraded from CopyBlock to NewBlock
	Instruction instToUp = null;
	
	// Store instructions in queue to process
	private Queue<Instruction> instructionPool = new LinkedList<Instruction>();

	/**
	 * SyncThreadClient constructor
	 * 
	 * @param file
	 *            File to synchronize on client side
	 * @param host
	 *            Host of server
	 * @param port
	 *            Port of host
	 */
	SyncThreadClient(SynchronisedFile file, String host, int port, int blockSize, String direction) {
		this.clientFile = file;
		this.host = host;
		this.port = port;
		this.blockSize = blockSize;
		this.direction = direction;
	}
	
	/*
	 * Main logic for the thread to communicate with server.
	 */
	@Override
	public void run() {
		
		// Main logic
		while (true) {

			// Send block size & direction to server
			if (isFirstReg) {
				// Set it to false
				isFirstReg = false;
				this.firstReg();
				// Prepare message for BlockSize
			}
			
			// Operate in push direction
			else if (this.direction.equals("push"))	{
				this.push();
			}
			else if (this.direction.equals("pull")) {
				this.pull();
			}
		}
	}
	
	private void firstReg() {
		
		// Socket to connect to server
		Socket socket;
		
		try {
			// Connect progress
			InetAddress hostAddress = InetAddress.getByName(this.host);
			System.out.println("Connecting to: " + hostAddress + ":"
					+ this.port);
			socket = new Socket(hostAddress, this.port);
			System.out.println("Connected to: "
					+ socket.getRemoteSocketAddress());

			// Send message to server
			PrintWriter toServer = new PrintWriter(
					socket.getOutputStream(), true);
			this.clientMsg = "BlockSize:" + Integer.toString(this.blockSize) + ";Direction:" + this.direction;
			toServer.println(this.clientMsg);
			System.out.println("Sent => " + this.clientMsg);

			// Receive message from server
			BufferedReader fromServer = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			this.serverMsg = fromServer.readLine();
			System.out.println("Server reply => " + this.serverMsg);

			// Close connections
			fromServer.close();
			toServer.close();
			socket.close();
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println();
		
	}
	
	/**
	 * Method for synchronise file while considering Client as sender
	 * and Server as receiver
	 */
	private void push() {
		// Instructions of synchronized file
		Instruction inst;

		// Socket to connect to server
		Socket socket;
				
		try {
			// Connect progress
			InetAddress hostAddress = InetAddress.getByName(this.host);
			System.out.println("Connecting to: " + hostAddress + ":"
					+ this.port);
			socket = new Socket(hostAddress, this.port);
			System.out.println("Connected to: "
					+ socket.getRemoteSocketAddress());

			// Send message to server
			PrintWriter toServer = new PrintWriter(
					socket.getOutputStream(), true);

			// Upgrade instruction to NewBlock to handle
			// BlockUnavailableException
			if (this.serverMsg.equals("BlockUnavailableException")) {
				System.out.println("Upgrading intruction...");
				this.instToUp = new NewBlockInstruction(
						(CopyBlockInstruction) this.instToUp);
				this.clientMsg = this.instToUp.ToJSON();
				toServer.println(this.clientMsg);
				System.out.println("Sent: " + this.clientMsg);
			}

			// Send next instruction if no exception throwed from server
			else {
				inst = clientFile.NextInstruction();
				this.instToUp = inst;
				this.clientMsg = inst.ToJSON();
				toServer.println(this.clientMsg);
				System.out.println("Sent => " + this.clientMsg);
			}

			// Receive message from server
			BufferedReader fromServer = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			this.serverMsg = fromServer.readLine();
			System.out.println("Server reply => " + this.serverMsg);

			// Close connections
			fromServer.close();
			toServer.close();
			socket.close();
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println();
	}
	
	/**
	 * Method for synchronise file while considering Client as receiver
	 * and Server as sender
	 */
	private void pull() {
		// Instructions of synchronized file
		Instruction inst;
		
		// Instruction factory to convert JSON message
		InstructionFactory instructFact = new InstructionFactory();

		// Socket to connect to server
		Socket socket;
		
		try {
			// Connect progress
			InetAddress hostAddress = InetAddress.getByName(this.host);
			System.out.println("Connecting to: " + hostAddress + ":"
					+ this.port);
			socket = new Socket(hostAddress, this.port);
			System.out.println("Connected to: "
					+ socket.getRemoteSocketAddress());
			
			// Receive message from server
			BufferedReader fromServer = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			this.serverMsg = fromServer.readLine();
			System.out.println("Server reply => " + this.serverMsg);
			
			// Send message to server
			PrintWriter toServer = new PrintWriter(
					socket.getOutputStream(), true);
			
			// Process instructions from Server
			inst = instructFact.FromJSON(this.serverMsg);
			this.instructionPool.add(inst);
			
			if (this.instructionPool.size() != 0) {
				try {
					this.clientFile
							.ProcessInstruction(this.instructionPool
									.poll());
					this.clientMsg = "Successfully processed!";
					toServer.println(this.clientMsg);
				
				// Handle BlockUnavailableException
				} catch (BlockUnavailableException e) {
					this.clientMsg = "BlockUnavailableException";
					toServer.println(this.clientMsg);
				}
			}			
			
			System.out.println("Sent => " + this.clientMsg);
			
			// Close connections
			fromServer.close();
			toServer.close();
			socket.close();
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println();
	}
}
