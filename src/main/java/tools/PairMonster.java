package tools;

import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;

/**
 * Represents a pair of values.
 *
 * @param <E> The type of the left value.
 * @param <F> The type of the right value.
 * @author Frz
 * @version 1.0
 * @since Revision 333
 */
public class PairMonster implements Comparable<PairMonster> {

    private static final long serialVersionUID = 9179541993413738569L;
    public final MonsterStatus left;
    public final MonsterStatusEffect right;

    /**
     * Class constructor - pairs two objects together.
     *
     * @param left  The left object.
     * @param right The right object.
     */
    public PairMonster(MonsterStatus left, MonsterStatusEffect right) {
        this.left = left;
        this.right = right;
    }

    public static <MonsterStatus, MonsterStatusEffect> Pair<MonsterStatus, MonsterStatusEffect> Create(MonsterStatus left, MonsterStatusEffect right) {
        return new Pair<>(left, right);
    }

    /**
     * Gets the left value.
     *
     * @return The left value.
     */
    public MonsterStatus getLeft() {
        return left;
    }

    /**
     * Gets the right value.
     *
     * @return The right value.
     */
    public MonsterStatusEffect getRight() {
        return right;
    }

    /**
     * Turns the pair into a string.
     *
     * @return Each value of the pair as a string joined by a colon.
     */
    @Override
    public String toString() {
        return left.toString() + ":" + right.toString();
    }

    /**
     * Gets the hash code of this pair.
     */
    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        return result;
    }

    /**
     * Checks to see if two pairs are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Pair other = (Pair) obj;
        if (left == null) {
            if (other.left != null) {
                return false;
            }
        } else if (!left.equals(other.left)) {
            return false;
        }
        if (right == null) {
            if (other.right != null) {
                return false;
            }
        } else if (!right.equals(other.right)) {
            return false;
        }
        return true;
    }

    public Integer getOrder() {
        return this.left.getPosition();
    }

    public void setOrder(Integer order) {
        //this.order = order;
    }

    @Override
    public int compareTo(PairMonster o) {
        return this.getOrder().compareTo(o.getLeft().getPosition());
    }
}
