/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.ArrayList;
import java.util.List;

/**
 * @author PlayDK
 */
public class StructExclusiveEquip {

    public final List<Integer> itemIDs = new ArrayList<>(); //禁止穿戴的列表
    public int id;
    public String msg; //提示
}
