/**
 *
 */
package org.theseed.web.rna;

import org.theseed.rna.RnaFeatureData;
import org.theseed.web.ColumnProcessor;

/**
 * This is a row filter that restricts the display to rows with a value above a specified minimum in
 * the current filter column.
 *
 * @author Bruce Parrello
 *
 */
public class ColumnValueHighFilter extends RowFilter {

    // FIELDS
    /** controlling command processor */
    private ColumnProcessor processor;
    /** minimum value for display */
    private double filterMin;

    public ColumnValueHighFilter(ColumnProcessor processor) {
        this.processor = processor;
        this.filterMin = processor.getFilterMin();
    }

    @Override
    public boolean isRowDisplayable(RnaFeatureData feat) {
        double valueInCol = this.processor.getFilterColumnValue(feat);
        return (valueInCol >= this.filterMin);
    }

}
