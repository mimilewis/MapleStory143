/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.skills;

public class ForceSkillEntry {

    private final int df_recovery;
    private int attackCount;

    public ForceSkillEntry(int attackCount, int df_recovery) {
        this.attackCount = attackCount;
        this.df_recovery = df_recovery;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public void decAttackCount() {
        this.attackCount--;
    }

    public int getDf_recovery() {
        return df_recovery;
    }

}
