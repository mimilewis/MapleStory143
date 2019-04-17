package server.maps;

import tools.Pair;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MapleReactorStats {

    private final Map<Byte, StateData> stateInfo = new HashMap<>();
    private Point tl;
    private Point br;

    public Point getTL() {
        return tl;
    }

    public void setTL(Point tl) {
        this.tl = tl;
    }

    public Point getBR() {
        return br;
    }

    public void setBR(Point br) {
        this.br = br;
    }

    public void addState(byte state, int type, Pair<Integer, Integer> reactItem, byte nextState, int timeOut, byte canTouch) {
        stateInfo.put(state, new StateData(type, reactItem, nextState, timeOut, canTouch));
    }

    public byte getNextState(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getNextState();
        } else {
            return -1;
        }
    }

    public int getType(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getType();
        } else {
            return -1;
        }
    }

    public Pair<Integer, Integer> getReactItem(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getReactItem();
        } else {
            return null;
        }
    }

    public int getTimeOut(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getTimeOut();
        } else {
            return -1;
        }
    }

    public byte canTouch(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.canTouch();
        } else {
            return 0;
        }
    }

    private static class StateData {

        private final int type;
        private final int timeOut;
        private final Pair<Integer, Integer> reactItem;
        private final byte nextState;
        private final byte canTouch;

        private StateData(int type, Pair<Integer, Integer> reactItem, byte nextState, int timeOut, byte canTouch) {
            this.type = type;
            this.reactItem = reactItem;
            this.nextState = nextState;
            this.timeOut = timeOut;
            this.canTouch = canTouch;
        }

        private int getType() {
            return type;
        }

        private byte getNextState() {
            return nextState;
        }

        private Pair<Integer, Integer> getReactItem() {
            return reactItem;
        }

        private int getTimeOut() {
            return timeOut;
        }

        private byte canTouch() {
            return canTouch;
        }
    }
}
