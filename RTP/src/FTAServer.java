import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;

public class FTAServer {
	public static void main(String[] args) throws IOException {
		// Check for correct arguments
		if (args.length != 2) {
			System.out.println("Requied input format:'ftaserver P W'");
		}
		// Initialize arguments
		int servPort = Integer.parseInt(args[0]);
		int rwndMaxSize = Integer.parseInt(args[1]);
		// Convert files to byte arrays
		//byte[] jpgFile = Files.readAllBytes(Paths.get("3251.jpg"));
		//byte[] txtFile = Files.readAllBytes(Paths.get("sample.txt"));
		//byte[] largeFile = Files.readAllBytes(Paths.get("longsample.txt"));
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(servPort);
			RTP rtp = new RTP(socket, rwndMaxSize);
			System.out.println("Server is listening... (" + InetAddress.getLocalHost().toString() + ", " + servPort + ")");
			rtp.listen(socket);
			while (true) {
				for (int i = 0; i < rtp.MAX_CONNECTIONS; i++) {
					byte[] data = rtp.receive(i);
					if (data != null) {
						//index 0 : (post or get) 
						//index 1: <filename>
						String cmd = new String(data, "UTF-8");
						//System.out.println("Received command: " + cmd);
						String[] parsedCMD = cmd.split("\\s+");
						//Send file based on user GET cmd
						byte[] sendData = new String("File does not exist.").getBytes();
						if (parsedCMD[0].equals("get")) {
							String fileName = parsedCMD[1].trim();
							sendData = Files.readAllBytes(Paths.get(fileName));
							/*if (parsedCMD[1].trim().equals("3251.jpg")) {
								sendData = jpgFile;
							} 
							if (parsedCMD[1].trim().equals("sample.txt")) {
								sendData = txtFile;
							} 
							if (parsedCMD[1].trim().equals("longsample.txt")) {
								sendData = largeFile;
							}*/
						}
						
						int clientPort = rtp.getConnections()[i].getClientPort();
						InetAddress clientIP = rtp.getConnections()[i].getSendIP();

						// and use rtp.send() to send those packets to the client
												
						rtp.send(socket, servPort, clientPort, clientIP, sendData);
						
					}
				}
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
