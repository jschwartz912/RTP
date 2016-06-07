import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Random;

import java.io.FileOutputStream;

public class FTAClient {

	public static void main(String[] args) throws IOException {
		// Check for correct arguments
		if (args.length != 2) { 
 			System.out.println("Requied input format:'ftaclient H:P W'"); 
 			return; 
 		} 

		// Initialize arguments
		String[] serv = args[0].split(":");
		InetAddress servIP = InetAddress.getByName(serv[0]);
		int servPort = Integer.parseInt(serv[1]);
		int rwndMaxSize = Integer.parseInt(args[1]);

		// Scanner to read in user commands
		Scanner keyboard = new Scanner(System.in);

		// Determine a random port number to listen on
		Random r = new Random();
		int clientPort = r.nextInt(64511) + 1024;
		boolean isValidPort = false;
		DatagramSocket socket;
		while (!isValidPort) {
			try {
				socket = new DatagramSocket(clientPort);
				isValidPort = true;
				// Initialize RTP instance
				RTP rtp = new RTP(socket, rwndMaxSize);
				// Connect to the server
				rtp.connect(socket, clientPort, servPort, servIP);
				rtp.listen(socket);
				boolean isRunning = true;
				while (isRunning) {
					// Receive command from user
					System.out.println("Enter 'get <file_name>' or 'disconnect'"); 
					System.out.print("Command: ");
					String input = keyboard.nextLine();
					if (!input.equals("") && !input.equals("disconnect")) {
						byte[] cmd = null;
			 			// Error checking on command, should only be able to
			 			// enter "get <filename>"
			 			String[] parsedCMD = input.split("\\s+");
			 			if ((parsedCMD.length == 2) && (parsedCMD[0].equals("get"))) {
				 			cmd = input.getBytes();
			 			}
			 							
						if (cmd != null) {
							//Send command to the server 
							rtp.send(socket, clientPort, servPort, servIP, cmd); 
						} else {
							System.out.println("Invalid Command");
							System.exit(1);
						}

						FileOutputStream fos = new FileOutputStream("get-" + parsedCMD[1].trim());;						
						int receivedBytes = 968; // start at something > 968
						while (receivedBytes % 968 == 0) {
							byte[] data = rtp.receive(0);
							if (data != null) {
								String s = new String(data);
								if (s.equals("File does not exist.")) {
									System.out.println(s);
									fos.close();
									(new File("get-" + parsedCMD[1].trim())).delete();
									break;
								}
								try {
									fos.write(data);
									receivedBytes = data.length;
									//System.out.println("Received " + data.length + " bytes");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						fos.close();
					} else if (input.equals("disconnect")) {
						rtp.endConnection(socket, clientPort, servPort, servIP);
						isRunning = false;
						return;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
