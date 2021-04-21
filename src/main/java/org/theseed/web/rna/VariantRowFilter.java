/**
 *
 */
package org.theseed.web.rna;

import java.util.List;

import org.theseed.rna.RnaFeatureData;
import org.theseed.web.ColumnProcessor;

/**
 * This filter only displays rows where at least one cell has a different range color from the others.
 *
 * @author Bruce Parrello
 */
public class VariantRowFilter extends RowFilter {

    // FIELDS
    /** parent column processor */
    private ColumnProcessor processor;

    public VariantRowFilter(ColumnProcessor processor) {
        this.processor = processor;
    }

    @Override
    public boolean isRowDisplayable(RnaFeatureData feat) {
        boolean retVal = false;
        List<CellDescriptor> cells = processor.getColoredCells();
        if (cells.size() > 0) {
            int range = cells.get(0).getRange();
            for (int i = 1; i < cells.size() && ! retVal; i++)
                retVal = (cells.get(i).getRange() != range);
        }
        return retVal;
    }

}
