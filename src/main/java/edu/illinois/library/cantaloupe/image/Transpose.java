package edu.illinois.library.cantaloupe.image;

/**
 * Encapsulates a transposition (flipping/mirroring) operation on an image.
 */
public class Transpose implements Operation {

    public enum Axis {
        /** Indicates mirroring. */
        HORIZONTAL,
        /** Indicates flipping. */
        VERTICAL
    }

    private Axis axis = Axis.HORIZONTAL;

    /**
     * No-op constructor.
     */
    public Transpose() {}

    /**
     * @param axis
     */
    public Transpose(Axis axis) {
        this.setAxis(axis);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Transpose) {
            Transpose t = (Transpose) obj;
            if (t.getAxis() != null && this.getAxis() != null) {
                return t.getAxis().equals(this.getAxis());
            }
        }
        return super.equals(obj);
    }

    /**
     * @return Axis
     */
    public Axis getAxis() {
        return this.axis;
    }

    public boolean isNoOp() {
        return (this.getAxis() == null);
    }

    /**
     * @param axis
     */
    public void setAxis(Axis axis) {
        this.axis = axis;
    }

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to be meaningful.
     */
    @Override
    public String toString() {
        switch (getAxis()) {
            case HORIZONTAL:
                return "h";
            case VERTICAL:
                return "v";
            default:
                return super.toString();
        }
    }
}
