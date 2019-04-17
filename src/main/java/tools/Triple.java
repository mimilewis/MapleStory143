package tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Triple<E, F, G> implements Serializable {

    private static final long serialVersionUID = 9179541993413739999L;
    public E left;
    public F mid;
    public G right;

    @JsonCreator
    public Triple(@JsonProperty("left") E left, @JsonProperty("mid") F mid, @JsonProperty("right") G right) {
        this.left = left;
        this.mid = mid;
        this.right = right;
    }

    public E getLeft() {
        return left;
    }

    public F getMid() {
        return mid;
    }

    public G getRight() {
        return right;
    }

    @Override
    public String toString() {
        return left.toString() + ":" + mid.toString() + ":" + right.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((mid == null) ? 0 : mid.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        return result;
    }

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
        final Triple other = (Triple) obj;
        if (left == null) {
            if (other.left != null) {
                return false;
            }
        } else if (!left.equals(other.left)) {
            return false;
        }
        if (mid == null) {
            if (other.mid != null) {
                return false;
            }
        } else if (!mid.equals(other.mid)) {
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
}
