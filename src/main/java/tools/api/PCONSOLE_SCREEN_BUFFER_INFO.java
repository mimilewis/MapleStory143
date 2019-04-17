/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.api;

import com.sun.jna.Structure;

import java.util.List;

public class PCONSOLE_SCREEN_BUFFER_INFO extends Structure {

    public COORD dwSize;
    public COORD dwCursorPosition;
    public short wAttributes;
    public SMALL_RECT srWindow;
    public COORD dwMaximumWindowSize;

    @Override
    protected List getFieldOrder() {
        return null;
    }
}
