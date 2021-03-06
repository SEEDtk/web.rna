/**
 *
 */
package org.theseed.web.rna;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This creator expands the first primary sample to all its different time points.
 *
 * @author Bruce Parrello
 */
public class Time1NewColumnCreator extends NewColumnCreator {

    public Time1NewColumnCreator(List<String> samp1, String samp2, List<String> samps) {
        super(samp1, samp2, samps);
    }

    @Override
    public List<String> getNewColumns() {
        String samp2 = this.getSample2();
        String samp1 = this.getOnlySample1();
        // Get all the variations of the first sample and append the second.
        List<String> retVal = this.getAllTimes(samp1).values().stream()
                .filter(x -> ! (x.contentEquals(samp2))).map(x -> x + "," + samp2).collect(Collectors.toList());
        return retVal;
    }

}
