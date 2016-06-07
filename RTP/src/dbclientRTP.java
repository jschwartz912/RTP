import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
/*
* Jules Schwartz
* Networking Programming HW1
* RTP Client - Java
*/
public class dbclientRTP {
	
    public static void main(String[] args) throws IOException 
    {
    	InetAddress address;
    	int servPort;
    	//should receive ip:port ID and at least one query
    	//should receive max of 7 parameters
    	if (args.length < 2 || args.length > 7){
			System.out.println("Error. Must enter up to 5 queries using the following format...");
			System.out.println("dbclientTCP 127.0.0.1:<port#> <ID#> <query1> <query2> <query3> <query4> <query5>");
			System.exit(1);
		}
    	address = InetAddress.getByName(args[0].substring(0, args[0].indexOf(":")));
    	servPort = Integer.parseInt(args[0].substring(args[0].indexOf(":")+1));
    	// Determine a random port number to listen on
		Random r = new Random();
		int clientPort = r.nextInt(64511) + 1024;
		boolean isValidPort = false;
		DatagramSocket socket;
		while(!isValidPort) {
	    	try {
	    		socket = new DatagramSocket(clientPort);
	    		isValidPort = true;
				// Initialize RTP instance
		    	RTP rtp = new RTP(socket, 1000);
				// Connect to the server
		    	rtp.connect(socket, clientPort, servPort, address);
		    	rtp.listen(socket);
	    	
		    	
		    		
		    		//Pass ID/queries to server
		    		String s = "";
		    		byte[] toServer = null;
			    	for (int i = 1; i < args.length; i++) {
			    		s+= args[i] + " ";
			    	}
			    	toServer = s.getBytes();
			    	rtp.send(socket, clientPort, servPort, address, toServer);
			    	
					int receivedBytes = 968; // start at something > 968
					while (receivedBytes % 968 == 0) {
						byte[] data = rtp.receive(0);
						if (data != null) {
							String out = new String(data);
							System.out.println(out);
							break;
						}
					}
					//System.out.println("Closing Client");
					rtp.endConnection(socket, clientPort, servPort, address);
					return;

		    	
	        } catch (IOException e) {
	             e.printStackTrace();
	        } 
    	}
    }
}
