package tools.data.input;

import lombok.extern.log4j.Log4j2;

import java.awt.*;

/**
 * Provides a generic interface to a Little Endian stream of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
@Log4j2
public class GenericLittleEndianAccessor implements LittleEndianAccessor {

    private final ByteInputStream bs;

    /**
     * Class constructor - Wraps the accessor around a stream of bytes.
     *
     * @param bs The byte stream to wrap the accessor around.
     */
    public GenericLittleEndianAccessor(ByteInputStream bs) {
        this.bs = bs;
    }

    @Override
    public int readByteAsInt() {
        return bs.readByte();
    }

    /**
     * Read a single byte from the stream.
     *
     * @return The byte read.
     * @see tools.data.input.ByteInputStream#readByte
     */
    @Override
    public byte readByte() {
        return (byte) bs.readByte();
    }

    /**
     * Reads an integer from the stream.
     *
     * @return The integer read.
     */
    @Override
    public int readInt() {
        int byte1 = bs.readByte();
        int byte2 = bs.readByte();
        int byte3 = bs.readByte();
        int byte4 = bs.readByte();
        return (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    /**
     * Reads a short integer from the stream.
     *
     * @return The short read.
     */
    @Override
    public short readShort() {
        int byte1 = bs.readByte();
        int byte2 = bs.readByte();
        return (short) ((byte2 << 8) + byte1);
    }

    /**
     * Reads a short integer from the stream.
     *
     * @return The short read.
     */
    @Override
    public int readUShort() {
        int quest = readShort();
        if (quest < 0) { //questid 50000 and above, WILL cast to negative, this was tested.
            quest += 65536; //probably not the best fix, but whatever
        }
        return quest;
    }

    /**
     * Reads a single character from the stream.
     *
     * @return The character read.
     */
    @Override
    public char readChar() {
        return (char) readShort();
    }

    /**
     * Reads a long integer from the stream.
     *
     * @return The long integer read.
     */
    @Override
    public long readLong() {
        long byte1 = bs.readByte();
        long byte2 = bs.readByte();
        long byte3 = bs.readByte();
        long byte4 = bs.readByte();
        long byte5 = bs.readByte();
        long byte6 = bs.readByte();
        long byte7 = bs.readByte();
        long byte8 = bs.readByte();
        return (byte8 << 56) + (byte7 << 48) + (byte6 << 40) + (byte5 << 32) + (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    /**
     * Reads a floating point integer from the stream.
     *
     * @return The float-type integer read.
     */
    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads a double-precision integer from the stream.
     *
     * @return The double-type integer read.
     */
    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads an ASCII string from the stream with length
     * <code>n</code>.
     *
     * @param n Number of characters to read.
     * @return The string read.
     */
    @Override
    public String readAsciiString(int n) {
        byte ret[] = new byte[n];
        for (int x = 0; x < n; x++) {
            ret[x] = readByte();
        }
        try {
            return new String(ret, "gbk");
        } catch (Exception e) {
            log.error("readAsciiString", e);
        }
        return "";
    }

    /**
     * Gets the number of bytes read from the stream so far.
     *
     * @return A long integer representing the number of bytes read.
     * @see tools.data.input.ByteInputStream#getBytesRead()
     */
    @Override
    public long getBytesRead() {
        return bs.getBytesRead();
    }

    /**
     * Reads a MapleStory convention lengthed ASCII string. This consists of a
     * short integer telling the length of the string, then the string itself.
     *
     * @return The string read.
     */
    @Override
    public String readMapleAsciiString() {
        return readAsciiString(readShort());
    }

    /**
     * Reads a MapleStory Position information. This consists of 2 short
     * integer.
     *
     * @return The Position read.
     */
    @Override
    public Point readPos() {
        int x = readShort();
        int y = readShort();
        return new Point(x, y);
    }

    /**
     * Reads
     * <code>num</code> bytes off the stream.
     *
     * @param num The number of bytes to read.
     * @return An array of bytes with the length of <code>num</code>
     */
    @Override
    public byte[] read(int num) {
        byte[] ret = new byte[num];
        for (int x = 0; x < num; x++) {
            ret[x] = readByte();
        }
        return ret;
    }

    /**
     * Skips the current position of the stream
     * <code>num</code> bytes ahead.
     *
     * @param num Number of bytes to skip.
     */
    @Override
    public void skip(int num) {
        for (int x = 0; x < num; x++) {
            readByte();
        }
    }

    /**
     * @see tools.data.input.ByteInputStream#available
     */
    @Override
    public long available() {
        return bs.available();
    }

    /**
     * @see java.lang.Object#toString
     */
    @Override
    public String toString() {
        return bs.toString();
    }

    @Override
    public String toString(boolean b) {
        return bs.toString(b);
    }
}
