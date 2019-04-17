package tools.data.output;

import tools.HexTool;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Provides a generic writer of a little-endian sequence of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class GenericLittleEndianWriter implements LittleEndianWriter {

    private static final Charset ASCII = Charset.forName("GBK"); // ISO-8859-1, UTF-8
    private ByteOutputStream bos;

    /**
     * Class constructor - Protected to prevent instantiation with no arguments.
     */
    protected GenericLittleEndianWriter() {
        // Blah!
    }

    /**
     * Class constructor - only this one can be used.
     *
     * @param bos The stream to wrap this objecr around.
     */
    public GenericLittleEndianWriter(ByteOutputStream bos) {
        this.bos = bos;
    }

    /**
     * Sets the byte-output stream for this instance of the object.
     *
     * @param bos The new output stream to set.
     */
    protected void setByteOutputStream(ByteOutputStream bos) {
        this.bos = bos;
    }

    /**
     * Write the number of zero bytes
     */
    @Override
    public final void writeZeroBytes(final int i) {
        for (int x = 0; x < i; x++) {
            bos.writeByte((byte) 0);
        }
    }

    /**
     * Write an array of bytes to the stream.
     *
     * @param b The bytes to write.
     */
    @Override
    public final void write(final byte[] b) {
        for (byte aB : b) {
            bos.writeByte(aB);
        }
    }

    /**
     * Write a byte to the stream.
     *
     * @param b The byte to write.
     */
    @Override
    public final void write(final byte b) {
        bos.writeByte(b);
    }

    /**
     * Write a byte in integer form to the sequence.
     *
     * @param b The byte as an Integer to write.
     */
    @Override
    public final void write(final int b) {
        bos.writeByte((byte) b);
    }

    /**
     * Write a short integer to the stream.
     *
     * @param i The short integer to write.
     */
    @Override
    public final void writeShort(final short i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
    }

    /**
     * Write a int integer to the sequence.
     *
     * @param i The int integer to write.
     */
    @Override
    public final void writeShort(final int i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
    }

    /**
     * Writes an integer to the stream.
     *
     * @param i The integer to write.
     */
    @Override
    public final void writeInt(final int i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
        bos.writeByte((byte) ((i >>> 16) & 0xFF));
        bos.writeByte((byte) ((i >>> 24) & 0xFF));
    }

    @Override
    public final void writeReversedInt(final long l) {
        bos.writeByte((byte) ((l >>> 32) & 0xFF));
        bos.writeByte((byte) ((l >>> 40) & 0xFF));
        bos.writeByte((byte) ((l >>> 48) & 0xFF));
        bos.writeByte((byte) ((l >>> 56) & 0xFF));
    }

    /**
     * Writes an ASCII string the the stream.
     *
     * @param s The ASCII string to write.
     */
    @Override
    public final void writeAsciiString(final String s) {
        write(s.getBytes(ASCII));
    }

    /**
     * Writes a null-terminated ASCII string to the sequence.
     *
     * @param s   The ASCII string to write.
     * @param max
     */
    @Override
    public final void writeAsciiString(final String s, final int max) {
        write(s.getBytes(ASCII));
        for (int i = s.getBytes(ASCII).length; i < max; i++) {
            write(0);
        }
    }

    /**
     * Writes a Maple Name ASCII string to the sequence.
     *
     * @param s The ASCII string to write.
     */
    @Override
    public final void writeMapleNameString(String s) {
        if (s.getBytes().length > 13) {
            s = s.substring(0, 13);
        }
        writeAsciiString(s);
        for (int x = s.getBytes().length; x < 13; x++) {
            write(0);
        }
    }

    /**
     * Writes a maple-convention ASCII string to the stream.
     *
     * @param s The ASCII string to use maple-convention to write.
     */
    @Override
    public final void writeMapleAsciiString(final String s) {
        if (s == null) {
            writeShort(0);
            return;
        }
        writeShort((short) s.getBytes(ASCII).length);
        writeAsciiString(s);
    }

    @Override
    public final void writeMapleAsciiString(String s, final int max) {
        writeShort((short) max);
        if (s.getBytes().length > max) {
            s = HexTool.getSubstring(s, 1, max);
        }
        writeAsciiString(s);
        for (int x = s.getBytes(ASCII).length; x < max; x++) {
            write(0);
        }
    }

    public void writeMapleAsciiString(String[] arrstring) {
        int n2 = 0;
        for (String string : arrstring) {
            if (string != null) {
                n2 += string.getBytes(ASCII).length;
            }
        }
        if (n2 < 1) {
            writeShort(0);
            return;
        }
        writeShort((short) (n2 + arrstring.length - 1));
        for (int i = 0; i < arrstring.length; ++i) {
            if (arrstring[i] != null) {
                writeAsciiString(arrstring[i]);
            }
            if (i < arrstring.length - 1) {
                write(0);
            }
        }
    }

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s The Point position to write.
     */
    @Override
    public final void writePos(final Point s) {
        writeShort(s.x);
        writeShort(s.y);
    }

    /**
     * Writes a 4 int 16 byte Rectangle information
     *
     * @param s The Rectangle to write.
     */
    @Override
    public final void writeRect(final Rectangle s) {
        writeInt(s.x);
        writeInt(s.y);
        writeInt(s.x + s.width);
        writeInt(s.y + s.height);
    }

    /**
     * Write a long integer to the stream.
     *
     * @param l The long integer to write.
     */
    @Override
    public final void writeLong(final long l) {
        bos.writeByte((byte) (l & 0xFF));
        bos.writeByte((byte) ((l >>> 8) & 0xFF));
        bos.writeByte((byte) ((l >>> 16) & 0xFF));
        bos.writeByte((byte) ((l >>> 24) & 0xFF));
        bos.writeByte((byte) ((l >>> 32) & 0xFF));
        bos.writeByte((byte) ((l >>> 40) & 0xFF));
        bos.writeByte((byte) ((l >>> 48) & 0xFF));
        bos.writeByte((byte) ((l >>> 56) & 0xFF));
    }

    @Override
    public final void writeReversedLong(final long l) {
        bos.writeByte((byte) ((l >>> 32) & 0xFF));
        bos.writeByte((byte) ((l >>> 40) & 0xFF));
        bos.writeByte((byte) ((l >>> 48) & 0xFF));
        bos.writeByte((byte) ((l >>> 56) & 0xFF));
        bos.writeByte((byte) (l & 0xFF));
        bos.writeByte((byte) ((l >>> 8) & 0xFF));
        bos.writeByte((byte) ((l >>> 16) & 0xFF));
        bos.writeByte((byte) ((l >>> 24) & 0xFF));
    }

    /**
     * 写入布尔值 true ? 1 : 0
     *
     * @param b The boolean to write.
     */
    @Override
    public final void writeBool(final boolean b) {
        write(b ? 1 : 0);
    }

    /**
     * 写入反向布尔值 true ? 0 : 1
     *
     * @param b The boolean to write.
     */
    @Override
    public final void writeReversedBool(final boolean b) {
        write(b ? 0 : 1);
    }

    @Override
    public final void writeHexString(final String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int nexti = 0;
        int nextb = 0;
        boolean highoc = true;
        outer:
        for (; ; ) {
            int number = -1;
            while (number == -1) {
                if (nexti == s.length()) {
                    break outer;
                }
                char chr = s.charAt(nexti);
                if (chr >= '0' && chr <= '9') {
                    number = chr - '0';
                } else if (chr >= 'a' && chr <= 'f') {
                    number = chr - 'a' + 10;
                } else if (chr >= 'A' && chr <= 'F') {
                    number = chr - 'A' + 10;
                } else {
                    number = -1;
                }
                nexti++;
            }
            if (highoc) {
                nextb = number << 4;
                highoc = false;
            } else {
                nextb |= number;
                highoc = true;
                baos.write(nextb);
            }
        }
        write(baos.toByteArray());
    }
}
