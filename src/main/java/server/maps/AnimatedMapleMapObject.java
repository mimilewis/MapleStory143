package server.maps;


/**
 * 该类表示冒险岛地图上所有具体生命特征的对象.例如玩家、怪物等.
 *
 * @author dongjak
 */
public abstract class AnimatedMapleMapObject extends MapleMapObject {

    private int stance;

    public int getStance() {
        return stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
    }

    public boolean isFacingLeft() {
        return getStance() % 2 != 0;
    }

    public int getFacingDirection() {
        return Math.abs(getStance() % 2);
    }
}
