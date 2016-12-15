package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.awt.Dimension;

public abstract class Overlay implements Operation {

    private int inset = 0;
    private Position position;

    public Overlay(Position position, int inset) {
        this.setPosition(position);
        this.setInset(inset);
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

    public void setInset(int inset) {
        this.inset = inset;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

}
