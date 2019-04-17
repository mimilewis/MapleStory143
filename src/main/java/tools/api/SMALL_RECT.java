/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.api;

import com.sun.jna.Structure;

import java.util.List;

public class SMALL_RECT extends Structure {

    public short Left;
    public short Top;
    public short Right;
    public short Bottom;

    @Override
    protected List getFieldOrder() {
        return null;
    }
}
