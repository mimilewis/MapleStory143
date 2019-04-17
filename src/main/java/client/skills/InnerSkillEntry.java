/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.skills;

import java.io.Serializable;

/**
 * 内在能力技能设置
 *
 * @author PlayDK
 */
public class InnerSkillEntry implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private int skillId;
    private int skillevel;
    private byte position, rank;

    public InnerSkillEntry() {

    }

    public InnerSkillEntry(int skillId, int skillevel, byte position, byte rank) {
        this.skillId = skillId;
        this.skillevel = skillevel;
        this.position = position;
        this.rank = rank;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLevel() {
        return skillevel;
    }

    public byte getPosition() {
        return position;
    }

    public byte getRank() {
        return rank;
    }
}
