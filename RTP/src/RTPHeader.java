import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

public class RTPHeader {
	private int srcPort;
	private byte[] srcIP = new byte[4];
	private int dstPort;
	private byte[] dstIP = new byte[4];
	private int seqNum;
	private int rwndSize;
	private int checksum;
	private boolean ack;
	private boolean syn;
	private boolean fin;

	public RTPHeader() {
		this.rwndSize = 0;
		this.checksum = 0;
		this.ack = false;
		this.syn = false;
		this.fin = false;
	}

	public RTPHeader(int srcPort, byte[] srcIP, int dstPort, byte[] dstIP, int seqNum) {
		this();
		this.srcPort = srcPort;
		this.srcIP = srcIP;
		this.dstPort = dstPort;
		this.dstIP = dstIP;
		this.seqNum = seqNum;
	}

	public RTPHeader(byte[] headerByteArray) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(headerByteArray);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		this.srcPort = intBuffer.get(0);
		this.srcIP[0] = (byte) ((intBuffer.get(1) >> 24) & 0xFF);
		this.srcIP[1] = (byte) ((intBuffer.get(1) >> 16) & 0xFF);
		this.srcIP[2] = (byte) ((intBuffer.get(1) >> 8) & 0xFF);
		this.srcIP[3] = (byte) (intBuffer.get(1) & 0xFF);
		this.dstPort = intBuffer.get(2);
		this.dstIP[0] = (byte) ((intBuffer.get(3) >> 24) & 0xFF);
		this.dstIP[1] = (byte) ((intBuffer.get(3) >> 16) & 0xFF);
		this.dstIP[2] = (byte) ((intBuffer.get(3) >> 8) & 0xFF);
		this.dstIP[3] = (byte) (intBuffer.get(3) & 0xFF);
		this.seqNum = intBuffer.get(4);
		this.rwndSize = intBuffer.get(5);
		this.checksum = intBuffer.get(6);
		int flags = intBuffer.get(7);

		this.ack = ((flags >>> 31) & 0x1) != 0;
		this.syn = ((flags >>> 30) & 0x1) != 0;
		this.fin = ((flags >>> 29) & 0x1) != 0;
	}

	public int getSrcPort() {
		return this.srcPort;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public byte[] getSrcIP() {
		return this.srcIP;
	}

	public void setSrcIP(byte[] srcIP) {
		this.srcIP = srcIP;
	}

	public int getDstPort() {
		return this.dstPort;
	}

	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}

	public byte[] getDstIP() {
		return this.dstIP;
	}

	public void setDstIP(byte[] dstIP) {
		this.dstIP = dstIP;
	}

	public int getSeqNum() {
		return this.seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	public int getRwndSize() {
		return this.rwndSize;
	}

	public void setRwndSize(int rwndSize) {
		this.rwndSize = rwndSize;
	}

	public int getChecksum() {
		return this.checksum;
	}

	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}

	public boolean isACK() {
		return this.ack;
	}

	public void setACK(boolean ack) {
		this.ack = ack;
	}

	public boolean isSYN() {
		return this.syn;
	}

	public void setSYN(boolean syn) {
		this.syn = syn;
	}

	public boolean isFIN() {
		return this.fin;
	}

	public void setFIN(boolean fin) {
		this.fin = fin;
	}

	public byte[] getHeaderByteArray() {
		byte[] headerByteArray;
		ByteBuffer byteBuffer = ByteBuffer.allocate(32);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putInt(srcPort);
		byteBuffer.putInt(srcIP[0] << 24 | srcIP[1] << 16 | srcIP[2] << 8 | srcIP[3]);
		byteBuffer.putInt(dstPort);
		byteBuffer.putInt(dstIP[0] << 24 | dstIP[1] << 16 | dstIP[2] << 8 | dstIP[3]);
		byteBuffer.putInt(seqNum);
		byteBuffer.putInt(rwndSize);
		byteBuffer.putInt(checksum);

		int flags = ((ack ? 1 : 0) << 31)
			| ((syn ? 1 : 0) << 30)
			| ((fin ? 1 : 0) << 29);
		byteBuffer.putInt(flags);

		headerByteArray = byteBuffer.array();
		return headerByteArray;
	}

}