import java.net.DatagramSocket;
import java.io.IOException;

public class SendThread extends Thread {
	private Thread t;
   	private RTP rtp;
   	private DatagramSocket socket;
	private boolean isRunning;
   
   	public SendThread(RTP rtp, DatagramSocket socket) {
       this.rtp = rtp;
       this.socket = socket;
		isRunning = true;
   	}

	public void close() {
		isRunning = false;
	}

   	public void run() {
        while (isRunning) {
            // Iterate through all of the connections
            for (int i = 0; i < rtp.MAX_CONNECTIONS; i++) {
                rtp.getConnectionsLock().lock();
                Connection[] connections = rtp.getConnections();
                if (connections[i] != null) {
                    connections[i].updateSendBuffer(socket);
                    connections[i].retransmitPackets(socket);
                }
                rtp.getConnectionsLock().unlock();
				if (!isRunning) {
					//System.out.println("Ending send thread");
					return;
				}
            }
        }
   	}
   
   	public void start () {
      	if (t == null) {
        	t = new Thread (this, "SendThread");
        	t.start();
      	}
   	}
}
