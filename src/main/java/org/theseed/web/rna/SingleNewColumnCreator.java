/**
 *
 */
package org.theseed.web.rna;

import java.util.Collections;
import java.util.List;

/**
 * This is the simplest new-column creator.  It returns a single new column.
 *
 * @author Bruce Parrello
 *
 */
public class SingleNewColumnCreator extends NewColumnCreator {

    public SingleNewColumnCreator(String samp1, String samp2, List<String> samps) {
        super(samp1, samp2, samps);
    }

    @Override
    public List<String> getNewColumns() {
        return Collections.singletonList(this.getSample1() + "," + this.getSample2());
    }

}
