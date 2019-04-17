package tools.data.input;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Provides an abstract layer to a byte stream. This layer can be accessed
 * randomly.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class RandomAccessByteStream implements SeekableInputStreamBytestream {

    private final RandomAccessFile raf;
    private long read = 0;

    /**
     * Class constructor. Wraps this object around a RandomAccessFile.
     *
     * @param raf The RandomAccessFile instance to wrap this around.
     * @see java.io.RandomAccessFile
     */
    public RandomAccessByteStream(RandomAccessFile raf) {
        super();
        this.raf = raf;
    }

    /**
     * Reads a byte off of the file.
     *
     * @return The byte read as an integer.
     */
    @Override
    public int readByte() {
        int temp;
        try {
            temp = raf.read();
            if (temp == -1) {
                throw new RuntimeException("EOF");
            }
            read++;
            return temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws java.io.IOException
     * @see tools.data.input.SeekableInputStreamBytestream#seek(long)
     */
    @Override
    public void seek(long offset) throws IOException {
        raf.seek(offset);
    }

    /**
     * @throws java.io.IOException
     * @see tools.data.input.SeekableInputStreamBytestream#getPosition()
     */
    @Override
    public long getPosition() throws IOException {
        return raf.getFilePointer();
    }

    /**
     * Get the number of bytes read.
     *
     * @return The number of bytes read as a long integer.
     */
    @Override
    public long getBytesRead() {
        return read;
    }

    /**
     * Get the number of bytes available for reading.
     *
     * @return The number of bytes available for reading as a long integer.
     */
    @Override
    public long available() {
        try {
            return raf.length() - raf.getFilePointer();
        } catch (IOException e) {
            System.err.println("ERROR" + e);
            return 0;
        }
    }

    @Override
    public String toString(boolean b) { //?
        return toString();
    }
}
