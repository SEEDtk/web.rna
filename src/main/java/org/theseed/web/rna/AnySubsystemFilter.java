/**
 *
 */
package org.theseed.web.rna;

import org.theseed.rna.RnaData.FeatureData;
import org.theseed.web.ColumnProcessor;

/**
 * @author Bruce Parrello
 *
 */
public class AnySubsystemFilter extends RowFilter {

    // FIELDS
    /** controlling column processor */
    private ColumnProcessor processor;

    /**
     * Construct a subsystem row filter.
     *
     * @param processor		controlling column processor
     */
    public AnySubsystemFilter(ColumnProcessor processor) {
        this.processor = processor;
    }


    @Override
    public boolean isRowDisplayable(FeatureData feat) {
        return this.processor.getSubTable().isInSubsystem(feat.getId());
    }

}
