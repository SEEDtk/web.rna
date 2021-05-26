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
    /** region index */
    private int regionIdx;

    public AllGroupPageFilter(IParms processor) {
        super(processor);
        this.genomeCols = processor.getColumns();
        this.regionIdx = processor.getRegionIndex();
    }

    @Override
    public boolean isDisplay(String[] featureSpec) {
        boolean retVal = true;
        for (int i = 4; i < featureSpec.length && retVal; i++) {
            if (this.genomeCols.get(i) && featureSpec[i].charAt(this.regionIdx) == ' ')
                retVal = false;
        }
        return retVal;
    }

}
