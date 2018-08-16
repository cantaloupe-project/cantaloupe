package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Sharpen implements Operation {

    private static final double DELTA = 0.00000001;

    private double amount;
    private boolean isFrozen;

    /**
     * No-op constructor.
     */
    public Sharpen() {}

    /**
     * @param amount Amount to sharpen.
     */
    public Sharpen(double amount) {
        setAmount(amount);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    @Override
    public boolean hasEffect() {
        return (getAmount() > DELTA);
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    public double getAmount() {
        return amount;
    }

    /**
     * @param amount Amount to sharpen.
     * @throws IllegalArgumentException if the supplied amount is less than
     *                                  zero.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setAmount(double amount) throws IllegalArgumentException {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be >= 0.");
        }
        this.amount = amount;
    }

    /**
     * @return Map with an {@literal amount} key corresponding to the amount.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("amount", getAmount());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to have any particular format.
     */
    @Override
    public String toString() {
        return String.valueOf(getAmount());
    }
}
