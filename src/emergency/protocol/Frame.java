package emergency.protocol;

/**
 * Represents an individual physical/data-link layer frame protected by CRC-8.
 */
public final class Frame {
    public final int seqNum;
    public final int messageId;
    public final byte[] payload;
    public final int crc;
    public boolean corrupted = false;
    public boolean acknowledged = false;
    public int attempts = 0;

    public Frame(int seqNum, int messageId, byte[] payload) {
        this.seqNum = seqNum;
        this.messageId = messageId;
        this.payload = payload != null ? payload.clone() : new byte[0];
        this.crc = Crc8.compute(this.payload);
    }

    public boolean isValid() {
        return !corrupted && Crc8.verify(payload, crc);
    }

    @Override
    public String toString() {
        return String.format("Frame #%d [Msg %d, CRC: %s, Attempts: %d, Ack: %b]",
                seqNum, messageId, Crc8.toHex(crc), attempts, acknowledged);
    }
}
