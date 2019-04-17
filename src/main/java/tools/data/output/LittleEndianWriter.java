package tools.data.output;

import java.awt.*;

/**
 * Provides an interface to a writer class that writes a little-endian sequence
 * of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public interface LittleEndianWriter {

    /**
     * Write the number of zero bytes
     *
     * @param i The bytes to write.
     */
    void writeZeroBytes(int i);

    /**
     * Write an array of bytes to the sequence.
     *
     * @param b The bytes to write.
     */
    void write(byte b[]);

    /**
     * Write a byte to the sequence.
     *
     * @param b The byte to write.
     */
    void write(byte b);

    /**
     * Write a byte in integer form to the sequence.
     *
     * @param b The byte as an Integer to write.
     */
    void write(int b);

    /**
     * Writes an integer to the sequence.
     *
     * @param i The integer to write.
     */
    void writeInt(int i);

    void writeReversedInt(long l);

    /**
     * Write a short integer to the sequence.
     *
     * @param s The short integer to write.
     */
    void writeShort(short s);

    /**
     * Write a int integer to the sequence.
     *
     * @param i The int integer to write.
     */
    void writeShort(int i);

    /**
     * Write a long integer to the sequence.
     *
     * @param l The long integer to write.
     */
    void writeLong(long l);

    void writeReversedLong(long l);

    /**
     * Writes an ASCII string the the sequence.
     *
     * @param s The ASCII string to write.
     */
    void writeAsciiString(String s);

    /**
     * Writes a null-terminated ASCII string to the sequence.
     *
     * @param s   The ASCII string to write.
     * @param max
     */
    void writeAsciiString(String s, int max);

    /**
     * Writes a Maple Name ASCII string to the sequence.
     *
     * @param s The ASCII string to write.
     */
    void writeMapleNameString(String s);

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s The Point position to write.
     */
    void writePos(Point s);

    /**
     * Writes a 4 int 16 byte Rectangle information
     *
     * @param s The Rectangle to write.
     */
    void writeRect(Rectangle s);

    /**
     * Writes a maple-convention ASCII string to the sequence.
     *
     * @param s The ASCII string to use maple-convention to write.
     */
    void writeMapleAsciiString(String s);

    void writeMapleAsciiString(String s, int max);

    /**
     * 写入布尔值 true ? 1 : 0
     *
     * @param b The boolean to write.
     */
    void writeBool(boolean b);

    /**
     * 写入反向布尔值 true ? 0 : 1
     *
     * @param b The boolean to write.
     */
    void writeReversedBool(boolean b);

    void writeHexString(String s);
}
