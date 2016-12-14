package edu.illinois.library.cantaloupe.operation;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

public class Sharpen implements Operation {

    private float amount = 0;

    /**
     * No-op constructor.
     */
    public Sharpen() {}

    /**
     * @param amount Amount to sharpen.
     */
    public Sharpen(float amount) {
        setAmount(amount);
    }

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    @Override
    public boolean hasEffect() {
        return (getAmount() > 0.000001f);
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    public float getAmount() {
        return amount;
    }

    /**
     * @param amount Amount to sharpen.
     * @throws IllegalArgumentException If the supplied amount is less than
     *                                  zero.
     */
    public void setAmount(float amount) throws IllegalArgumentException {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be >= 0.");
        }
        this.amount = amount;
    }

    /**
     * @param fullSize Ignored.
     * @return Map with an <code>amount</code> key corresponding to the amount.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("amount", this.getAmount());
        return map;
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
