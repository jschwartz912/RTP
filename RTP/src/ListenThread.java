import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Arrays;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

/**
 * This is a thread that will run in the background of the application using
 * RTP. It will continuously listen for new packets, receive them, and store them
 * in the receive buffer for the particular client they were received from.
 */
public class ListenThread extends Thread {
	private Thread t;
   	private RTP rtp;
   	private DatagramSocket socket;
    //private Connection[] connections;
	private boolean isRunning;
	private Lock connectionsLock;
   
   	public ListenThread(RTP rtp, DatagramSocket socket) {
       this.rtp = rtp;
       this.socket = socket;
       //this.connections = rtp.getConnections();
		isRunning = true;
		this.connectionsLock = new ReentrantLock();
   	}

	public boolean getIsRunning() {
		return isRunning;
	}

	public void sendFinMessage(int srcPort, int dstPort, InetAddress serverAddress) {
		try {
			// Send FIN message
			RTPPacket finPacket;
			RTPHeader finHeader = new RTPHeader(srcPort, /*InetAddress.getByName("127.0.0.1").getAddress()*/InetAddress.getLocalHost().getAddress(), dstPort, serverAddress.getAddress(), 0);
			finHeader.setFIN(true);
			finPacket = new RTPPacket(finHeader, null);
			finPacket.updateChecksum();
			byte[] finPacketBytes = finPacket.getPacketByteArray();
			DatagramPacket sendPacket = new DatagramPacket(finPacketBytes, finPacketBytes.length, serverAddress, dstPort);
			socket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		isRunning = false;
	}

   	public void run() {
        try {
            while (isRunning) {
                DatagramPacket recvPacket = new DatagramPacket(new byte[rtp.MSS], rtp.MSS);
                socket.receive(recvPacket);
				
				rtp.getConnectionsLock().lock();				
				Connection[] connections = rtp.getConnections();

                byte[] receivedData = new byte[recvPacket.getLength()];
                receivedData = Arrays.copyOfRange(recvPacket.getData(), 0, recvPacket.getLength());
                RTPPacket receivedRTPPacket = new RTPPacket(receivedData);
                RTPHeader receivedRTPHeader = receivedRTPPacket.getHeader();
                // Check for corrupted packets
                int headerCHKSUM = receivedRTPHeader.getChecksum();
                receivedRTPPacket.updateChecksum();
                int updatedCHKSUM = receivedRTPPacket.getHeader().getChecksum();
                if (headerCHKSUM == updatedCHKSUM) {
	                if (receivedRTPHeader.isSYN()) {
	                    // Create a new connection and initialize the receive buffer
	                    Connection c = new Connection(receivedRTPHeader.getSrcIP(), recvPacket.getAddress(), receivedRTPHeader.getSrcPort(), rtp.MAX_RWND, receivedRTPHeader.getRwndSize(), rtp.MSS);
						//System.out.println(InetAddress.getByAddress(receivedRTPPacket.getHeader().getSrcIP()).toString());
						//System.out.println(recvPacket.getAddress().toString());
                        // Check to see if this is a duplicate syn message
                        boolean isAlreadyConnected = false;
                        for (int i = 0; i < connections.length; i++) {
                            if (connections[i] != null
                                && (InetAddress.getByAddress(connections[i].getClientIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())) || (connections[i].getSendIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())))
                                && connections[i].getClientPort() == receivedRTPHeader.getSrcPort()) {
                                // We already have a connection established for this client
                                // Resend the synack message if the received message is not a synack
								if (!receivedRTPHeader.isACK()) {
		                            RTPPacket synackPacket;
		                            RTPHeader synackHeader = new RTPHeader(receivedRTPHeader.getDstPort(), receivedRTPHeader.getDstIP(), receivedRTPHeader.getSrcPort(), receivedRTPHeader.getSrcIP(), 0);
		                            synackHeader.setSYN(true);
		                            synackHeader.setACK(true);
									synackHeader.setRwndSize(rtp.MAX_RWND);
		                            synackPacket = new RTPPacket(synackHeader, null);
		                            synackPacket.updateChecksum();
		                            byte[] synackPacketBytes = synackPacket.getPacketByteArray();
		                            DatagramPacket sendPacket = new DatagramPacket(synackPacketBytes, synackPacketBytes.length, InetAddress.getByAddress(receivedRTPHeader.getSrcIP()), receivedRTPHeader.getSrcPort());
									for (int j = 0; j < 5; j++) {
		                            	socket.send(sendPacket);
									}
								}
                                isAlreadyConnected = true;
                            }
                        }
                        // If the client isn't already connected, establish a new connection
                        if (!isAlreadyConnected) {
    	                    // Attempt to insert the new connection into the array of connections
    	                    boolean isAccepted = false;
    	                    for (int i = 0; i < connections.length; i++) {
    	                        if (connections[i] == null) {
    	                            connections[i] = c;
    	                            isAccepted = true;
									//System.out.println("New client: " + InetAddress.getByAddress(receivedRTPHeader.getSrcIP()).toString());
    	                            break;
    	                        }
    	                    }
    	                    if (isAccepted) {
    	                        // Send SYN/ACK message to establish connection with new client
    	                        RTPPacket synackPacket;
    	                        RTPHeader synackHeader = new RTPHeader(receivedRTPHeader.getDstPort(), receivedRTPHeader.getDstIP(), receivedRTPHeader.getSrcPort(), receivedRTPHeader.getSrcIP(), 0);
    	                        synackHeader.setSYN(true);
    	                        synackHeader.setACK(true);
								synackHeader.setRwndSize(rtp.MAX_RWND);
    	                        synackPacket = new RTPPacket(synackHeader, null);
    	                        synackPacket.updateChecksum();
    	                        byte[] synackPacketBytes = synackPacket.getPacketByteArray();
    	                        DatagramPacket sendPacket = new DatagramPacket(synackPacketBytes, synackPacketBytes.length, recvPacket.getAddress(), receivedRTPHeader.getSrcPort());
    	                        socket.send(sendPacket);
    	                    } else {
    	                        // The server has reached the maximum number of connections
    	                        System.out.println("Server cannot accept any more connections at this time");
    	                    }
                        }
	
	                    // DEBUGGING
	                    int numConnected = 0;
	                    for (int i = 0; i < connections.length; i++) {
	                        if (!(connections[i] == null)) {
	                            numConnected++;
	                        }
	                    }
	                    //System.out.println(numConnected + " clients are connected");
	
	                } else if (receivedRTPHeader.isFIN() && receivedRTPHeader.isACK()) {
						// Received FINACK message, kill this thread
						isRunning = false;
					} else if (receivedRTPHeader.isACK()) {
	                    // Mark sent packet as acknowledged
	                    for (int i = 0; i < connections.length; i++) {
	                        if (connections[i] != null
	                            && (InetAddress.getByAddress(connections[i].getClientIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())) || (connections[i].getSendIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())))
	                            && connections[i].getClientPort() == receivedRTPHeader.getSrcPort()) {
	                            // Index i points to the correct connection
	                            // Mark the sent packet as acknowledged
	                            connections[i].markAcknowledged(receivedRTPPacket);     
	                        }
	                    }
					} else if (receivedRTPHeader.isFIN()) {
						// Received FIN message, null out the connection and send FINACK
						// Send FIN/ACK message to terminate connection with client
                        RTPPacket finackPacket;
                        RTPHeader finackHeader = new RTPHeader(receivedRTPHeader.getDstPort(), receivedRTPHeader.getDstIP(), receivedRTPHeader.getSrcPort(), receivedRTPHeader.getSrcIP(), 0);
                        finackHeader.setFIN(true);
                        finackHeader.setACK(true);
                        finackPacket = new RTPPacket(finackHeader, null);
                        finackPacket.updateChecksum();
                        byte[] finackPacketBytes = finackPacket.getPacketByteArray();
                        DatagramPacket sendPacket = new DatagramPacket(finackPacketBytes, finackPacketBytes.length, recvPacket.getAddress(), receivedRTPHeader.getSrcPort());
                        socket.send(sendPacket);
						for (int i = 0; i < connections.length; i++) {
	                        if (connections[i] != null
	                            && (InetAddress.getByAddress(connections[i].getClientIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())) || (connections[i].getSendIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())))
	                            && connections[i].getClientPort() == receivedRTPHeader.getSrcPort()) {
								//System.out.println("A client disconnected");
								
								connections[i] = null;
							}
						}
					} else {
	                    // Store the received packet in the receive buffer and wait for application to call receive
	                    for (int i = 0; i < connections.length; i++) {
	                        if (connections[i] != null && receivedData.length != 32
	                            && (InetAddress.getByAddress(connections[i].getClientIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())) || (connections[i].getSendIP()).equals(InetAddress.getByAddress(receivedRTPHeader.getSrcIP())))
	                            && connections[i].getClientPort() == receivedRTPHeader.getSrcPort()) {
	                            // Index i points to the correct connection
	                            // Add the packet to the receive buffer
	                            boolean isReceived = connections[i].receivePacket(receivedRTPPacket); 
	                            if (isReceived) {
	                                // Send acknowledgement
	                                RTPPacket ackPacket;
	                                RTPHeader ackHeader = new RTPHeader(receivedRTPHeader.getDstPort(), receivedRTPHeader.getDstIP(), receivedRTPHeader.getSrcPort(), receivedRTPHeader.getSrcIP(), receivedRTPHeader.getSeqNum());
	                                ackHeader.setACK(true);
	                                ackPacket = new RTPPacket(ackHeader, null);
	                                ackPacket.updateChecksum();
	                                byte[] ackPacketBytes = ackPacket.getPacketByteArray();
	                                DatagramPacket sendPacket = new DatagramPacket(ackPacketBytes, ackPacketBytes.length, recvPacket.getAddress(), receivedRTPHeader.getSrcPort());
	                                socket.send(sendPacket);
	                            }    
	                        }
	                    }
	                }
					rtp.getConnectionsLock().unlock();
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
   	}
   
   	public void start () {
      	if (t == null) {
        	t = new Thread(this, "ListenThread");
        	t.start();
      	}
   	}

}
