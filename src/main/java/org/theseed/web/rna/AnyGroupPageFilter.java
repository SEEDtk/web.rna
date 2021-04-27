/**
 *
 */
package org.theseed.web.rna;

import java.util.BitSet;

/**
 * This class passes a feature if any of the genomes of interest have changed.
 *
 * @author Bruce Parrello
 *
 */
public class AnyGroupPageFilter extends GroupPageFilter {

    // FIELDS
    /** columns of interest */
    private BitSet genomeCols;

    public AnyGroupPageFilter(IParms processor) {
        super(processor);
        this.genomeCols = processor.getColumns();
    }

    @Override
    public boolean isDisplay(String[] featureSpec) {
        boolean retVal = false;
        for (int i = 0; i < featureSpec.length &&  ! retVal; i++) {
            if (this.genomeCols.get(i) && ! featureSpec[i].isEmpty())
                retVal = true;
        }
        return retVal;
    }

}
