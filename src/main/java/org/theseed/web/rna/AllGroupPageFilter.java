/**
 *
 */
package org.theseed.web.rna;

import java.util.BitSet;

/**
 * This class passes a feature if all of the selected genomes have changed.
 *
 * @author Bruce Parrello
 */
public class AllGroupPageFilter extends GroupPageFilter {

    // FIELDS
    /** columns of interest */
    private BitSet genomeCols;

    public AllGroupPageFilter(IParms processor) {
        super(processor);
        this.genomeCols = processor.getColumns();
    }

    @Override
    public boolean isDisplay(String[] featureSpec) {
        boolean retVal = true;
        for (int i = 0; i < featureSpec.length && retVal; i++) {
            if (this.genomeCols.get(i) && featureSpec[i].isEmpty())
                retVal = false;
        }
        return retVal;
    }

}
