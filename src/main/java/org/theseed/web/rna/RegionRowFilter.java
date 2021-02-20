/**
 *
 */
package org.theseed.web.rna;

import org.theseed.locations.Location;
import org.theseed.rna.RnaData;
import org.theseed.rna.RnaData.FeatureData;

/**
 * @author Bruce Parrello
 *
 */
public class RegionRowFilter extends RowFilter {

    // FIELDS
    /** location of focus peg */
    private Location loc;
    /** region distance to display */
    public static int MAX_DISTANCE = 5000;


    public RegionRowFilter(RnaData.Row focusRow) {
        if (focusRow == null)
            this.loc = null;
        else
            this.loc = focusRow.getFeat().getLocation();
    }

    @Override
    public boolean isRowDisplayable(FeatureData feat) {
        boolean retVal = true;
        if (this.loc != null) {
            Location loc2 = feat.getLocation();
            retVal = (this.loc.distance(loc2) <= MAX_DISTANCE);
        }
        return retVal;
    }

}
