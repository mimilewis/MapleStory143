/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.Serializable;

public class Quadruple<E, F, G, H> implements Serializable {

    private static final long serialVersionUID = 9179541993413749999L;
    public final E one;
    public final F two;
    public final G three;
    public final H four;

    public Quadruple(E one, F two, G three, H four) {
        this.one = one;
        this.two = two;
        this.three = three;
        this.four = four;
    }

    public E getOne() {
        return one;
    }

    public F getTwo() {
        return two;
    }

    public G getThree() {
        return three;
    }

    public H getFour() {
        return four;
    }

    @Override
    public String toString() {
        return one.toString() + ":" + two.toString() + ":" + three.toString() + ":" + four.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((one == null) ? 0 : one.hashCode());
        result = prime * result + ((two == null) ? 0 : two.hashCode());
        result = prime * result + ((three == null) ? 0 : three.hashCode());
        result = prime * result + ((four == null) ? 0 : four.hashCode());
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
        final Quadruple other = (Quadruple) obj;
        if (one == null) {
            if (other.one != null) {
                return false;
            }
        } else if (!one.equals(other.one)) {
            return false;
        }
        if (two == null) {
            if (other.two != null) {
                return false;
            }
        } else if (!two.equals(other.two)) {
            return false;
        }
        if (three == null) {
            if (other.three != null) {
                return false;
            }
        } else if (!three.equals(other.three)) {
            return false;
        }
        if (four == null) {
            if (other.four != null) {
                return false;
            }
        } else if (!four.equals(other.four)) {
            return false;
        }
        return true;
    }

}
