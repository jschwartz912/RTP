
Your Name and email address
- Nicolette Fink (nicolettefink@gatech.edu)
- Jules Schwartz (jschwartz43@gatech.edu)

Class name, date and assignment title
- CS 3251
- April 20, 2016
- Programming Project (Part-1 and Part-2): Design and Implementation of a Reliable Transport Protocol

Names and descriptions of all files submitted
Code:
- RTP.java: The main library with the RTP functions that are accessed from the applications that use RTP.
- Connection.java: Stores the send/receive buffers between a particular connection. Contains helper methods used by RTP.
- ListenThread.java: A thread class that handles receiving and demultiplexing packets to the correct receive buffer.
- SendThread.java: A thread class that handles sending packets, updating send buffers, and retransmitting timed-out packets.
- RTPPacket.java: A class that represents a packet. Stores the data and the packet header.
- SentRTPPacket.java: A class that represents a sent packet. Stores the packet, time sent/received, and whether it has been acknowledged/retransmitted.
- RTPHeader.java: A class that stores the metadata in the packet header.
- FTAClient.java: The client file-transfer application that can request files.
- FTAServer.java: The server file-transfer application that sends files to clients.
- dbclientRTP.java: The client database management application that sends queries to the server.
- dbengineRTP.java: The server database management application that sends responses to the clients.

Other files:
- sample.txt: Contains sample run commands and output from the two applications.
- 3251.jpg: A sample image that can be transmitted using the file-transfer application.

Detailed instructions for compiling and running your client and server programs
Compiling:
$ javac *.java

Running:
FTAClient: java FTAClient <server IP>:<server port #> <receive window size (in bytes)>
FTAServer: java FTAServer <port #> <receive window size (in bytes)>
dbclientRTP: java dbengineRTP <port #>
dbengineRTP: java dbclientRTP <server IP>:<server port #> <student ID> <field 1> <...additional fields...>
* For examples, see sample.txt.

Any known bugs or limitations of your program
- The server for the file-transfer application expects a valid file name.
- The timeout has a max value of 5 seconds (because for the networklabs with excessive packet loss, the timeout increases exponentially if there is no max value)
