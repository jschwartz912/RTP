import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class Connection {
	// Identifiers of the connection
	private byte[] clientIP;
	private InetAddress sendIP;
	private int clientPort;

	// Receive buffer for the connection
	private int MAX_RWND;
	private int MSS;
	private int rN;
	private RTPPacket[] recvBuffer;
	private byte[] dataBuffer;
	private int recvBase = 0;
	private int expectedSeqNum = 0;
	private Lock lockRecvBuffer;
	private Lock lockDataBuffer;

	// Send buffer for the connection
	private int MAX_SWND;
	private int sN;
	private RTPPacket[] pendingBuffer = null;
	private Lock lockPendingBuffer;
	private int pendingBase = 0;
	private int nextSeqNum = 0;
	private SentRTPPacket[] sendBuffer;
	private int sendBase = 0;
	private int sendSeqNumBase = 0;
	private Lock lockSendBuffer;

	private Lock lockTimeout = new ReentrantLock();
	private long timeout = 5000;
	private long estimatedRTT = 4000;
	private long devRTT = 0;

	public Connection(byte[] clientIP, InetAddress sendIP, int clientPort, int max_rwnd, int max_swnd, int mss) {
		this.clientIP = clientIP;
		this.sendIP = sendIP;
		this.clientPort = clientPort;
		this.MSS = mss;
		this.MAX_RWND = max_rwnd / MSS;
		recvBuffer = new RTPPacket[max_rwnd / MSS];
		this.rN = max_rwnd / MSS;
		this.MAX_SWND = max_swnd;
		this.sN = max_swnd / MSS;
		sendBuffer = new SentRTPPacket[max_swnd];
		lockRecvBuffer = new ReentrantLock();
		lockDataBuffer = new ReentrantLock();
		lockPendingBuffer = new ReentrantLock();
		lockSendBuffer = new ReentrantLock();
	}

	public byte[] getClientIP() {
		return this.clientIP;
	}

	public InetAddress getSendIP() {
		return this.sendIP;
	}

	public int getClientPort() {
		return this.clientPort;
	}
	
	public byte[] getDataBuffer(){
		return this.dataBuffer;
	}

	public void clearDataBuffer() {
		// Data buffer locked from previous function
		dataBuffer = null;
	}

	public Lock getDataBufferLock() {
		return this.lockDataBuffer;
	}

	public SentRTPPacket[] getSendBuffer() {
		return this.sendBuffer;
	}

	public Lock getSendBufferLock() {
		return this.lockSendBuffer;
	}

	public int getNextSeqNum() {
		return this.nextSeqNum;
	}

	public void setNextSeqNum(int n) {
		this.nextSeqNum = n;
	}

	public boolean receivePacket(RTPPacket p) {
		boolean isReceived = false;
		int seq = p.getHeader().getSeqNum();
		lockRecvBuffer.lock();  
		if (seq >= expectedSeqNum && seq < expectedSeqNum + 968 * rN) {  
			// Packet is within receive window, so we can accept it  
			isReceived = true;
			if (recvBuffer[(recvBase + (seq-expectedSeqNum)/968) % MAX_RWND] != null) {
				// This is a duplicate packet
				// Return true so another acknowledgement will be sent
				return true;
			}
			// Place in receive buffer
			recvBuffer[(recvBase + (seq-expectedSeqNum)/968) % MAX_RWND] = p;  
			if (seq == expectedSeqNum) {  
				appendOutputStream();  
    		}  


		}  else if (seq < expectedSeqNum) {
			// This is a retransmitted packet caused by a lost ack
			// We already have the data delivered to the application,
			// so just resend the ack
			isReceived = true;
		}
		lockRecvBuffer.unlock();
		return isReceived;
	}

	public void appendOutputStream() {
		/* recvBuffer is already locked from its caller function */
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		lockDataBuffer.lock();
		// If dataBuffer already contains data, put that data first
		if (dataBuffer != null) {
			try {
				outputStream.write(dataBuffer);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		// Iterate through the receive window until you reach a null
		// which indicates a break in the sequential link of packets
		int i = recvBase;
		for (i = recvBase; i < recvBase + rN; i++) {
			if (recvBuffer[i % MAX_RWND] != null) {
				// We can add this packet to our byte stream
				try {
					outputStream.write(recvBuffer[i % MAX_RWND].getData());
				} catch(IOException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		dataBuffer = outputStream.toByteArray();
		lockDataBuffer.unlock();
		// Update expected sequence number
		expectedSeqNum = recvBuffer[(i-1) % MAX_RWND].getHeader().getSeqNum() + recvBuffer[(i-1) % MAX_RWND].getData().length;
		// Null out the entries that were just delivered to the application  
		for (int j = recvBase; j < i; j++) {  
			recvBuffer[j % MAX_RWND] = null;  
		}  
		// Update receive base
		recvBase = i;
	}

	public void sendPackets(RTPPacket[] p) {
		// Add the packets to the pending buffer for the connection
		lockPendingBuffer.lock();
		if (pendingBuffer == null) {
			pendingBuffer = p;
			pendingBase = 0;
		} else {
			RTPPacket[] temp = new RTPPacket[pendingBuffer.length];
			temp = Arrays.copyOfRange(pendingBuffer, 0, pendingBuffer.length);
			pendingBuffer = new RTPPacket[pendingBuffer.length + p.length];
			int i = 0;
			for (i = 0; i < temp.length; i++) {
				pendingBuffer[i] = temp[i];
			}
			for (int j = 0; j < p.length; j++) {
				pendingBuffer[i + j] = p[j];
			}
		}
		lockPendingBuffer.unlock();
	}

	public void markAcknowledged(RTPPacket p) {
		lockSendBuffer.lock();
		for (int i = sendBase; i < sendBase + sN; i++) {
			if (sendBuffer[i % MAX_SWND] != null) {
				if (sendBuffer[i % MAX_SWND].getPacket().getHeader().getSeqNum() == p.getHeader().getSeqNum()) {
					sendBuffer[i % MAX_SWND].setIsAcknowledged(true);
					sendBuffer[i % MAX_SWND].setReceivedTime(System.currentTimeMillis());
					if (!sendBuffer[i % MAX_SWND].getIsRetransmitted()) {
						lockTimeout.lock();
						long sampleRTT = sendBuffer[i % MAX_SWND].getReceivedTime() - sendBuffer[i % MAX_SWND].getSentTime();
						estimatedRTT = (long) (0.875 * estimatedRTT + 0.125 * sampleRTT);
						devRTT = (long) (0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimatedRTT));
						timeout = estimatedRTT + 4 * devRTT;
						lockTimeout.unlock();
					}
					lockSendBuffer.unlock();
					return;
				}
			}
		}
		lockSendBuffer.unlock();
	}

	public void updateSendBuffer(DatagramSocket socket) {
		// Check to see if the first packet in the send buffer has been acknowledged
        // or if send buffer is not full but pending buffer has data
        lockSendBuffer.lock();
        int newSendBase = sendBase;
        for (int i = sendBase; i < sendBase + sN; i++) {
        	if (sendBuffer[i % MAX_SWND] != null) {
	        	if (sendBuffer[i % MAX_SWND].getIsAcknowledged()) {
	        		sendBuffer[i % MAX_SWND] = null;
	        		newSendBase++;
	        	} else {
	        		break;
	        	}
	        } else {
	        	break;
	        }
        }
        sendBase = newSendBase;

        for (int i = sendBase; i < sendBase + sN; i++) {
        	if (sendBuffer[i % MAX_SWND] == null) {
        		// There are empty spaces in the send buffer
        		// Check for packets in pending buffer
        		lockPendingBuffer.lock();
        		if (pendingBuffer != null && pendingBase < pendingBuffer.length && pendingBuffer[pendingBase] != null) {
        			// There is data in the pending buffer
        			// Create a sent rtp packet and store in send buffer
        			SentRTPPacket p = new SentRTPPacket(pendingBuffer[pendingBase]);
        			sendBuffer[i % MAX_SWND] = p;
        			pendingBase++;
        			// Send the packet
        			RTPPacket toSend = p.getPacket();
					byte[] toSendBytes = toSend.getPacketByteArray();
					try {
						p.setSentTime(System.currentTimeMillis());
						p.setInitialTimeSent(System.currentTimeMillis());
						DatagramPacket sendPacket = new DatagramPacket(toSendBytes, toSendBytes.length, InetAddress.getByAddress(clientIP), clientPort);
						socket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
        		}
        		lockPendingBuffer.unlock();
        	}
        }
        lockSendBuffer.unlock();
	}

	public void retransmitPackets(DatagramSocket socket) {		
		// Selective Repeat : Check for timeout at sendBase to sendBase + N
		// Only resend timed-out packets
		for (int i = sendBase; i < sendBase + sN; i++) {	
			if (sendBuffer[i % MAX_SWND] != null) {
				if (!sendBuffer[i % MAX_SWND].getIsAcknowledged()) {
					long timeSent = sendBuffer[i % MAX_SWND].getSentTime();
					long curTime = System.currentTimeMillis();
					if (curTime - timeSent > timeout) {
						try {
							RTPPacket toSend = sendBuffer[i % MAX_SWND].getPacket();
							byte[] toSendBytes = toSend.getPacketByteArray();
							DatagramPacket sendPacket = new DatagramPacket(toSendBytes, toSendBytes.length, InetAddress.getByAddress(toSend.getHeader().getDstIP()), toSend.getHeader().getDstPort());
							socket.send(sendPacket);
							sendBuffer[i % MAX_SWND].setIsRetransmitted(true);
							sendBuffer[i % MAX_SWND].setSentTime(System.currentTimeMillis());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
	        	}
			}
		}
	}
}
