/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.HashMap;
import java.util.Map;

/**
 * @author PlayDK
 */
public class StructItemOption {

    public static final String[] types = {
            "incSTR", "incDEX", "incINT", "incLUK", "incACC", "incEVA", "incPAD", "incMAD", "incPDD", "incMDD", "incMHP", "incMMP",
            "incSTRr", "incDEXr", "incINTr", "incLUKr", "incACCr", "incEVAr", "incPADr", "incMADr", "incPDDr", "incMDDr", "incMHPr", "incMMPr",
            "incSTRlv", "incDEXlv", "incINTlv", "incLUKlv", "incPADlv", "incMADlv", //角色每10级增加属性
            "incSpeed", "incJump", "incCr", "incDAMr", "incTerR", "incAsrR", "incEXPr", "incMaxDamage",
            "HP", "MP", "RecoveryHP", "RecoveryMP", "level", "prop", "time",
            "ignoreTargetDEF", "ignoreDAM", "incAllskill", "ignoreDAMr", "RecoveryUP",
            "incCriticaldamageMin", "incCriticaldamageMax", "DAMreflect",
            "mpconReduce", "reduceCooltime", "incMesoProp", "incRewardProp", "boss", "attackType", "bufftimeR"};
    public final Map<String, Integer> data = new HashMap<>();
    public int optionType, reqLevel, opID; // opID = nebulite Id or potential ID
    public String face; // angry, cheers, love, blaze, glitter
    public String opString; //potential string

    public int get(String type) {
        return data.get(type) != null ? data.get(type) : 0;
    }

    @Override
    public String toString() { // I should read from the "string" value instead.
        StringBuilder ret = new StringBuilder();
        for (String type : types) {
            if (get(type) > 0) {
                ret.append(opString.replace("#" + type, String.valueOf(get(type))));
            }
        }
        return ret.toString();
    }
}
