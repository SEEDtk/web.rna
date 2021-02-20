/**
 *
 */
package org.theseed.web.rna;

import org.theseed.rna.RnaData.FeatureData;
import org.theseed.utils.IDescribable;
import org.theseed.web.ColumnProcessor;

/**
 * This is the base class for the object that determines which rows are displayed.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RowFilter {

    public static enum Type implements IDescribable {
        VARIANT {
            @Override
            public RowFilter create(ColumnProcessor processor) {
                return new VariantRowFilter(processor);
            }

            @Override
            public String getDescription() {
                return "Only show rows with more than one range category.";
            }
        }, ALL {
            @Override
            public RowFilter create(ColumnProcessor processor) {
                return new RowFilter.All();
            }

            @Override
            public String getDescription() {
                return "Show all rows.";
            }
        }, REGION {
            @Override
            public RowFilter create(ColumnProcessor processor) {
                return new RegionRowFilter(processor.getFocus());
            }

            @Override
            public String getDescription() {
                return "Only show rows near the focus peg.";
            }
        }, SUBSYSTEM {
            @Override
            public RowFilter create(ColumnProcessor processor) {
                return new SubsystemRowFilter(processor);
            }

            @Override
            public String getDescription() {
                return "Only show rows in the focus subsystem.";
            }
        };


        public abstract RowFilter create(ColumnProcessor processor);

        public abstract String getDescription();

    }

    /**
     * @return TRUE if the specified row should be displayed, else FALSE
     *
     * @param feat		feature being displayed
     */
    public abstract boolean isRowDisplayable(FeatureData feat);

    /**
     * This is the simplest type of row filter:  it accepts every row.
     */
    public static class All extends RowFilter {

        @Override
        public boolean isRowDisplayable(FeatureData feat) {
            return true;
        }

    }

}
