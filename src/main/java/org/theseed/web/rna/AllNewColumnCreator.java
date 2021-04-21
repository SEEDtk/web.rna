/**
 *
 */
package org.theseed.web.rna;

import java.util.List;

/**
 * This is an insane column creator that returns all of the samples as single-value columns.
 *
 * @author Bruce Parrello
 *
 */
public class AllNewColumnCreator extends NewColumnCreator {

    public AllNewColumnCreator(List<String> samples) {
        super(null, null, samples);
    }

    @Override
    public List<String> getNewColumns() {
        return this.getAllSamples();
    }

}
