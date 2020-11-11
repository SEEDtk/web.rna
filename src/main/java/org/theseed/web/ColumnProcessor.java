/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.reports.ColSpec;
import org.theseed.reports.CoreHtmlUtilities;
import org.theseed.reports.HtmlForm;
import org.theseed.reports.HtmlTable;
import org.theseed.reports.Key;
import org.theseed.rna.RnaData;
import org.theseed.web.rna.ColumnDescriptor;
import org.theseed.web.rna.NewColumnCreator;
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
 * --sortCol	index of the column to sort on
 * --deleteCol	index of a column to delete
 * --reset		erase all of the saved columns (this automatically changes sortCol to 1)
 * --raw		display raw numbers instead of normalized results
 * --name		name of the column configuration to use
 * --cmd		command to run for new columns:  TIME1 (all times for sample 1), TIMES (matching times for both samples),
 * 				SINGLE (only one column)
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
    private static final int HEAD_COLS = 4;
    /** URL generator for column delete */
    private static final String DELETE_COL_URL_FORMAT = "/rna.cgi/columns?sortCol=%d;deleteCol=%d";
    /** definition for filtering checkboxes */
    private static final String[] CHECK_COLUMNS = new String[] { "Host strain", null, "Core Thr operon",
            null, null, null, null, "Thr operon induction"};
    /** checkbox filter ID format */
    private static final String SELECTOR_FORMAT = "sampleFilter%d";
    /** name of the sample-name datalist */
    private static final String SAMPLE_NAME_LIST = "sampleNameList";


    // COMMAND-LINE OPTIONS

    /** name of primary sample (if none, no column is added) */
    @Option(name = "--sample1", usage = "primary sample name")
    protected String sample1;

    /** name of secondary sample (if none, column is not differential) */
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

    /** new-column strategy */
    @Option(name = "--cmd", usage = "strategy for new columns")
    protected NewColumnCreator.Type strategy;

    @Override
    protected void setWebDefaults() {
        this.sortCol = -1;
        this.deleteCol = -1;
        this.sample1 = "";
        this.sample2 = "";
        this.resetFlag = false;
        this.rawFlag = false;
        this.configuration = "Default";
        this.strategy = NewColumnCreator.Type.SINGLE;
    }

    @Override
    protected boolean validateWebParms() throws IOException {
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
        if (! this.sample1.isEmpty() && this.data.getColIdx(this.sample1) < 0)
            throw new IllegalArgumentException("Invalid sample name " + this.sample1 + ".");
        if (! this.sample2.isEmpty() && this.data.getColIdx(this.sample2) < 0)
            throw new IllegalArgumentException("Invalid sample name " + this.sample2 + ".");
        return true;
    }

    @Override
    protected String getCookieName() {
        return RNA_COLUMN_COOKIE_FILE;
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Create the list of samples.
        this.samples = this.data.getSamples().stream().map(x -> x.getName()).collect(Collectors.toList());
        // Build the cookie string describing all the columns.
        String cookieString = "";
        if (! this.resetFlag) {
            // Here we are not resetting, so we want to keep the old columns.
            String oldCookieString = cookies.get(COLUMNS_PREFIX + this.configuration, "");
            if (this.sample1.isEmpty()) {
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
            if (this.sortCol < 0) {
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
            // Here we have a table.  If the sort column is out of range, set it back to 0.
            if (this.sortCol < 0 || this.sortCol >= columns.length)
                this.sortCol = 0;
            // Create the column specs.
            ColSpec[] specs = new ColSpec[columns.length + HEAD_COLS];
            specs[0] = new ColSpec.Num("#");
            specs[1] = new ColSpec.Normal("peg_id");
            specs[2] = new ColSpec.Normal("gene");
            specs[3] = new ColSpec.Num("na_len");
            for (int i = 0; i < columns.length; i++)
                specs[i+HEAD_COLS] = this.columnSpec(columns[i], i);
            HtmlTable<Key.RevRatio> table = new HtmlTable<>(specs);
            // Now we create a row for each feature.
            for (RnaData.Row dataRow : this.data) {
                // Get the feature for this row.
                RnaData.FeatureData feat = dataRow.getFeat();
                // Get the sort key for this row.
                Key.RevRatio rowKey = columns[this.sortCol].getKey(feat);
                // Create the row.
                HtmlTable<Key.RevRatio>.Row tableRow = table.new Row(rowKey);
                tableRow.add(0);
                tableRow.add(ColumnDescriptor.fidLink(feat.getId()));
                tableRow.add(feat.getGene());
                tableRow.add(feat.getLocation().getLength());
                // Now fill in the numbers.
                for (int i = 0; i < columns.length; i++)
                    tableRow.add(columns[i].getValue(feat));
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
        form.addSearchRow("sample2", "Optional Denominator Sample", "", SAMPLE_NAME_LIST);
        // Add the sort column specifier.
        List<String> sortCols = Arrays.stream(columns).map(x -> x.getTitle()).collect(Collectors.toList());
        String defaultCol = (sortCols.size() > 0 ? sortCols.get(this.sortCol) : null);
        form.addChoiceIndexedRow("sortCol", "Column for sorting", defaultCol, sortCols);
        // Add the strategy.
        form.addEnumRow("cmd", "New-column Strategy", NewColumnCreator.Type.SINGLE, NewColumnCreator.Type.values());
        // Add the checkboxes.
        form.addCheckBoxWithDefault("reset", "Remove existing columns", false);
        form.addCheckBoxWithDefault("raw", "display raw FPKM numbers", this.rawFlag);
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
                retVal.new Row(Key.NONE).add(CHECK_COLUMNS[i]).add(checks);
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


}
