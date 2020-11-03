/**
 *
 */
package org.theseed.web.rna;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author Bruce Parrello
 *
 */
public class TimeBothNewColumnCreator extends NewColumnCreator {

    public TimeBothNewColumnCreator(String samp1, String samp2, List<String> samps) {
        super(samp1, samp2, samps);
    }

    @Override
    public List<String> getNewColumns() {
        SortedMap<String, String> samp1Map = this.getAllTimes(this.getSample1());
        if (this.getSample2().isEmpty())
            throw new IllegalArgumentException("Cannot use time-series comparison mode without a second sample ID.");
        Map<String, String> samp2Map = this.getAllTimes(this.getSample2());
        List<String> retVal = new ArrayList<String>(samp1Map.size());
        // Loop through the sample 1 time points and compare them to the associated sample 2 points.
        for (Map.Entry<String, String> samp1Entry : samp1Map.entrySet()) {
            String point1 = samp1Entry.getKey();
            String samp2 = samp2Map.get(point1);
            if (samp2 != null)
                retVal.add(samp1Entry.getValue() + "," + samp2);
        }
        if (retVal.size() == 0)
            throw new IllegalArgumentException(this.getSample1() + " and " + this.getSample2()
                    + " have no time points in common.");
        return retVal;
    }

}
