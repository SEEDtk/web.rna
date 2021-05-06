/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.reports.CoreHtmlUtilities;
import org.theseed.rna.RnaData;
import org.theseed.rna.RnaFeatureData;
import org.theseed.subsystems.GenomeSubsystemTable;
import org.theseed.utils.FloatList;
import org.theseed.utils.ParseFailureException;
import org.theseed.web.rna.CellDescriptor;
import org.theseed.web.rna.ColumnDescriptor;
import org.theseed.web.rna.ColumnQualifierType;
import org.theseed.web.rna.MultiKey;
import org.theseed.web.rna.NewColumnCreator;
import org.theseed.web.rna.RowFilter;
import org.theseed.web.rna.SimpleColumnDescriptor;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This command displays a web page with RNA sequence data.  The output is a table with feature information on the
 * left and then output columns based on that feature data.  Each output column is either the expression data for
 * a single sample or differential ratios between two samples.  The user can add columns one by one and can also choose
 * a column for sorting.
 *
 * The feature information consists of the feature ID, its common gene name, and a list of subsystem IDs.  The
 * subsystem information is taken from the "rnaSubs.txt" file.
 *
 * Each column is titled with a sample name.  Sample names have multiple components separated by underscores.  These are
 * translated to spaces for display to allow wrapping.  Differential columns have two sample names separated by a colored slash.
 * The form for the next column is placed to the right of the table.
 *
 * The positional parameters, as always, are the name of the coreSEED data directory and the name of the user workspace.
 * The binary repository of the RNA data is in "fpkm.ser" in this directory.
 *
 * The specification of a column is (0) name of primary sample, (1) optional name of secondary sample.  If the second
 * column is present, then the display is differential with the primary sample in the numerator.  A list of column
 * specifications for previous columns is stored in the workspace data.
 *
 * Column specifications are kept in a cookie variable.  The variable name is "column." plus the configuration name.  The
 * configuration name cannot contain spaces or special characters.
 *
 * The command-line options are as follows.
 *
 * --sample1	name of the primary sample for the next column
 * --sample2	name of the secondary sample (optional)
 * --samplei	name of a sample to display (optional); more than one of these can be specified, and they act as
 * 				additional values to sample1
 * --sortCol	index of the column to sort on
 * --deleteCol	index of a column to delete
 * --reset		erase all of the saved columns (this automatically changes sortCol to 1)
 * --raw		display raw numbers instead of normalized results
 * --name		name of the column configuration to use
 * --cmd		command to run for new columns:  TIME1 (all times for sample 1), TIMES (matching times for both samples),
 * 				SINGLE (only one column)
 * --colFilter	type of column to use in difference filter-- DIFFERENTIAL, VALUE, or NONE
 * --ranges		comma-delimited list of range limits, from lowest to highest (maximum 3)
 * --rowFilter	rule to use for difference filter; DIFFERENT, NONE
 * --focus		if specified, the ID of a peg; the screen will scroll to that peg
 * --subsystem	subsystem to color
 *
 * @author Bruce Parrello
 *
 */
public class ColumnProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ColumnProcessor.class);
    /** RNA data repository */
    private RnaData data;
    /** subsystem data table */
    private GenomeSubsystemTable subTable;
    /** list of sample names */
    private List<String> samples;
    /** cookie file name */
    public static final String RNA_COLUMN_COOKIE_FILE = "web.rna.columns";
    /** TPM file name */
    public static final String RNA_DATA_FILE_NAME = "tpm.ser";
    /** FPKM file name */
    public static final String RNA_RAW_DATA_FILE_NAME = "fpkm.ser";
    /** column configuration variable name prefix */
    public static final String COLUMNS_PREFIX = "Columns.";
    /** number of columns before the data section */
    private static final int HEAD_COLS = 9;
    /** URL generator for column delete */
    private static final String DELETE_COL_URL_FORMAT = "/rna.cgi/columns?sortCol=%d;deleteCol=%d";
    /** definition for filtering checkboxes */
    private static final String[] CHECK_COLUMNS = new String[] { "Host strain", null, "Core Thr operon",
            null, null, null, null, "Thr operon induction"};
    /** checkbox filter ID format */
    private static final String SELECTOR_FORMAT = "sampleFilter%d";
    /** name of the sample-name datalist */
    private static final String SAMPLE_NAME_LIST = "sampleNameList";
    /** array of range limits; each array entry is the exclusive upper limit for the range */
    private double[] rangeLimits;
    /** cell descriptors for the current row */
    private List<CellDescriptor> row;
    /** cell descriptors for the range-colored elements */
    private List<CellDescriptor> coloredCells;
    /** set of columns to be range-colored */
    private BitSet coloredColumns;
    /** row filter */
    private RowFilter rowFilterObject;
    /** set of features in the focus subsystem */
    private Set<String> subFids;
    /** TRUE if we are doing baseline coloring (the default) */
    private boolean baseLineColoring;

    // COMMAND-LINE OPTIONS

    /** name of primary sample (if none, no column is added) */
    @Option(name = "--sample1", usage = "primary sample name")
    protected List<String> sample1;

    /** name of secondary sample (if none, column is not differential, if "baseline", column is sample over baseline) */
    @Option(name = "--sample2", usage = "secondary sample name")
    protected String sample2;

    /** index of sort column (0 = first data column, negative = use saved value) */
    @Option(name = "--sortCol", usage = "sort column index")
    protected int sortCol;

    /** index of column to delete (0 = first data column, negative = no deletion) */
    @Option(name = "--deleteCol", usage = "index of column to delete")
    protected int deleteCol;

    /** if specified, raw numbers will be displayed */
    @Option(name = "--raw", usage = "display raw FPKM instead of TPM")
    protected boolean rawFlag;

    /** reset flag */
    @Option(name = "--reset", usage = "if specified, existing columns are deleted")
    protected boolean resetFlag;

    /** configuration name */
    @Option(name = "--name", usage = "configuration name")
    protected String configuration;

    /** type of column filter */
    @Option(name = "--colFilter", usage = "rule for choosing columns to range-color")
    protected ColumnQualifierType colFilter;

    /** comma-delimited list of range limits */
    @Option(name = "--ranges", metaVar = "0.1,1.1,3.0", usage = "comma-delimited list of range limits")
    protected String ranges;

    /** type of row filter */
    @Option(name = "--rowFilter", usage = "rule for choosing rows to display")
    protected RowFilter.Type rowFilter;

    /** new-column strategy */
    @Option(name = "--cmd", usage = "strategy for new columns")
    protected NewColumnCreator.Type strategy;

    /** focus peg */
    @Option(name = "--focus", metaVar = "fig|511145.183.peg.640", usage = "focus gene")
    protected String focusPeg;

    /** subsystem to color */
    @Option(name = "--subsystem", metaVar = "AspaThreModu", usage = "subsystem to highlight")
    protected String subsystem;

    @Override
    protected void setWebDefaults() {
        this.sortCol = -2;
        this.deleteCol = -1;
        this.sample1 = new ArrayList<String>();
        this.sample2 = "";
        this.resetFlag = false;
        this.rawFlag = false;
        this.configuration = "Default";
        this.strategy = NewColumnCreator.Type.SINGLE;
        this.colFilter = ColumnQualifierType.NONE;
        this.ranges = "";
        this.rowFilter = RowFilter.Type.ALL;
        this.focusPeg = "";
        this.subsystem = "";
        this.baseLineColoring = true;
    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        // Read in the RNA data file.
        try {
            log.info("Loading RNA-seq data.");
            File dataFile = new File(this.getCoreDir(), (this.rawFlag ? RNA_RAW_DATA_FILE_NAME : RNA_DATA_FILE_NAME));
            this.data = RnaData.load(dataFile);
            log.info("{} samples in RNA dataset {}.", this.data.size(), dataFile);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + e.getMessage());
        }
        // Verify the samples.
        for (String samplei : this.sample1) {
            if (! samplei.isEmpty() && this.data.getColIdx(samplei) < 0)
                throw new ParseFailureException("Invalid sample name " + samplei + ".");
        }
        if (! this.sample2.isEmpty() && ! this.sample2.contentEquals("baseline") && this.data.getColIdx(this.sample2) < 0)
            throw new ParseFailureException("Invalid sample name " + this.sample2 + ".");
        // Verify the ranges.
        if (this.ranges.isEmpty()) {
            // If no range limits were specified, everything is treated as normal (range 0).
            this.rangeLimits = new double[] { Double.POSITIVE_INFINITY };
        } else {
            // Here we must verify that the range limits are ascending and that there are no more than 3.
            FloatList rangeList = new FloatList(this.ranges);
            if (rangeList.size() > 3)
                throw new ParseFailureException("No more than 3 range limits can be specified.");
            this.rangeLimits = rangeList.getValues();
            // Sort the range limits from lowest to highest.
            Arrays.sort(this.rangeLimits);
            this.baseLineColoring = false;
        }
        this.rowFilterObject = this.rowFilter.create(this);
        // If baseline coloring is in effect, we color value columns only.
        if (this.baseLineColoring)
            this.colFilter = ColumnQualifierType.VALUE;
        return true;
    }

    @Override
    protected String getCookieName() {
        return RNA_COLUMN_COOKIE_FILE;
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Get the subsystem table.
        File subFile = new File(this.getCoreDir(), "rnaSubs.txt");
        this.subTable = new GenomeSubsystemTable(subFile);
        // Create the list of samples.
        this.samples = this.data.getSamples().stream().map(x -> x.getName()).collect(Collectors.toList());
        // Build the cookie string describing all the columns.
        String cookieString = "";
        if (! this.resetFlag) {
            // Here we are not resetting, so we want to keep the old columns.
            String oldCookieString = cookies.get(COLUMNS_PREFIX + this.configuration, "");
            if (this.sample1.isEmpty() && this.strategy.requires1()) {
                // Here no columns are being added.  We add a blank column to clear the sort string.
                cookieString = ColumnDescriptor.addColumn(oldCookieString, ",");
            } else {
                NewColumnCreator creator = this.strategy.create(this.sample1, this.sample2, this.samples);
                List<String> columns = creator.getNewColumns();
                log.info("{} new columns computed.", columns.size());
                // Get a copy of the cookie string so we can update it.  The first update will delete the sort
                // information.
                cookieString = oldCookieString;
                for (String newColumn : columns)
                    cookieString = ColumnDescriptor.addColumn(cookieString, newColumn);
            }
            // Are we deleting a column?
            if (this.deleteCol >= 0) {
                cookieString = ColumnDescriptor.deleteColumn(cookieString, this.deleteCol);
            }
            // Is the sort column unspecified?
            if (this.sortCol < -1) {
                // Yes.  Extract it from the original string.
                this.sortCol = ColumnDescriptor.getSortCol(oldCookieString);
            }
        }
        // Save the columns for next time.
        cookies.put(COLUMNS_PREFIX + this.configuration, ColumnDescriptor.savecookies(cookieString, this.sortCol));
        // Split the cookie string into columns.
        ColumnDescriptor[] columns = ColumnDescriptor.parse(cookieString, this.data);
        // The two page components will be put here.
        List<DomContent> parts = new ArrayList<DomContent>(2);
        // Verify that we have a table to display.
        if (columns.length > 0) {
            // Here we have a table.  If the sort column is out of range, set to to -1.
            if (this.sortCol < 0 || this.sortCol >= columns.length)
                this.sortCol = -1;
            // Fetch the actual column for sorting.
            ColumnDescriptor sortingColumn = (this.sortCol < 0 ? null : columns[this.sortCol]);
            // Create the filtering data structures.
            this.row = new ArrayList<CellDescriptor>(columns.length);
            this.coloredCells = new ArrayList<CellDescriptor>(columns.length);
            this.coloredColumns = new BitSet(columns.length);
            // Compute the colored columns.
            for (int i = 0; i < columns.length; i++) {
                if (this.colFilter.isRangeColored(columns[i]))
                    this.coloredColumns.set(i);
            }
            // Create the column specs.
            ColSpec[] specs = new ColSpec[columns.length + HEAD_COLS];
            specs[0] = new ColSpec.Num("#");
            specs[1] = new ColSpec.Normal("peg_id");
            specs[2] = new ColSpec.Normal("gene");
            specs[3] = new ColSpec.Num("na_len");
            specs[4] = new ColSpec.Normal("subsystems");
            specs[5] = new ColSpec.Num("ar_num");
            specs[6] = new ColSpec.Normal("modulons");
            specs[7] = new ColSpec.Normal("operon");
            specs[8] = new ColSpec.Num("baseline");
            for (int i = 0; i < columns.length; i++)
                specs[i+HEAD_COLS] = this.columnSpec(columns[i], i);
            HtmlTable<MultiKey> table = new HtmlTable<>(specs);
            // Save the subsystem feature set.
            this.subFids = this.subTable.getSubFeatures(this.subsystem);
            // Now we create a row for each feature.
            for (RnaData.Row dataRow : this.data) {
                // Get the feature for this row.
                RnaFeatureData feat = dataRow.getFeat();
                // Get the sort key for this row.
                MultiKey rowKey = new MultiKey(feat, sortingColumn);
                // Build the column descriptors.
                for (int i = 0; i < columns.length; i++) {
                    double value = columns[i].getValue(feat);
                    if (! this.coloredColumns.get(i)) {
                        // Here the column is not colored.
                        this.row.add(new CellDescriptor(value, 0));
                    } else {
                        // Here the column is colored.  We need to compute the color.
                        int color = this.computeColoring(feat, value);
                        // Create the cell descriptor.
                        CellDescriptor cell = new CellDescriptor(value, color);
                        this.row.add(cell);
                        this.coloredCells.add(cell);
                    }
                }
                // Check the row filter.
                if (this.rowFilterObject.isRowDisplayable(feat)) {
                    // Create the row.
                    Row<MultiKey> tableRow = new Row<MultiKey>(table, rowKey);
                    // Put in a placeholder for the numbering column.
                    tableRow.add(0);
                    // Set up the PEG ID.  This contains a link to the feature's PATRIC page.
                    // We also need to mark it if it is the focus peg.
                    String fid = feat.getId();
                    ContainerTag fidLink = ColumnDescriptor.fidLink(fid);
                    if (fid.contentEquals(this.focusPeg))
                        fidLink.withId(FOCUS_CLASS);
                    tableRow.add(fidLink);
                    // Set up the gene ID.  if it is non-empty, we link it to a neighborhood filter.
                    String gene = feat.getGene();
                    DomContent geneHtml;
                    if (gene.isEmpty())
                        geneHtml = rawHtml("&nbsp;");
                    else {
                        String regionURL = String.format("/rna.cgi/columns?focus=%s;rowFilter=REGION;sortCol=-1", fid);
                        String regionLink = this.getPageWriter().local_url(regionURL, this.getWorkSpace());
                        geneHtml = a(gene).withHref(regionLink).withTarget("_blank");
                    }
                    tableRow.add(geneHtml);
                    tableRow.add(feat.getLocation().getLength());
                    // Now we process the subsystem column.
                    Set<GenomeSubsystemTable.SubData> subs = this.subTable.getSubsystems(feat.getId());
                    tableRow.add(this.getSubsystemList(fid, subs));
                    // Check for the highlight subsystem.
                    if (this.subFids.contains(fid))
                        tableRow.highlight(4);
                    // Next come the regulon, modulon, and operon.
                    tableRow.add(feat.getAtomicRegulon());
                    tableRow.add(StringUtils.join(feat.getiModulons(), ", "));
                    tableRow.add(feat.getOperon());
                    // Finally, the baseline.
                    tableRow.add(feat.getBaseLine());
                    // Now fill in the numbers.
                    for (int i = 0; i < columns.length; i++) {
                        CellDescriptor cell = this.row.get(i);
                        tableRow.add(cell.getValue());
                        int color = cell.getRange();
                        if (color > 0)
                            tableRow.addStyle(i + HEAD_COLS, String.format("range%d", color));
                    }
                }
                // Set up for the next row.
                this.row.clear();
                this.coloredCells.clear();
            }
            table.setIndexColumn(0);
            // Format the table and store it in the output list.
            parts.add(table.output());
        }
        // Build the forms.
        DomContent forms = buildForms(columns, cookies);
        parts.add(forms);
        // Render the web page.  We build an invisible one-row table with each component in a cell.
        DomContent assembly = this.getPageWriter().scrollBlock(table(tr().with(parts.stream().map(x -> td(x).withClass("borderless")))).withClass("borderless"));
        DomContent wrapped = this.getPageWriter().highlightBlock(assembly);
        // Write the page.
        this.getPageWriter().writePage("RNA Expression Data", text("RNA Expression Data"), wrapped);
    }

    /**
     * This method computes the color for the specified cell value.  This depends on the coloring type.
     *
     * @param feat		feature for which the cell is an expression
     * @param value		value of expression
     *
     * @return the color index
     */
    private int computeColoring(RnaFeatureData feat, double value) {
        int retVal = 0;
        if (this.baseLineColoring) {
            // Here we have baseline coloring.
            double base = feat.getBaseLine();
            if (value <= 0.5 * base)
                retVal = 3;
            else if (value >= 2 * base)
                retVal = 1;
        } else {
            // Here we have range-based coloring.
            while (retVal < this.rangeLimits.length && value > this.rangeLimits[retVal]) retVal++;
        }
        return retVal;
    }

    /**
     * This method creates the subsystem ID link list.  Each ID shows a tooltip describing the subsystem and
     * links to the subsystem page.
     *
     * @param fid	ID of the feature whose subsystems are being listed
     * @param subs	list of subsystem descriptors
     *
     * @return HTML listing the subsystems connected to the specified feature
     */
    private DomContent getSubsystemList(String fid, Collection<GenomeSubsystemTable.SubData> subs) {
        DomContent retVal;
        if (subs == null)
            retVal = rawHtml("&nbsp;");
        else {
            // Here we have actual subsystems to list.
            List<DomContent> linkList = new ArrayList<DomContent>(subs.size());
            for (GenomeSubsystemTable.SubData sub : subs) {
                String text = sub.getId();
                String tooltip = sub.getDescription();
                // Set up a URL to focus on the subsystem.
                String url = String.format("/rna.cgi/columns?subsystem=%s;focus=%s", text, fid);
                url = this.getPageWriter().local_url(url, this.getWorkSpace());
                linkList.add(a(text).withTitle(tooltip).withHref(url));
            }
            retVal = rawHtml(linkList.stream().map(x -> x.render()).collect(Collectors.joining(", ")));
        }
        return retVal;
    }

    /**
     * @return the column specification for the specified data column descriptor
     *
     * @param columnDescriptor	descriptor specifying this column
     * @param idx				index of this column
     */
    private ColSpec columnSpec(ColumnDescriptor columnDescriptor, int idx) {
        // Compute the index of the new sort column if this column is deleted.
        int newSort;
        if (this.sortCol < idx)
            newSort = this.sortCol;
        else if (this.sortCol == idx)
            newSort = 0;
        else
            newSort = this.sortCol - 1;
        // Form a URL for deleting this column.
        String url = this.getPageWriter().local_url(String.format(DELETE_COL_URL_FORMAT,
                newSort, idx), this.getWorkSpace());
        ContainerTag button = a(button("Delete")).withHref(url);
        DomContent buttons;
        if (columnDescriptor instanceof SimpleColumnDescriptor) {
            ContainerTag button2 = button("Primary").attr("onclick",
                    "storeVal('sample1', '" + columnDescriptor.getSample1() + "');");
            ContainerTag button3 = button("Optional").attr("onclick",
                    "storeVal('sample2', '" + columnDescriptor.getSample1() + "');");
            buttons = join(button, button2, button3);
        } else
            buttons = button;
        DomContent title = join(columnDescriptor.getTitle(), buttons);
        ColSpec retVal = new ColSpec.Fraction(title);
        retVal.setTip(columnDescriptor.getTooltip());
        return retVal;
    }

    /**
     * @return HTML for the various forms required
     *
     * @param columns	columns currently present
     * @param cookies	cookie file containing the column configurations
     *
     * @throws IOException
     */
    private DomContent buildForms(ColumnDescriptor[] columns, CookieFile cookies) throws IOException {
        // Here we must build the filtering checkboxes.
        HtmlTable<Key.Null> filterTable = this.buildFilters();
        // Now we build the main form.
        HtmlForm form = new HtmlForm("rna", "columns", this);
        // Get the list of samples and add the null selection.
        List<String> samples0 = new ArrayList<String>(this.samples);
        samples0.add("");
        // Create the sample data list.
        form.createDataList(samples0, SAMPLE_NAME_LIST);
        // Create the sample selectors.
        form.addSearchRow("sample1", "Primary RNA Sampling", "", SAMPLE_NAME_LIST);
        form.addSearchRow("sample2", "Optional Denominator Sample (or \"baseline\")", "", SAMPLE_NAME_LIST);
        // Add the sort column specifier.
        List<String> sortCols = Arrays.stream(columns).map(x -> x.getTitleString()).collect(Collectors.toList());
        String defaultCol = (sortCols.size() > 0 && this.sortCol >= 0 ? sortCols.get(this.sortCol) : null);
        form.addChoiceIndexedRow("sortCol", "Column for sorting", defaultCol, sortCols, "Sort by Location");
        // Add the strategy.
        form.addEnumRow("cmd", "New-column Strategy", NewColumnCreator.Type.SINGLE, NewColumnCreator.Type.values());
        // Add the checkboxes.
        form.addCheckBoxWithDefault("reset", "Remove existing columns", false);
        form.addCheckBoxWithDefault("raw", "display raw FPKM numbers", this.rawFlag);
        // Add the coloring controls.
        form.addTextRow("ranges", "Comma-delimited list of range-coloring limits (no spaces)", this.ranges);
        form.addEnumRow("rowFilter", "Row-filtering rule", this.rowFilter, RowFilter.Type.values());
        form.addEnumRow("colFilter", "Range-coloring rule", this.colFilter, ColumnQualifierType.values());
        // Now the focus peg and the subsystem chooser.
        form.addTextRow("focus", "Focus Peg", this.focusPeg);
        form.addChoiceRow("subsystem", "Subsystem to highlight", this.subsystem, this.subTable.getAllSubsystems(), "");
        // Add a hidden field to maintain the configuration name.
        form.addHidden("name", this.configuration);
        // Now create the load form.
        HtmlForm lForm = new HtmlForm("rna", "columns", this);
        // Get a list of the existing configuration names.
        List<String> configs = getConfigurations(cookies);
        // Build the form.
        lForm.addChoiceRow("name", "Configuration to Load", this.configuration, configs);
        lForm.setTarget("_blank");
        // Create the save form.
        HtmlForm sForm = new HtmlForm("rna", "saveCols", this);
        sForm.addHidden("old", this.configuration);
        sForm.addHidden("env", "IFRAME");
        sForm.addTextRow("name", "New Configuration to Create", "");
        sForm.setTarget("saveResult");
        // Create the result frame for the save form.
        ContainerTag iFrame = iframe().withName("saveResult").withStyle("height: 4em; width: 100%;");
        // Get a link to the sample summary page.
        String metaLink = this.getPageWriter().local_url("/rna.cgi/meta", this.getWorkSpace());
        DomContent metaLinkHtml = a("Display Summary of Samples").withHref(metaLink).withTarget("_blank");
        String manageLink = this.getPageWriter().local_url("/rna.cgi/manage", this.getWorkSpace());
        DomContent manageLinkHtml = a("Manage Saved Configurations").withHref(manageLink);
        DomContent retVal = div(h2(metaLinkHtml), h2("Add New Column / Configure").withClass("form"),
                filterTable.output(), form.output(),
                h2("Save Configuration").withClass("form"), sForm.output(), iFrame,
                h2("Load Configuration").withClass("form"), lForm.output(), h2(manageLinkHtml)
                ).withClass("center");
        return retVal;
    }

    /**
     * @return a table containing checkbox filters for the selection list
     */
    private HtmlTable<Key.Null> buildFilters() {
        // Create a list of sets for the different possible filters.
        List<Set<String>> filterSets = new ArrayList<Set<String>>();
        int partLen = 0;
        for (String sample : this.samples) {
            String[] parts = StringUtils.split(sample, '_');
            if (parts.length > partLen) partLen = parts.length;
            while (filterSets.size() < parts.length)
                filterSets.add(new TreeSet<String>());
            for (int i = 0; i < parts.length; i++)
                filterSets.get(i).add(parts[i]);
        }
        // Build the onclick event.
        List<String> selectors = IntStream.range(0, CHECK_COLUMNS.length).filter(i -> CHECK_COLUMNS[i] != null)
                .mapToObj(i -> String.format(SELECTOR_FORMAT, i)).collect(Collectors.toList());
        String onclick = "fixList('" + SAMPLE_NAME_LIST + "', " + Integer.toString(partLen) + ", '"
                + StringUtils.join(selectors, "', '") + "');";
        // Now we create the filter table.  We put in one row for each part in the specification.
        HtmlTable<Key.Null> retVal = new HtmlTable<Key.Null>(new ColSpec.Normal("Field"), new ColSpec.Normal("Filters"));
        for (int i = 0; i < CHECK_COLUMNS.length; i++) {
            if (CHECK_COLUMNS[i] != null) {
                // Here we have a filtering item of interest.
                Object[] checkboxes = filterSets.get(i).stream().map(x -> CoreHtmlUtilities.checkBox(x, x, true, onclick))
                        .toArray();
                DomContent checks = div(join(checkboxes)).withId(String.format(SELECTOR_FORMAT, i));
                new Row<Key.Null>(retVal, Key.NONE).add(CHECK_COLUMNS[i]).add(checks);
            }
        }
        return retVal;
    }

    /**
     * @return a list of the configuration names
     *
     * @param cookies	cookie file containing the configurations
     */
    public static List<String> getConfigurations(CookieFile cookies) {
        return Arrays.stream(cookies.getKeys()).filter(x -> StringUtils.startsWith(x, COLUMNS_PREFIX))
                .map(x -> StringUtils.removeStart(x, COLUMNS_PREFIX)).sorted().collect(Collectors.toList());
    }

    /**
     * @return the row for the current focus peg
     */
    public RnaData.Row getFocus() {
        return this.data.getRow(this.focusPeg);
    }

    /**
     * @return the cell descriptors for the cells with coloring in the current row
     */
    public List<CellDescriptor> getColoredCells() {
        return this.coloredCells;
    }

    /**
     * @return the set of features in the focus subsystem
     */
    public Set<String> getSubFids() {
        return this.subFids;
    }

    /**
     * @return the subsystem table
     */
    public GenomeSubsystemTable getSubTable() {
        return this.subTable;
    }

}
