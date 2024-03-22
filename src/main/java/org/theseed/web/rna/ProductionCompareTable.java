/**
 *
 */
package org.theseed.web.rna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.theseed.reports.CoreHtmlUtilities;
import org.theseed.samples.SampleId;
import org.theseed.stats.BestColumn;
import org.theseed.web.ColSpec;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.ProductionProcessor;
import org.theseed.web.Row;
import org.theseed.web.WebProcessor;

import j2html.tags.ContainerTag;
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
    protected Map<String, Row<Key.RevFloat>> rowMap;
    /** sort column for table */
    protected int sortCol;
    /** index of the fragment being compared */
    private int fragIdx;
    /** original choice list, used to compute column indices */
    private List<String> choiceList;
    /** map used to track maximum value for each row */
    private Map<String, BestColumn> maxMap;
    /** title of each data column */
    private String[] colTitles;
    /** web processor for computing links */
    private WebProcessor processor;
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
        // Initialize the data structures.
        this.setup(parent, choices);
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
     *
     * @param parent	parent command processor
     * @param colNames	names of the data columns
     */
    protected void setup(ProductionProcessor parent, Collection<String> colNames) {
        // Create the row-based maps.
        this.rowMap = new HashMap<String, Row<Key.RevFloat>>(1000);
        this.maxMap = new HashMap<String, BestColumn>(1000);
        // Create the table and save the column names.
        ColSpec[] columns = new ColSpec[colNames.size() + 1];
        columns[0] = new ColSpec.Normal("Sample Spec");
        this.colTitles = new String[colNames.size() + 1];
        int i = 1;
        for (String colName : colNames) {
            columns[i] = this.sortable(parent, i, colName);
            this.colTitles[i] = colName;
            i++;
        }
        this.prodTable = new HtmlTable<Key.RevFloat>(columns);
        this.processor = parent;
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
            this.store(sampleName, sampleName, colIdx, production, actual, growth);
        }
    }

    /**
     * Store production information into a cell of a production comparison table.
     *
     * @param spec			specification for the sample of interest
     * @param pattern		pattern to use for the sample link
     * @param colIdx		index of the target column
     * @param production	predicted production value
     * @param actual		actual production value (if any)
     * @param growth		growth (if any)
     */
    protected void store(String spec, String pattern, int colIdx, double production, double actual,
            double growth) {
        DomContent html = this.getSampleLink(spec, pattern);
        Row<Key.RevFloat> row = this.rowMap.computeIfAbsent(spec, x -> (new Row<Key.RevFloat>(this.prodTable, DUMMY_KEY).add(html)));
        DomContent prodHtml = text(String.format("%11.6f", production));
        if (! Double.isNaN(actual)) {
            prodHtml = CoreHtmlUtilities.toolTip(prodHtml, String.format("actual = %g, growth = %g", actual, growth));
            row.highlight(colIdx);
        }
        // Store the value in the column.
        row.store(colIdx, prodHtml);
        // Insure the correct key is in place.
        if (colIdx == this.sortCol)
            this.prodTable.moveRow(row, new Key.RevFloat(production));
        // Update the best-column information.
        BestColumn tracker = this.maxMap.computeIfAbsent(spec, x -> new BestColumn());
        tracker.merge(colIdx, production);
    }

    @Override
    public HtmlTable<? extends Key> closeTable() {
        return this.prodTable;
    }

    @Override
    public DomContent getSummary() {
        // Here we want to know how often each column had the highest value.
        int[] counters = new int[this.colTitles.length];
        Arrays.fill(counters, 0);
        for (BestColumn best : this.maxMap.values())
            counters[best.getBestIdx()]++;
        // Create a table to output the counts.
        ColSpec[] columns = new ColSpec[counters.length];
        columns[0] = new ColSpec.Normal("");
        for (int i = 1; i < columns.length; i++)
            columns[i] = new ColSpec.Num(this.colTitles[i]);
        HtmlTable<Key.Null> maxTable = new HtmlTable<Key.Null>(columns);
        Row<Key.Null> row = new Row<Key.Null>(maxTable, Key.NONE);
        // Fill in the heading and the counts.
        row.add("Number of times column had maximum value.");
        for (int i = 1; i < counters.length; i++)
            row.add(counters[i]);
        // Return the table.
        return maxTable.output();
    }

    /**
     * @return a link to the web page for displaying the specified sample's information
     *
     * @param text		display text for the sample ID
     * @param sampleId	ID of the target sample
     */
    protected ContainerTag getSampleLink(String text, String sampleId) {
        return this.processor.commandLink(text, "rna", "sample", "sample=" + sampleId).withTarget("_blank");
    }

}
