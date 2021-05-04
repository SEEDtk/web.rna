/**
 *
 */
package org.theseed.web.rna;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.theseed.samples.SampleId;
import org.theseed.web.ProductionProcessor;

/**
 * This is a special case of the production comparison table where we are checking for deletion or not
 * of a specific protein.
 *
 * @author Bruce Parrello
 *
 */
public class ProductionInsertTable extends ProductionCompareTable {

    // FIELDS
    /** name of protein being inserted */
    private String protName;

    /**
     * Construct a production delete table.
     *
     * @param protein	protein whose deletion is being monitored.
     */
    public ProductionInsertTable(ProductionProcessor parent, String protein) {
        // Save the sort column.
        this.init(parent);
        // Create the row maps and the main table.
        this.setup(parent, Arrays.asList("no " + protein, "insert " + protein));
        // Save the protein name.
        this.protName = protein;
    }

    @Override
    public void recordSample(SampleId sample, double production, double actual, double growth) {
        // Get the inserts for this sample.
        Set<String> inserts = sample.getInserts();
        // Figure out whether or not the protein was deleted.
        int colIdx;
        String sampleSpec;
        if (inserts.contains(this.protName)) {
            colIdx = 2;
            sampleSpec = sample.unInsert(protName);
        } else {
            colIdx = 1;
            sampleSpec = sample.toString();
        }
        // Form a sample ID pattern to get all possibilities.
        String pattern = IntStream.range(0, SampleId.NORMAL_SIZE).mapToObj(i -> (i == SampleId.INSERT_COL ? "X" : sample.getFragment(i)))
                .collect(Collectors.joining("_"));
        this.store(sampleSpec, pattern, colIdx, production, actual, growth);

    }

}
