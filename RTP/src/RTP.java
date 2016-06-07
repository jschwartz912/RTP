import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Arrays;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class RTP {

	// Maximum Segment Size (including header)
	public int MSS = 1000;

	// Connections
	public int MAX_CONNECTIONS = 10;
	private Connection[] connections = new Connection[MAX_CONNECTIONS];
	private Lock lockConnections;
	public int MAX_RWND;
	private int lastReceivedIndex = 0;

	private SendThread sendThread;
	private ListenThread listenThread;

	public RTP(DatagramSocket socket, int max_rwnd) {
		this.MAX_RWND = max_rwnd;
		lockConnections = new ReentrantLock();
		// Start send thread
		try {
			sendThread = new SendThread(this,socket);
			sendThread.start();
			sendThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Lock getConnectionsLock() {
		return this.lockConnections;
	}

	public int connect(DatagramSocket socket, int srcPort, int dstPort, InetAddress serverAddress) throws IOException {
		// Send SYN message to server
		RTPPacket synPacket;
		RTPHeader synHeader = new RTPHeader(srcPort, /*InetAddress.getByName("127.0.0.1").getAddress()*/InetAddress.getLocalHost().getAddress(), dstPort, serverAddress.getAddress(), 0);
		synHeader.setSYN(true);
		synHeader.setRwndSize(MAX_RWND);
		synPacket = new RTPPacket(synHeader, null);
		synPacket.updateChecksum();
		byte[] synPacketBytes = synPacket.getPacketByteArray();
		DatagramPacket sendPacket = new DatagramPacket(synPacketBytes, synPacketBytes.length, serverAddress, dstPort);
		//socket.send(sendPacket);

		socket.setSoTimeout(5000);

		// Receive SYN/ACK message from server
		DatagramPacket recvPacket = new DatagramPacket(new byte[MSS], MSS);
		boolean isReceivedSynack = false;
		while (!isReceivedSynack) {
            try {
				//System.out.println("Attempting to connect");
				socket.send(sendPacket);
				socket.receive(recvPacket);
				isReceivedSynack = true;
            } catch (SocketTimeoutException e) {
            	// Resend the syn packet

            }
        }

        socket.setSoTimeout(0);

		byte[] receivedData = new byte[recvPacket.getLength()];
		receivedData = Arrays.copyOfRange(recvPacket.getData(), 0, recvPacket.getLength());
		RTPPacket receivedRTPPacket = new RTPPacket(receivedData);
		RTPHeader receivedRTPHeader = receivedRTPPacket.getHeader();

		// Send ACK message to server
		if(receivedRTPHeader.isSYN() && receivedRTPHeader.isACK()) {
			RTPPacket ackPacket;
			RTPHeader ackHeader = new RTPHeader(srcPort, receivedRTPHeader.getDstIP(), dstPort, serverAddress.getAddress(), 0);
			ackHeader.setACK(true);
			ackPacket = new RTPPacket(ackHeader, null);
			ackPacket.updateChecksum();
			byte[] ackPacketBytes = ackPacket.getPacketByteArray();
			sendPacket = new DatagramPacket(ackPacketBytes, ackPacketBytes.length, serverAddress, dstPort);
			socket.send(sendPacket);
		}
		lockConnections.lock();
		// Create a new connection and initialize the receive buffer
        Connection c = new Connection(receivedRTPHeader.getSrcIP(), serverAddress, receivedRTPHeader.getSrcPort(), MAX_RWND, receivedRTPHeader.getRwndSize(), MSS);
        // Attempt to insert the new connection into the array of connections
        boolean isAccepted = false;
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] == null) {
                connections[i] = c;
                isAccepted = true;
                break;
            }
        }

        // TO-DO: return some kind of error if there was no room for another connection (ie, isAccepted == false)
        
        lockConnections.unlock();
		//System.out.println("Successfully connected to server");
		return 0; // success
	}

	public void listen(DatagramSocket socket) {
		try {
			listenThread = new ListenThread(this,socket);
			listenThread.start();
			listenThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void send(DatagramSocket socket, int srcPort, int dstPort, InetAddress dstAddress, byte[] data) throws IOException { 
 		for (int i = 0; i < connections.length; i++) {
            if (connections[i] != null
                && connections[i].getSendIP().equals(dstAddress)
                && connections[i].getClientPort() == dstPort) {
				//System.out.println("Sending " + data.length + " bytes of data to" + dstAddress.toString());
            	RTPPacket[] packets = packetize(srcPort, dstPort, dstAddress, data, connections[i].getNextSeqNum());
            	connections[i].sendPackets(packets);
            	// Update the next sequence number for the pending buffer
            	connections[i].setNextSeqNum(connections[i].getNextSeqNum() + data.length);
          	}
        }
 	} 

 	public RTPPacket[] packetize(int srcPort, int dstPort, InetAddress dstAddress, byte[] data, int nextSeqNum) {
 		RTPPacket[] packets = new RTPPacket[(data.length % 968 == 0) ? (data.length / 968) : ((data.length / 968) + 1)];
 		int packetNum = 0;
 		for (int i = 0; i < data.length; i+=968) {
 			try {
				RTPHeader header = new RTPHeader(srcPort, /*InetAddress.getByName("127.0.0.1").getAddress()*/InetAddress.getLocalHost().getAddress(), dstPort, dstAddress.getAddress(), nextSeqNum + i);
				int endSeqNum = ((i + 968) > (data.length)) ? data.length : i + 968;
				RTPPacket packet = new RTPPacket(header, Arrays.copyOfRange(data, i, endSeqNum));
				packet.updateChecksum();
				packets[packetNum] = packet;
				packetNum++;
			} catch (IOException e) {
				e.printStackTrace();
			}
 		}
 		return packets;
 	}

 	public byte[] receive(int i) {
 		Connection c = connections[i % MAX_CONNECTIONS];
 		byte[] data = null;
		if (c != null) {
			// Check to see if it has data
			c.getDataBufferLock().lock();
			if (c.getDataBuffer() != null) {
				data = Arrays.copyOfRange(c.getDataBuffer(), 0, c.getDataBuffer().length);
				c.clearDataBuffer();
			}
			c.getDataBufferLock().unlock();
		}
		return data;
 	}
	
 	public Connection[] getConnections() {
 		return this.connections;
 	}

	public void endConnection(DatagramSocket socket, int srcPort, int dstPort, InetAddress serverAddress) {
		while (listenThread.getIsRunning()) {
			try {
				RTPPacket[] packets = new RTPPacket[1];
				// Send FIN message
				RTPPacket finPacket;
				RTPHeader finHeader = new RTPHeader(srcPort, /*InetAddress.getByName("127.0.0.1").getAddress()*/InetAddress.getLocalHost().getAddress(), dstPort, serverAddress.getAddress(), 0);
				finHeader.setFIN(true);
				finPacket = new RTPPacket(finHeader, null);
				finPacket.updateChecksum();
				packets[0] = finPacket;
				connections[0].sendPackets(packets);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		sendThread.close();
	}
}
