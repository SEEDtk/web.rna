/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;
import org.theseed.samples.SampleId;
import org.theseed.web.rna.IProductionTable;
import org.theseed.web.rna.ProductionCompareTable;
import org.theseed.web.rna.ProductionDeleteTable;
import org.theseed.web.rna.ProductionInsertTable;
import org.theseed.web.rna.ProductionDisplayTable;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.DomContentJoiner;

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
 * --saved		name of a configuration (or 1 for default): use saved parameter values for filters (no filters may be specified)
 * --store		name of a configuration in which to store the current one
 * --source		name of prediction file to load
 * --max		maxmimum number of table rows to display
 * --first		number (0-based) of first table data row to display
 * --actual		sort by actual production instead of predicted (where present)
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
    public static final String[] FRAGMENT_NAMES = FRAGMENT_TITLES;
    /** prefix for configuration cookie file names */
    public static final String RNA_PRODUCTION = "rna.production.";
    /** pattern for configuration file names */
    public static final Pattern RNA_PRODUCTION_CONFIG = Pattern.compile("_rna\\.production\\.([^\\.]+)\\.cookie\\.tbl");
    /** production table builder */
    private IProductionTable tableBuilder;
    /** configuration message */
    private String configMessage;
    /** map of source types to source files */
    private Map<String, String> sourceMap;
    /** name of current configuration ("1" for the default) */
    private String configName;

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
    protected String restoreFilters;

    /** cookie file in which to store configuration */
    @Option(name = "--store", usage = "cookie file in which to store configuration")
    protected String storeConfig;

    /** column to sort on (if doing a comparison) */
    @Option(name = "--sortCol", usage = "index of column to sort on")
    protected int sortCol;

    /** TRUE if we should sort by actual instead of predicted value (if not doing a comparison) */
    @Option(name = "--actual", usage = "if specified, the output will be sorted by actual instead of predicted value")
    protected boolean actual;

    /** file of predictions to load */
    @Option(name = "--source", usage = "name of file to load")
    protected String source;

    /** maximum number of samples to display */
    @Option(name = "--max", usage = "maximum number of samples to show")
    protected int maxSamples;

    /** number (0-based) of first sample to display */
    @Option(name = "--first", usage = "number (0-based) of first sample to display")
    protected int firstSample;

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
        this.restoreFilters = null;
        this.sortCol = 1;
        this.storeConfig = null;
        this.maxSamples = 500;
        this.firstSample = 0;
        this.source = "thrall.production.tbl";
        this.actual = false;
    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        if (this.minPred > this.maxPred)
            throw new ParseFailureException("Minimum predicted value is greater than maximum.");
        // Save the filters.
        this.filters = Arrays.asList(this.f1, this.f2, this.f3, this.f4, this.f5, this.f6, this.f7, this.f8);
        // Initialize the choice list to empty sets all across the board.
        this.choices = IntStream.range(0, filters.size()).mapToObj(x -> new TreeSet<String>()).collect(Collectors.toList());
        // Get the map of source types to files.
        this.sourceMap = TabbedLineReader.readMap(new File(this.getCoreDir(), "map.production.tbl"), "description", "value");
        if (! this.sourceMap.values().contains(this.source))
            throw new FileNotFoundException("Invalid data source specified.");
        return true;
    }

    @Override
    protected String getCookieName() {
        return HtmlForm.formCookieName("rna", "production");
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Save and/or restore the form data.
        if (this.restoreFilters == null) {
            // Here we have to save the new form data.
            for (int i = 0; i < FRAGMENT_NAMES.length; i++) {
                Collection<String> filter = this.filters.get(i);
                cookies.put(FRAGMENT_NAMES[i], StringUtils.join(filter, ','));
            }
            cookies.put("realOnly", this.realOnly);
            cookies.put("minPred", this.minPred);
            cookies.put("maxPred", this.maxPred);
            cookies.put("compare", this.compare);
            cookies.put("max", this.maxSamples);
            cookies.put("source", this.source);
            cookies.put("actual", this.actual);
            cookies.flush();
            if (this.storeConfig == null) {
                this.configMessage = "Default configuration in use.";
                this.configName = "1";
            } else if (badConfigName(this.storeConfig)) {
                this.configMessage = "Invalid characters in configuration name.  Configuration not saved.";
            } else {
                // Here we want to save the configuration.
                File saveName = CookieFile.computeFile(this.getWorkSpaceDir(), getConfigName(this.storeConfig));
                cookies.flush(saveName);
                this.configMessage = "Configuration saved to " + this.storeConfig + ".";
                this.configName = this.storeConfig;
            }
        } else if (this.restoreFilters.contentEquals("1")) {
            // Here we have to restore the old form data.
            restoreParmsFromCookieFile(cookies);
            this.configMessage = "Configuration restored from saved default.";
            this.configName = "1";
        } else {
            // Here we have to restore the old form data from an alternate cookie file.
            CookieFile otherCookies = new CookieFile(this.getWorkSpaceDir(), getConfigName(this.restoreFilters));
            restoreParmsFromCookieFile(otherCookies);
            cookies.flush();
            this.configMessage = "Configuration restored from \"" + this.restoreFilters + "\".";
            this.configName = this.restoreFilters;
        }
        // Compute the type of output table from the comparison string.
        int compareIdx = ArrayUtils.indexOf(FRAGMENT_TITLES, this.compare);
        if (compareIdx >= 0)
            this.tableBuilder = new ProductionCompareTable(this, compareIdx, this.filters.get(compareIdx));
        else if (this.compare.charAt(0) == 'D')
            this.tableBuilder = new ProductionDeleteTable(this, this.compare.substring(1));
        else if (this.compare.charAt(0) == 'I')
            this.tableBuilder = new ProductionInsertTable(this, this.compare.substring(1));
        else
            this.tableBuilder = new ProductionDisplayTable(this, this.actual);
        // Insure delete-nothing is a choice for the insert and delete columns.
        this.choices.get(SampleId.DELETE_COL).add("000");
        this.choices.get(SampleId.INSERT_COL).add("000");
        // Read the production file.
        File prodFile = new File(this.getCoreDir(), this.source);
        try (TabbedLineReader prodStream = new TabbedLineReader(prodFile)) {
            int sampleCol = prodStream.findField("sample_id");
            int predCol = prodStream.findField("predicted");
            int actualCol = prodStream.findField("production");
            int growthCol = prodStream.findField("density");
            Iterator<TabbedLineReader.Line> iter = prodStream.iterator();
            while (iter.hasNext()) {
                TabbedLineReader.Line line = iter.next();
                // Determine the current sample.
                String sampleId = line.get(sampleCol);
                SampleId sample = new SampleId(sampleId);
                // Update the choice lists and determine if we are keeping this sample.
                boolean keep = true;
                for (int i = 0; i < FRAGMENT_TITLES.length; i++) {
                    if (i == SampleId.DELETE_COL) {
                        // For deletes, we check for the nothing case, and otherwise insure the
                        // selected choice is in the filtered items list.
                        Set<String> mods = sample.getDeletes();
                        keep = processChoiceColumn(keep, i, mods);
                    } else if (i == SampleId.INSERT_COL) {
                        // Inserts are similar to deletes.
                        Set<String> mods = sample.getInserts();
                        keep = processChoiceColumn(keep, i, mods);
                    } else {
                        // Here we have a normal fragment.
                        String fragment = sample.getFragment(i);
                        this.choices.get(i).add(fragment);
                        if (! this.filters.get(i).contains(fragment))
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
        DomContent summary;
        if (prodTable.getHeight() == 0) {
            summary = p("No samples to display.");
            outputTable = p("");
        } else {
            summary = this.tableBuilder.getSummary();
            outputTable = prodTable.output(this.firstSample, this.maxSamples);
        }
        // Build the form for the next time.
        DomContent submitForm = this.createForm();
        // Write the web page.
        DomContent paginator = this.computePaginator(prodTable);
        DomContent highlightBlock = this.getPageWriter().highlightBlock(submitForm, summary, paginator, outputTable, paginator);
        this.getPageWriter().writePage("Threonine Production Predictions", text("Threonine Production Predictions"), highlightBlock);
    }

    /**
     * Compare a set of inserts/deletes to the appropriate filters.
     *
     * @param keep		incoming keep indicator
     * @param i			column index
     * @param mods		set of inserts/deletes
     *
     * @return TRUE if the sample should be kept, else FALSE
     */
    public boolean processChoiceColumn(boolean keep, int i, Set<String> mods) {
        Collection<String> filter = this.filters.get(i);
        this.choices.get(i).addAll(mods);
        if (mods.isEmpty()) {
            if (! filter.contains("000"))
                keep = false;
        } else if (filter.stream().allMatch(x -> ! mods.contains(x)))
            keep = false;
        return keep;
    }

    /**
     * @return the cookie file name for a configuration
     *
     * @param configName	configuration name whose file is desired
     */
    public static String getConfigName(String configName) {
        return RNA_PRODUCTION + configName;
    }

    /**
     * @return TRUE if the specified configuration name is invalid
     *
     * @param configName	configuration name to validate
     */
    public static boolean badConfigName(String configName) {
        return StringUtils.containsAny(configName, " \\//.,?*&<>|");
    }

    /**
     * Restore all the parameters from a cookie file.
     *
     * @param cookies	source cookie file to restore
     */
    protected void restoreParmsFromCookieFile(CookieFile cookies) {
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
        this.source = cookies.get("source", "thrall.production.tbl");
        this.actual = cookies.get("actual", false);
        this.maxSamples = cookies.get("max", Integer.MAX_VALUE);
    }

    /**
     * @return a form for configuring this page
     *
     * @throws IOException
     */
    private DomContent createForm() throws IOException {
        HtmlForm retVal = new HtmlForm("rna", "production", this);
        // Add a hidden row for the start position.
        retVal.addHidden("first", "0");
        // Create the main filter.
        retVal.addFilterBox("sampleFilter", "Sample ID filtering (click on column head to set/clear a column)", FRAGMENT_NAMES, FRAGMENT_TITLES, this.choices, this.filters);
        // Specify the maximum number of rows to display.
        retVal.addIntRow("max", "Maximum number of samples to show", this.maxSamples, 10, Integer.MAX_VALUE);
        // Create the real-only flag.
        retVal.addCheckBoxWithDefault("real", "Only show samples with real values", this.realOnly);
        // Create the actual-sort flag.
        retVal.addCheckBoxWithDefault("actual", "Sort by actual values before predicted", this.actual);
        // Specify the prediction limits.
        retVal.addTextRow("minPred", "Minimum prediction to display", Double.toString(this.minPred));
        retVal.addTextRow("maxPred", "Maximum prediction to display", Double.toString(this.maxPred));
        // Specify the comparison column.
        List<String> comparisons = new ArrayList<String>(FRAGMENT_TITLES.length + this.choices.get(SampleId.DELETE_COL).size()
                + this.choices.get(SampleId.INSERT_COL).size() + 1);
        comparisons.add("(none)");
        for (int i = 0; i < FRAGMENT_TITLES.length; i++) {
            if (i != SampleId.DELETE_COL && i != SampleId.INSERT_COL)
                comparisons.add(FRAGMENT_TITLES[i]);
        }
        comparisons.addAll(this.choices.get(SampleId.INSERT_COL).stream().map(x -> "I" + x).collect(Collectors.toList()));
        comparisons.addAll(this.choices.get(SampleId.DELETE_COL).stream().map(x -> "D" + x).collect(Collectors.toList()));
        retVal.addChoiceRow("compare", "Comparison Attribute", this.compare, comparisons);
        // Specify the source map.
        retVal.addMapRow("source", "Predictions to Display", this.sourceMap, this.source);
        // Add the configuration status message.
        retVal.addMessageRow(p(join(this.configMessage, this.commandLink("Manage configurations", "rna", "predManage"))));
        // Add the save options.
        retVal.addTextRow("store", "Save this configuration", "");
        return retVal.output();
    }

    /**
     * @return the sortCol
     */
    public int getSortCol() {
        return sortCol;
    }

    /**
     * @return a pagination control for the master table
     *
     * @param mainTable		main HTML table to paginate
     */
    private ContainerTag computePaginator(HtmlTable<? extends Key> mainTable) {
        ContainerTag retVal = p();
        if (this.firstSample > 0 || this.maxSamples < mainTable.getHeight()) {
            // Here we are showing only part of the table, so we need a real paginator.
            // We will show five pages with the current one in the center, plus the first and last pages.
            // Start with the number of pages.  The page numbers are 0-based; we fix them when we display them.
            int n = mainTable.getHeight();
            int totalPages = (n + this.maxSamples - 1) / this.maxSamples;
            int lastPage = totalPages - 1;
            int currPage = this.firstSample / this.maxSamples;
            int midPage0 = currPage - 2;
            int midPage1 = currPage + 2;
            // Insure we are not trying to display past either end.
            if (midPage0 < 0) {
                midPage0 = 0;
                midPage1 = 4;
            }
            if (midPage1 > lastPage)
                midPage1 = lastPage;
            // Compute our base URL.
            String url = this.getPageWriter().local_url("/rna.cgi/production?saved=" + this.configName, this.getWorkSpace());
            // We accumulate the page hyperlinks in here.
            List<Object> pages = new ArrayList<Object>(10);
            if (midPage0 > 0) {
                // Here we need a first page.
                pages.add(this.pageUrl(0, url));
                // If there is a gap, put in dots.
                if (midPage0 > 1)
                    pages.add("...");
            }
            // Add the middle pages.
            for (int p = midPage0; p <= midPage1; p++)
                pages.add(pageUrl(p, url));
            // Now we handle the last page.
            if (lastPage > midPage1) {
                // If there is a gap, put in dots.
                if (midPage1 + 1 < lastPage)
                    pages.add("...");
                pages.add(pageUrl(lastPage, url));
            }
            // Join all the pieces together.
            Object[] pieces = pages.stream().toArray();
            retVal.with(DomContentJoiner.join(" ", false, pieces));
        }
        return retVal;
    }

    /**
     * @return the hyperlink for a specified table page
     *
     * @param p		page index (0-based)
     * @param url	base url
     */
    private ContainerTag pageUrl(int p, String url) {
        return a(String.format("%d", p+1)).withHref(String.format("%s;first=%d", url, p * this.maxSamples));
    }
}
