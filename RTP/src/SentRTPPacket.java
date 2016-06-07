public class SentRTPPacket {
	private RTPPacket p;
	private boolean isAcknowledged;
	private long initialTimeSent;
	private long timeSent;
	private long timeReceived;
	private boolean isRetransmitted;

	public SentRTPPacket(RTPPacket p) {
		this.p = p;
		this.isAcknowledged = false;
	}

	public RTPPacket getPacket() {
		return this.p;
	}

	public boolean getIsAcknowledged() {
		return this.isAcknowledged;
	}

	public long getInitialTimeSent() {
		return this.initialTimeSent;
	}

	public void setInitialTimeSent(long initialTimeSent) {
		this.initialTimeSent = initialTimeSent;
	}
	
	public long getSentTime() {
		return this.timeSent;
	}

	public void setIsAcknowledged(boolean isAcknowledged) {
		this.isAcknowledged = isAcknowledged;
	}

	public void setSentTime(long timeSent) {
		this.timeSent = timeSent;
	}

	public Long getReceivedTime() {
		return this.timeReceived;
	}

	public void setReceivedTime(Long timeReceived) {
		this.timeReceived = timeReceived;
	}

	public boolean getIsRetransmitted() {
		return this.isRetransmitted;
	}

	public void setIsRetransmitted(boolean isRetransmitted) {
		this.isRetransmitted = isRetransmitted;
	}
}
