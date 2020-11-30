/**
 *
 */
package org.theseed.web.rna;

import org.theseed.proteins.SampleId;
import org.theseed.web.ColSpec;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;

/**
 * This table displays the raw production data without any comparison.
 *
 * @author Bruce Parrello
 *
 */
public class ProductionDisplayTable implements IProductionTable {

    // FIELDS
    /** output table */
    HtmlTable<Key.RevFloat> table;

    public ProductionDisplayTable() {
        this.table = new HtmlTable<Key.RevFloat>(new ColSpec.Normal("Sample"), new ColSpec.Fraction("Predicted"),
                new ColSpec.Fraction("Actual"), new ColSpec.Fraction("Growth"));
    }

    @Override
    public void recordSample(SampleId sample, double production, double actual, double growth) {
        HtmlTable<Key.RevFloat>.Row row = this.table.new Row(new Key.RevFloat(production))
                .add(sample.toString()).addKey();
        if (Double.isNaN(actual))
            row.add("").add("");
        else
            row.add(actual).add(growth);
    }

    @Override
    public HtmlTable<? extends Key> closeTable() {
        return table;
    }

}
