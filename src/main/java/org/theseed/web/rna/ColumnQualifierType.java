/**
 *
 */
package org.theseed.web.rna;

import org.theseed.utils.IDescribable;

/**
 * This enum comprises the column qualification types.  Each enum value has an abstract method that takes as
 * input a column descriptor and returns TRUE iff the column should be colored by range.
 *
 * This is an enum rather than a base class or interface because it is significantly less complex than other type-based
 * parameters.
 *
 * @author Bruce Parrello
 *
 */
public enum ColumnQualifierType implements IDescribable {
    RATIO {
        /** only color ratio columns */

        @Override
        public boolean isRangeColored(ColumnDescriptor col) {
            return col instanceof DifferentialColumnDescriptor;
        }

        @Override
        public String getDescription() {
            return "Only color ratio columns.";
        }

    },
    VALUE {
        /** only color value columns */

        @Override
        public boolean isRangeColored(ColumnDescriptor col) {
            return col instanceof SimpleColumnDescriptor;
        }

        @Override
        public String getDescription() {
            return "Only color value columns.";
        }

    },
    NONE {
        /** do not color any columns */

        @Override
        public boolean isRangeColored(ColumnDescriptor col) {
            return false;
        }

        @Override
        public String getDescription() {
            return "Do not color any columns.";
        }

    };

    /**
     * @return TRUE iff the specified column should be range-colored
     *
     * @param col	descriptor of column of interest
     * @return
     */
    public abstract boolean isRangeColored(ColumnDescriptor col);
}
