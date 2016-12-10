package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;

import java.awt.Dimension;

public abstract class Watermark implements Operation {

    private int inset = 0;
    private Position position;

    public Watermark(Position position, int inset) {
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

    public boolean isNoOp() {
        return false;
    }

    @Override
    public boolean isNoOp(Dimension fullSize, OperationList opList) {
        return isNoOp();
    }

    public void setInset(int inset) {
        this.inset = inset;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

}
