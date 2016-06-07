import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class RTPPacket {
	private RTPHeader header;
	private byte[] data;

	public RTPPacket(int srcPort, byte[] srcIP, int dstPort, byte[] dstIP, byte[] data) {
		this.header = new RTPHeader(srcPort, srcIP, dstPort, dstIP, 0);
		this.data = data;
	}

	public RTPPacket(int srcPort, byte[] srcIP, int dstPort, byte[] dstIP) {
		this.header = new RTPHeader(srcPort, srcIP, dstPort, dstIP, 0);
	}

	public RTPPacket(RTPHeader header, byte[] data) {
		this.header = header;
		this.data = data;
	}

	public RTPPacket(byte[] packetByteArray) {
		byte[] headerBytes = Arrays.copyOfRange(packetByteArray, 0, 32);
		this.header = new RTPHeader(headerBytes);

		if (packetByteArray.length > 32) {
			byte[] dataBytes = Arrays.copyOfRange(packetByteArray, 32, packetByteArray.length);
			this.data = dataBytes;
		}
	}

	public byte[] getPacketByteArray() {
		byte[] packetByteArray;
		byte[] headerByteArray;

		headerByteArray = header.getHeaderByteArray();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(headerByteArray);
			if (data != null) {
				outputStream.write(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		packetByteArray = outputStream.toByteArray();
		return packetByteArray;
	}

	public void updateChecksum() {
		CRC32 checksum = new CRC32();
		byte[] packetByteArray = getPacketByteArray();
		packetByteArray[24] = 0x0;
		packetByteArray[25] = 0x0;
		packetByteArray[26] = 0x0;
		packetByteArray[27] = 0x0;
		checksum.update(packetByteArray);
		header.setChecksum((int) checksum.getValue());
	}

	public RTPHeader getHeader() {
		return this.header;
	}

	public byte[] getData() {
		return this.data;
	}
}