/**
 *
 */
package org.theseed.web.rna;

import java.util.Set;

import org.theseed.proteins.SampleId;
import org.theseed.web.ColSpec;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.ProductionProcessor;

/**
 * This is a special case of the production comparison table where we are checking for deletion or not
 * of a specific protein.
 *
 * @author Bruce Parrello
 *
 */
public class ProductionDeleteTable extends ProductionCompareTable {

    // FIELDS
    /** name of protein being deleted */
    private String protName;

    /**
     * Construct a production delete table.
     *
     * @param protein	protein whose deletion is being monitored.
     */
    public ProductionDeleteTable(ProductionProcessor parent, String protein) {
        // Create the output table.
        this.prodTable = new HtmlTable<Key.RevFloat>(new ColSpec.Normal("Sample Spec"), this.sortable(parent, 1, "keep " + protein),
                this.sortable(parent, 2, "delete " + protein));
        // Create the row map.
        this.setup();
        // Save the protein name.
        this.protName = protein;

    }

    @Override
    public void recordSample(SampleId sample, double production, double actual, double growth) {
        // Get the deletes for this sample.
        Set<String> deletes = sample.getDeletes();
        // Figure out whether or not the protein was deleted.
        int colIdx;
        String sampleSpec;
        if (deletes.contains(this.protName)) {
            colIdx = 2;
            sampleSpec = sample.unDelete(protName);
        } else {
            colIdx = 1;
            sampleSpec = sample.toString();
        }
        this.store(sampleSpec, colIdx, production, actual, growth);

    }

}
