/**
 *
 */
package org.theseed.web.rna;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.theseed.proteins.SampleId;
import org.theseed.reports.CoreHtmlUtilities;
import org.theseed.web.ColSpec;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.ProductionProcessor;
import org.theseed.web.WebProcessor;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This produces a production comparison table.  The table shows how sample predictions differ due to variances in a
 * single ID fragment.
 *
 * @author Bruce Parrello
 *
 */
public class ProductionCompareTable implements IProductionTable {

    // FIELDS
    /** table being built */
    protected HtmlTable<Key.RevFloat> prodTable;
    /** map of sample strings to table rows */
    protected Map<String, HtmlTable<Key.RevFloat>.Row> rowMap;
    /** sort column for table */
    protected int sortCol;
    /** index of the fragment being compared */
    private int fragIdx;
    /** original choice list, used to compute column indices */
    private List<String> choiceList;
    /** dummy key for each row */
    private static final Key.RevFloat DUMMY_KEY = new Key.RevFloat(Double.NEGATIVE_INFINITY);

    /**
     * Create the production comparison table
     *
     * @param sortCol		sort column for table
     * @param compareIdx	index of the fragment that is the basis for the comparison
     * @param choices		list of choices being compared
     */
    public ProductionCompareTable(ProductionProcessor parent, int compareIdx, Collection<String> choices) {
        // Create the table.
        ColSpec[] columns = new ColSpec[choices.size() + 1];
        columns[0] = new ColSpec.Normal("Sample Spec");
        int i = 1;
        for (String choice : choices) {
            columns[i] = this.sortable(parent, i, choice);
            i++;
        }
        this.prodTable = new HtmlTable<Key.RevFloat>(columns);
        // Initialize the other structures.
        this.setup();
        // Create the list used to determine which column contains each choice.
        this.choiceList = new ArrayList<String>(choices.size() + 1);
        this.choiceList.add("");
        this.choiceList.addAll(choices);
        // Save the fragment index.
        this.fragIdx = compareIdx;
        // Get the necessary parameters from the parent processor.
        init(parent);
    }

    /**
     * Initialize the common data structures.
     */
    protected void setup() {
        this.rowMap = new HashMap<String, HtmlTable<Key.RevFloat>.Row>(1000);
    }

    /**
     * Get the necessary parameters from the parent processor.
     *
     * @param parent	parent processor producing this table
     */
    public void init(ProductionProcessor parent) {
        // Save the sort column.
        this.sortCol = parent.getSortCol();
    }

    /**
     * @return the column specification for a sortable column
     *
     * @param i			index of the column
     * @param title		title of the column
     */
    protected ColSpec sortable(WebProcessor parent, int i, String title) {
        DomContent colTitle = parent.commandLink(title, "rna", "production", String.format("sortCol=%d", i), "saved=1");
        ColSpec retVal = new ColSpec.Fraction(colTitle);
        return retVal;
    }

    /**
     * Default constructor for subclasses
     */
    public ProductionCompareTable() { }

    @Override
    public void recordSample(SampleId sample, double production, double actual, double growth) {
        // Compute the name of the row for this sample.
        String sampleName = sample.replaceFragment(this.fragIdx, "X");
        // Compute the column.
        int colIdx = this.choiceList.indexOf(sample.getFragment(this.fragIdx));
        if (colIdx >= 1) {
            // We found the column, so store the cell in the sample's row.
            this.store(sampleName, colIdx, production, actual, growth);
        }
    }

    /**
     * Store production information into a cell of a production comparison table.
     *
     * @param spec			specification for the sample of interest
     * @param colIdx		index of the target column
     * @param production	predicted production value
     * @param actual		actual production value (if any)
     * @param growth		growth (if any)
     */
    protected void store(String spec, int colIdx, double production, double actual,
            double growth) {
        HtmlTable<Key.RevFloat>.Row row = this.rowMap.computeIfAbsent(spec, x -> (this.prodTable.new Row(DUMMY_KEY).add(spec)));
        DomContent prodHtml = text(String.format("%11.6f", production));
        if (! Double.isNaN(actual)) {
            prodHtml = CoreHtmlUtilities.toolTip(prodHtml, String.format("actual = %g, growth = %g", actual, growth));
            row.highlight(colIdx);
        }
        row.store(colIdx, prodHtml);
        if (colIdx == this.sortCol)
            this.prodTable.moveRow(row, new Key.RevFloat(production));
    }

    @Override
    public HtmlTable<? extends Key> closeTable() {
        return this.prodTable;
    }

}
