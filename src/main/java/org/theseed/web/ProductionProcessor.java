/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.TabbedLineReader;
import org.theseed.proteins.SampleId;
import org.theseed.utils.ParseFailureException;
import org.theseed.web.rna.IProductionTable;
import org.theseed.web.rna.ProductionCompareTable;
import org.theseed.web.rna.ProductionDeleteTable;
import org.theseed.web.rna.ProductionDisplayTable;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This page displays a table of production data from a joined prediction file.  This is a tab-delimited file with
 * the following columns.
 *
 * 	sample_id	ID of a sample
 *  predicted	prediction output of the sample
 *  production	actual production of the sample (if known)
 *  density		growth of the sample (if known)
 *
 * The name of the joined production file is "thrall.production.tbl" in the CoreSEED data directory.
 *
 * The positional parameters are the name of the CoreSEED data directory and the name of the user's workspace.
 *
 * The command-line options are as follows.  The following are multi-valued.  They list all the values that should appear
 * in the output.
 *
 * --host		host fragment of sample ID
 * --del		deletion fragment of sample ID
 * --operon		operon fragment of sample ID
 * --loc		location fragment of sample ID
 * --asd		asd-status fragment of sample ID
 * --insert		insertion fragment of sample ID
 * --delete		deletion fragment of sample ID
 * --iptg		IPTG fragment of sample ID
 *
 * The following are normal parameters.
 *
 * --real		if specified, only samples with actual values (as opposed to predicted) will be displayed
 * --minPred	minimum predicted value to display
 * --maxPred	maximum predicted value to display
 * --compare	fragment for comparison
 * --saved		used saved parameter values for filters (no filters may be specified)
 *
 * @author Bruce Parrello
 *
 */
public class ProductionProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ProductionProcessor.class);
    /** list of selected choices for each fragment */
    private List<Collection<String>> filters;
    /** list of available choices for each fragment */
    private List<SortedSet<String>> choices;
    /** titles of fragments */
    private static final String[] FRAGMENT_TITLES = new String[] { "host", "del", "operon", "loc", "asd", "insert", "delete", "iptg" };
    /** names of fragment parameters (currently the same as the titles) */
    private static final String[] FRAGMENT_NAMES = FRAGMENT_TITLES;
    /** production table builder */
    private IProductionTable tableBuilder;

    // COMMAND-LINE OPTIONS

    /** legal values for first sample ID fragment */
    @Option(name = "--host", usage = "legal values for host fragment")
    protected List<String> f1;

    /** legal values for second sample ID fragment */
    @Option(name = "--del", usage = "legal values for deletion fragment")
    protected List<String> f2;

    /** legal values for third sample ID fragment */
    @Option(name = "--operon", usage = "legal values for operon fragment")
    protected List<String> f3;

    /** legal values for fourth sample ID fragment */
    @Option(name = "--loc", usage = "legal values for location fragment")
    protected List<String> f4;

    /** legal values for fifth sample ID fragment */
    @Option(name = "--asd", usage = "legal values for asd fragment")
    protected List<String> f5;

    /** legal values for sixth sample ID fragment */
    @Option(name = "--insert", usage = "legal values for insertion fragment")
    protected List<String> f6;

    /** legal values for seventh sample ID fragment */
    @Option(name = "--delete", usage = "legal values for deletion fragment")
    protected List<String> f7;

    /** legal values for eighth sample ID fragment */
    @Option(name = "--iptg", usage = "legal values for IPTG fragment")
    protected List<String> f8;

    /** TRUE if only samples with real results should be displayed */
    @Option(name = "--real", usage = "display only samples with real results")
    protected boolean realOnly;

    /** minimum predicted value */
    @Option(name = "--minPred", usage = "minimum predicted value to display")
    protected double minPred;

    /** maximum predicted value */
    @Option(name = "--maxPred", usage = "maximum predicted value to display")
    protected double maxPred;

    /** index of the fragment to use for comparison */
    @Option(name = "--compare", usage = "fragment column to compare")
    protected String compare;

    /** used saved parameter values */
    @Option(name = "--saved", usage = "use parameter values from cookie string")
    protected int restoreFilters;

    /** column to sort on */
    @Option(name = "--sortCol", usage = "index of column to sort on")
    protected int sortCol;

    @Override
    protected void setWebDefaults() {
        this.f1 = new ArrayList<String>();
        this.f2 = new ArrayList<String>();
        this.f3 = new ArrayList<String>();
        this.f4 = new ArrayList<String>();
        this.f5 = new ArrayList<String>();
        this.f6 = new ArrayList<String>();
        this.f7 = new ArrayList<String>();
        this.f8 = new ArrayList<String>();
        this.realOnly = false;
        this.minPred = 0.0;
        this.maxPred = 5.0;
        this.compare = "(none)";
        this.restoreFilters = 0;
        this.sortCol = 1;
    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        if (this.minPred > this.maxPred)
            throw new ParseFailureException("Minimum predicted value is greater than maximum.");
        // Save the filters.
        this.filters = Arrays.asList(this.f1, this.f2, this.f3, this.f4, this.f5, this.f6, this.f7, this.f8);
        // Initialize the choice list to empty sets all across the board.
        this.choices = IntStream.range(0, filters.size()).mapToObj(x -> new TreeSet<String>()).collect(Collectors.toList());
        return true;
    }

    @Override
    protected String getCookieName() {
        return HtmlForm.formCookieName("rna", "production");
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Save and/or restore the form data.
        if (this.restoreFilters == 1) {
            // Here we have to restore the old form data.
            for (int i = 0; i < FRAGMENT_NAMES.length; i++) {
                Collection<String> filter = this.filters.get(i);
                filter.clear();
                List<String> restored = Arrays.asList(StringUtils.split(cookies.get(FRAGMENT_NAMES[i], ""), ','));
                filter.addAll(restored);
            }
            this.realOnly = cookies.get("realOnly", false);
            this.minPred = cookies.get("minPred", 0.0);
            this.maxPred = cookies.get("maxPred", 5.0);
            this.compare = cookies.get("compare", "(none)");
        } else {
            // Here we have to save the new form data.
            for (int i = 0; i < FRAGMENT_NAMES.length; i++) {
                Collection<String> filter = this.filters.get(i);
                cookies.put(FRAGMENT_NAMES[i], StringUtils.join(filter, ','));
            }
            cookies.put("realOnly", this.realOnly);
            cookies.put("minPred", this.minPred);
            cookies.put("maxPred", this.maxPred);
            cookies.put("compare", this.compare);
        }
        // Compute the type of output table from the comparison string.
        int compareIdx = ArrayUtils.indexOf(FRAGMENT_TITLES, this.compare);
        if (compareIdx >= 0)
            this.tableBuilder = new ProductionCompareTable(this, compareIdx, this.filters.get(compareIdx));
        else if (this.compare.charAt(0) == 'D')
            this.tableBuilder = new ProductionDeleteTable(this, this.compare.substring(1));
        else
            this.tableBuilder = new ProductionDisplayTable();
        // Insure delete-nothing is a choice for the delete column.
        this.choices.get(SampleId.DELETE_COL).add("000");
        // Read the production file.
        File prodFile = new File(this.getCoreDir(), "thrall.production.tbl");
        try (TabbedLineReader prodStream = new TabbedLineReader(prodFile)) {
            int sampleCol = prodStream.findField("sample_id");
            int predCol = prodStream.findField("predicted");
            int actualCol = prodStream.findField("production");
            int growthCol = prodStream.findField("density");
            for (TabbedLineReader.Line line : prodStream) {
                // Determine the current sample.
                SampleId sample = new SampleId(line.get(sampleCol));
                // Update the choice lists and determine if we are keeping this sample.
                boolean keep = true;
                for (int i = 0; i < FRAGMENT_TITLES.length; i++) {
                    if (i != SampleId.DELETE_COL) {
                        String fragment = sample.getFragment(i);
                        this.choices.get(i).add(fragment);
                        if (! this.filters.get(i).contains(fragment))
                            keep = false;
                    } else {
                        // For deletes, if nothing was deleted, we check for the nothing case being in the filter.
                        // If something was deleted, it must be one of the filtered items.
                        Set<String> deletes = sample.getDeletes();
                        Collection<String> filter = this.filters.get(SampleId.DELETE_COL);
                        this.choices.get(SampleId.DELETE_COL).addAll(deletes);
                        if (deletes.isEmpty() && ! filter.contains("000"))
                            keep = false;
                        else if (filter.stream().allMatch(x -> ! deletes.contains(x)))
                            keep = false;
                    }
                }
                // Now we have updated the choice lists and we know if we want to keep this sample.
                // We still have to filter on the prediction value and the real-only flag.
                if (keep) {
                    double pred = line.getDouble(predCol);
                    double actual = Double.NaN;
                    double growth = Double.NaN;
                    if (line.isEmpty(actualCol))
                        keep = ! this.realOnly;
                    else {
                        actual = line.getDouble(actualCol);
                        growth = line.getDouble(growthCol);
                    }
                    if (keep && pred >= this.minPred && pred <= this.maxPred) {
                        // We are keeping this sample:  record it.
                        this.tableBuilder.recordSample(sample, pred, actual, growth);
                    }
                }
            }
        }
        // Get the display table.
        HtmlTable<? extends Key> prodTable = this.tableBuilder.closeTable();
        DomContent outputTable;
        if (prodTable.getHeight() == 0)
            outputTable = p("No samples to display.");
        else
            outputTable = prodTable.output();
        // Build the form for the next time.
        DomContent submitForm = this.createForm();
        // Write the web page.
        DomContent highlightBlock = this.getPageWriter().highlightBlock(submitForm, outputTable);
        this.getPageWriter().writePage("Threonine Production Predictions", text("Threonine Production Predictions"), highlightBlock);
    }

    /**
     * @return a form for configuring this page
     *
     * @throws IOException
     */
    private DomContent createForm() throws IOException {
        HtmlForm retVal = new HtmlForm("rna", "production", this);
        // Create the main filter.
        retVal.addFilterBox("sampleFilter", "Sample ID filtering", FRAGMENT_NAMES, FRAGMENT_TITLES, this.choices, this.filters);
        // Create the real-only flag.
        retVal.addCheckBoxWithDefault("real", "Only show samples with real values", this.realOnly);
        // Specify the prediction limits.
        retVal.addTextRow("minPred", "Minimum prediction to display", Double.toString(this.minPred));
        retVal.addTextRow("maxPred", "Maximum prediction to display", Double.toString(this.maxPred));
        // Specify the comparison column.
        List<String> comparisons = new ArrayList<String>(FRAGMENT_TITLES.length + this.choices.get(SampleId.DELETE_COL).size() + 1);
        comparisons.add("(none)");
        for (int i = 0; i < FRAGMENT_TITLES.length; i++) {
            if (i != SampleId.DELETE_COL)
                comparisons.add(FRAGMENT_TITLES[i]);
        }
        comparisons.addAll(this.choices.get(SampleId.DELETE_COL).stream().map(x -> "D" + x).collect(Collectors.toList()));
        retVal.addChoiceRow("compare", "Comparison Attribute", this.compare, comparisons);
        return retVal.output();
    }

    /**
     * @return the sortCol
     */
    public int getSortCol() {
        return sortCol;
    }

}
