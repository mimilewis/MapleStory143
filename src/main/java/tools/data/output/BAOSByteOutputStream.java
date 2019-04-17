package tools.data.output;

import java.io.ByteArrayOutputStream;

/**
 * Uses a byte array to output a stream of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 352
 */
public class BAOSByteOutputStream implements ByteOutputStream {

    private final ByteArrayOutputStream baos;

    /**
     * Class constructor - Wraps the stream around a Java BAOS.
     *
     * @param baos <code>The ByteArrayOutputStream</code> to wrap this around.
     */
    public BAOSByteOutputStream(ByteArrayOutputStream baos) {
        super();
        this.baos = baos;
    }

    /**
     * Writes a byte to the stream.
     *
     * @param b The byte to write to the stream.
     * @see tools.data.output.ByteOutputStream#writeByte(byte)
     */
    @Override
    public void writeByte(byte b) {
        baos.write(b);
    }
}
