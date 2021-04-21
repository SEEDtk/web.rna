/**
 *
 */
package org.theseed.web.rna;

/**
 * This class contains the description of what to place in a single cell of the output table.  This includes the
 * value to display and the range code (0 for normal, 1 for low, 2 for middle, ...).
 *
 * @author Bruce Parrello
 *
 */
public class CellDescriptor {

    // FIELDS
    /** output value */
    private double value;
    /** range code */
    private int range;

    /**
     * Construct a new cell descriptor.
     *
     * @param value		proposed value
     * @param range		proposed range code
     */
    public CellDescriptor(double value, int range) {
        this.value = value;
        this.range = range;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return this.value;
    }

    /**
     * @return the range code
     */
    public int getRange() {
        return this.range;
    }


}
