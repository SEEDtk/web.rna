/**
 *
 */
package org.theseed.web.rna;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import static j2html.TagCreator.*;
import org.theseed.proteins.SampleId;
import org.theseed.web.ColSpec;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.Row;

import j2html.tags.DomContent;

/**
 * This table displays the raw production data without any comparison.
 *
 * @author Bruce Parrello
 *
 */
public class ProductionDisplayTable implements IProductionTable {

    // FIELDS
    /** output table */
    private HtmlTable<Key.RevFloat> table;
    /** prediction/actual error tracker */
    private SummaryStatistics tracker;

    public ProductionDisplayTable() {
        this.table = new HtmlTable<Key.RevFloat>(new ColSpec.Normal("Sample"), new ColSpec.Fraction("Predicted"),
                new ColSpec.Fraction("Actual"), new ColSpec.Fraction("Growth"));
        this.tracker = new SummaryStatistics();
    }

    @Override
    public void recordSample(SampleId sample, double production, double actual, double growth) {
        Row<Key.RevFloat> row = new Row<Key.RevFloat>(this.table, new Key.RevFloat(production))
                .add(sample.toString()).addKey();
        if (Double.isNaN(actual))
            row.add("").add("");
        else {
            row.add(actual).add(growth);
            tracker.addValue(Math.abs(production - actual));
        }
    }

    @Override
    public HtmlTable<? extends Key> closeTable() {
        return table;
    }

    @Override
    public DomContent getSummary() {
        DomContent retVal;
        int n = (int) this.tracker.getN();
        if (n == 0)
            retVal = p("No actual results were found in this set.");
        else
            retVal = p(String.format("%d actual results in this set.  Mean error is %g, stdev %g.", n, this.tracker.getMean(),
                    this.tracker.getStandardDeviation()));
        return retVal;
    }

}
