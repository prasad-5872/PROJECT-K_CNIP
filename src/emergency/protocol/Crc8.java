package emergency.protocol;

import java.nio.charset.StandardCharsets;

/**
 * CRC-8 implementation using standard polynomial 0x07 (x^8 + x^2 + x + 1).
 * Commonly deployed in ATM, I2C, and tactical sensor networks for frame integrity.
 */
public final class Crc8 {
    public static final int POLYNOMIAL = 0x07;

    private Crc8() {}

    /**
     * Computes the CRC-8 checksum of a byte array using polynomial 0x07.
     */
    public static int compute(byte[] data) {
        if (data == null) return 0;
        int crc = 0x00;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = ((crc << 1) ^ POLYNOMIAL);
                } else {
                    crc <<= 1;
                }
                crc &= 0xFF;
            }
        }
        return crc & 0xFF;
    }

    /**
     * Computes CRC-8 for a UTF-8 string payload.
     */
    public static int compute(String payload) {
        if (payload == null) return 0;
        return compute(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates whether the received CRC matches the data payload.
     */
    public static boolean verify(byte[] data, int receivedCrc) {
        return compute(data) == (receivedCrc & 0xFF);
    }

    /**
     * Validates string payload against expected CRC.
     */
    public static boolean verify(String payload, int receivedCrc) {
        return compute(payload) == (receivedCrc & 0xFF);
    }

    /**
     * Creates a mutated copy of the data with one flipped bit at the specified byte and bit position.
     */
    public static byte[] flipBit(byte[] data, int byteIndex, int bitIndex) {
        if (data == null || data.length == 0) return new byte[0];
        byte[] copy = data.clone();
        int safeByte = Math.max(0, Math.min(byteIndex, copy.length - 1));
        int safeBit = Math.max(0, Math.min(bitIndex, 7));
        copy[safeByte] ^= (1 << safeBit);
        return copy;
    }

    /**
     * Formats integer as a 2-digit uppercase hex string (e.g. 0x07, 0xA3).
     */
    public static String toHex(int val) {
        return String.format("0x%02X", val & 0xFF);
    }

    /**
     * Generates a step-by-step bitwise execution trace of polynomial division for educational display.
     */
    public static String generateTrace(byte[] data) {
        if (data == null || data.length == 0) {
            return "Payload is empty. CRC-8 = 0x00";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== CRC-8 Calculation Trace (Polynomial: 0x07 = x^8 + x^2 + x + 1) ===\n");
        sb.append("Initial CRC Register: 0x00\n");

        int crc = 0x00;
        for (int byteIdx = 0; byteIdx < data.length; byteIdx++) {
            byte b = data[byteIdx];
            sb.append(String.format("Byte [%d] = 0x%02X ('%c'):\n", byteIdx, (b & 0xFF), (b >= 32 && b <= 126) ? (char)b : '?'));
            crc ^= (b & 0xFF);
            sb.append(String.format("  XOR with accumulator -> 0x%02X\n", crc));

            for (int bit = 7; bit >= 0; bit--) {
                boolean msb = (crc & 0x80) != 0;
                crc = (crc << 1) & 0xFF;
                if (msb) {
                    crc ^= POLYNOMIAL;
                    crc &= 0xFF;
                    sb.append(String.format("    Bit %d: MSB=1 -> Shift & XOR with 0x07 -> 0x%02X\n", 7 - bit, crc));
                } else {
                    sb.append(String.format("    Bit %d: MSB=0 -> Shift only -> 0x%02X\n", 7 - bit, crc));
                }
            }
        }
        sb.append("Final Computed CRC-8: ").append(toHex(crc)).append("\n");
        return sb.toString();
    }
}
