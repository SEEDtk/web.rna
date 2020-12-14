/**
 *
 */
package org.theseed.web.rna;

import java.util.List;

import org.theseed.utils.IDescribable;

/**
 * This is the base class for the object that determines which rows are displayed.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RowFilter {

    public static enum Type implements IDescribable {
        VARIANT, ALL;

        public RowFilter create() {
            RowFilter retVal = null;
            switch (this) {
            case VARIANT :
                retVal = new VariantRowFilter();
                break;

            case ALL :
                retVal = new RowFilter.All();
                break;

            }
            return retVal;
        }

        @Override
        public String getDescription() {
            String retVal = null;
            switch (this) {
            case VARIANT :
                retVal = "Only show rows with more than one range category.";
                break;
            case ALL :
                retVal = "Show all rows.";
                break;
            }
            return retVal;
        }
    }

    /**
     * @return TRUE if the specified row should be displayed, else FALSE
     *
     * @param cells		list of range-colored cells in the row of interest
     */
    public abstract boolean isRowDisplayable(List<CellDescriptor> cells);

    /**
     * This is the simplest type of row filter:  it accepts every row.
     */
    public static class All extends RowFilter {

        @Override
        public boolean isRowDisplayable(List<CellDescriptor> cells) {
            return true;
        }

    }

}
