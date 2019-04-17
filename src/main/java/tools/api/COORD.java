/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.api;

import com.sun.jna.Structure;

import java.util.List;

/**
 * @author zisedk
 */
public class COORD extends Structure {

    public short x;
    public short y;

    @Override
    protected List getFieldOrder() {
        return null;
    }
}