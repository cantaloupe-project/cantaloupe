package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;

public abstract class Overlay implements Operation {

    boolean isFrozen;
    private int inset;
    private Position position;

    public Overlay(Position position, int inset) {
        this.setPosition(position);
        this.setInset(inset);
    }

    void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    public int getInset() {
        return inset;
    }

    public Position getPosition() {
        return position;
    }

    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @param inset Inset to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setInset(int inset) {
        checkFrozen();
        this.inset = inset;
    }

    /**
     * @param position Position to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setPosition(Position position) {
        checkFrozen();
        this.position = position;
    }

}
