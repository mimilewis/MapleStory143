package server.movement;

public interface LifeMovement extends LifeMovementFragment {

    int getNewstate();

    int getDuration();

    int getType();
}
