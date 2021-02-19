/**
 *
 */
package org.theseed.web.rna;

import org.theseed.rna.RnaData;
import org.theseed.web.CellContent;
import org.theseed.web.ColSpec;
import org.theseed.web.Key;

/**
 * This is an HTML table key-type that allows sorting by RevRatio, then peg location and finally peg ID.
 *
 * @author Bruce Parrello
 *
 */
public class MultiKey extends Key implements Comparable<MultiKey> {

    // FIELDS
    /** target feature data */
    private RnaData.FeatureData feat;
    /** ratio for primary sort */
    private Key.RevRatio ratio;
    /** constant RevRatio for location-only sorts */
    private static final Key.RevRatio LOCATION_ONLY = new Key.RevRatio(1.0, 0.0);

    /**
     * Create a sort key for the specified feature in the specified column.
     *
     * @param feat	feature being sorted
     * @param col	sort column, or NULL if sorting is by location only
     */
    public MultiKey(RnaData.FeatureData feat, ColumnDescriptor col) {
        this.feat = feat;
        if (col == null)
            this.ratio = LOCATION_ONLY;
        else
            this.ratio = col.getKey(feat);
    }

    @Override
    public void store(CellContent cell, ColSpec col) {
        // We just store the ratio for display.
        this.ratio.store(cell, col);
    }

    @Override
    public int compareTo(MultiKey o) {
        int retVal = this.ratio.compareTo(o.ratio);
        if (retVal == 0)
            retVal = this.feat.compareTo(o.feat);
        return retVal;
    }

}
