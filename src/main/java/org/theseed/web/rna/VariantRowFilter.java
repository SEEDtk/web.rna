/**
 *
 */
package org.theseed.web.rna;

import java.util.List;

/**
 * This filter only displays rows where at least one cell has a different range color from the others.
 *
 * @author Bruce Parrello
 */
public class VariantRowFilter extends RowFilter {

    @Override
    public boolean isRowDisplayable(List<CellDescriptor> cells) {
        boolean retVal = false;
        if (cells.size() > 0) {
            int range = cells.get(0).getRange();
            for (int i = 1; i < cells.size() && ! retVal; i++)
                retVal = (cells.get(i).getRange() != range);
        }
        return retVal;
    }

}
