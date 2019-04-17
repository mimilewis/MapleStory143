/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.maps.pvp;

import java.awt.*;

/**
 * @author PlayDK
 */
public class PvpAttackInfo {

    public int skillId;
    public int critRate; //爆击概率
    public int ignoreDef; //无视防御
    public int skillDamage; //技能攻击
    public int mobCount; //攻击角色的数量
    public int attackCount; //攻击角色的次数
    public int pvpRange; //攻击的距离
    public boolean facingLeft;
    public double maxDamage;
    public Rectangle box;
}
