/**
 *
 */
package org.theseed.web.rna;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import static j2html.TagCreator.*;

import org.theseed.samples.SampleId;
import org.theseed.web.CellContent;
import org.theseed.web.ColSpec;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.Row;
import org.theseed.web.WebProcessor;

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
    private HtmlTable<FloatPairKey> table;
    /** prediction/actual error tracker */
    private SummaryStatistics tracker;
    /** sample counter */
    private int counter;
    /** actual-used flag */
    private boolean useActual;
    /** web processor for generating links */
    private WebProcessor processor;

    /**
     * This is a key class for an actual,predicted pairing.  We sort highest to lowest by predicted
     * value.  If the actualFlag is on, we sort by actual value and then predicted.  When the actual
     * value is missing, we put the predicted value in its place.
     */
    public class FloatPairKey extends Key implements Comparable<FloatPairKey> {

        private double predicted;
        private double actual;

        public FloatPairKey(double predicted, double actual) {
            this.predicted = predicted;
            if (Double.isNaN(actual))
                actual = predicted;
            this.actual = (ProductionDisplayTable.this.useActual ? actual : predicted);
        }

        @Override
        public void store(CellContent cell, ColSpec col) {
            col.store(cell, this.predicted);
        }

        @Override
        public int compareTo(FloatPairKey o) {
            int retVal = 0;
            if (! Double.isNaN(this.actual) || ! Double.isNaN(o.actual))
                retVal = Double.compare(o.actual, this.actual);
            if (retVal == 0)
                retVal = Double.compare(o.predicted, this.predicted);
            return retVal;
        }

    }


    public ProductionDisplayTable(WebProcessor processor, boolean actual) {
        this.table = new HtmlTable<FloatPairKey>(new ColSpec.Normal("Sample"), new ColSpec.Fraction("Predicted"),
                new ColSpec.Fraction("Actual"), new ColSpec.Fraction("Growth"));
        this.tracker = new SummaryStatistics();
        this.processor = processor;
        this.counter = 0;
        this.useActual = actual;
    }

    @Override
    public void recordSample(SampleId sample, double production, double actual, double growth) {
        String sampleName = sample.toString();
        this.counter++;
        DomContent sampleLink = this.processor.commandLink(sampleName, "rna", "sample", "sample=" + sampleName)
                .withTarget("_blank");
        Row<FloatPairKey> row = new Row<FloatPairKey>(this.table, this.new FloatPairKey(production, actual))
                .add(sampleLink).add(production);
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
            retVal = p(String.format("No actual results were found in this set of %d samples.", this.counter));
        else
            retVal = p(String.format("%d actual results in this set of %d samples.  Mean error is %g, stdev %g.",
                    n, this.counter, this.tracker.getMean(), this.tracker.getStandardDeviation()));
        return retVal;
    }

}
