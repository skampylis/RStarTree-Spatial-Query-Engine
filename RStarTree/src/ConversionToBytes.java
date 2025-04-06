import java.nio.ByteBuffer;

/**
 * This is a helper class that provides methods to convert different data types to bytes. The class includes methods to
 * convert a long, double, character, and integer to a byte array. Each method creates a ByteBuffer with the capacity of
 * the respective data type, puts the value into the buffer, and returns the byte array representation of the value.
 */
public class ConversionToBytes {

    /**
     * This method converts a long to a byte array. It first creates a ByteBuffer with the capacity of a long (8 bytes).
     * It then puts the long value into the buffer. Finally, it returns the byte array representation of the long value.
     *
     * @param x the long value to be converted
     * @return the byte array representation of the long value
     */
    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * This method converts a double to a byte array. It first creates a ByteBuffer with the capacity of a double (8
     * bytes). It then puts the double value into the buffer. Finally, it returns the byte array representation of the
     * double value.
     *
     * @param x the double value to be converted
     * @return the byte array representation of the double value
     */
    public static byte[] doubleToBytes(double x) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(x);
        return buffer.array();
    }

    /**
     * This method converts a character to a byte array. It first creates a ByteBuffer with the capacity of a character
     * (2 bytes). It then puts the character value into the buffer. Finally, it returns the byte array representation of
     * the character value.
     *
     * @param x the character value to be converted
     * @return the byte array representation of the character value
     */
    public static byte[] charToBytes(Character x) {
        ByteBuffer buffer = ByteBuffer.allocate(Character.BYTES);
        buffer.putChar(x);
        return buffer.array();
    }

    /**
     * This method converts an integer to a byte array. It first creates a ByteBuffer with the capacity of an integer (4
     * bytes). It then puts the integer value into the buffer. Finally, it returns the byte array representation of the
     * integer value.
     *
     * @param x the integer value to be converted
     * @return the byte array representation of the integer value
     */
    public static byte[] intToBytes(Integer x) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(x);
        return buffer.array();
    }

}