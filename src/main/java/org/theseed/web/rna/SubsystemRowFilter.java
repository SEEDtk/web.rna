/**
 *
 */
package org.theseed.web.rna;

import org.theseed.rna.RnaData.FeatureData;
import org.theseed.web.ColumnProcessor;

/**
 * This filter only shows rows that belong to the focus subsystem.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemRowFilter extends RowFilter {

    // FIELDS
    /** controlling column processor */
    private ColumnProcessor processor;

    /**
     * Construct a subsystem row filter.
     *
     * @param processor		controlling column processor
     */
    public SubsystemRowFilter(ColumnProcessor processor) {
        this.processor = processor;
    }

    @Override
    public boolean isRowDisplayable(FeatureData feat) {
        String fid = feat.getId();
        return this.processor.getSubFids().contains(fid);
    }

}
