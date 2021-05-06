/**
 *
 */
package org.theseed.web.rna;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the simplest new-column creator.  It returns a single new column for each primary sample.
 *
 * @author Bruce Parrello
 *
 */
public class SingleNewColumnCreator extends NewColumnCreator {

    public SingleNewColumnCreator(List<String> samp1, String samp2, List<String> samps) {
        super(samp1, samp2, samps);
    }

    @Override
    public List<String> getNewColumns() {
        String samp2 = this.getSample2();
        return this.getSample1().stream().map(x -> x + "," + samp2).collect(Collectors.toList());
    }

}
