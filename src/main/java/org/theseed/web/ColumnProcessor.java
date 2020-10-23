/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.reports.ColSpec;
import org.theseed.reports.HtmlForm;
import org.theseed.reports.HtmlTable;
import org.theseed.reports.Key;
import org.theseed.rna.RnaData;
import org.theseed.web.rna.ColumnDescriptor;

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
 * --reset		erase all of the saved columns (this automatically changes sortCol to 1)
 * --raw		display raw numbers instead of normalized results
 * --name		name of the column configuration to use
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
    /** cookie file name */
    public static final String RNA_COLUMN_COOKIE_FILE = "web.rna.columns";
    /** TPM file name */
    public static final String RNA_DATA_FILE_NAME = "fpkm.ser";
    /** FPKM file name */
    public static final String RNA_RAW_DATA_FILE_NAME = "fpkm.raw.ser";
    /** column configuration variable name prefix */
    public static final String COLUMNS_PREFIX = "Columns.";


    // COMMAND-LINE OPTIONS

    /** name of primary sample (if none, no column is added) */
    @Option(name = "--sample1", usage = "primary sample name")
    protected String sample1;

    /** name of secondary sample (if none, column is not differential) */
    @Option(name = "--sample2", usage = "secondary sample name")
    protected String sample2;

    /** index of sort column (0 = first data column) */
    @Option(name = "--sortCol", usage = "sort column index")
    protected int sortCol;

    /** if specified, raw numbers will be displayed */
    @Option(name = "--raw", usage = "display raw FPKM instead of TPM")
    protected boolean rawFlag;

    /** reset flag */
    @Option(name = "--reset", usage = "if specified, existing columns are deleted")
    protected boolean resetFlag;

    /** configuration name */
    @Option(name = "--name", usage = "configuration name")
    protected String configuration;

    @Override
    protected void setWebDefaults() {
        this.sortCol = -1;
        this.sample1 = "";
        this.sample2 = "";
        this.resetFlag = false;
        this.rawFlag = false;
        this.configuration = "Default";
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
        String cookieString = "";
        if (! this.resetFlag) {
            // Here we are not resetting, so we want to keep the old columns.
            String oldCookieString = cookies.get(COLUMNS_PREFIX + this.configuration, "");
            // Add the next column.  This deletes the sort information.
            cookieString = ColumnDescriptor.addColumn(oldCookieString, this.sample1 + "," + this.sample2);
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
            ColSpec[] specs = new ColSpec[columns.length + 2];
            specs[0] = new ColSpec.Normal("peg_id");
            specs[1] = new ColSpec.Normal("gene");
            for (int i = 0; i < columns.length; i++) {
                specs[i+2] = new ColSpec.Fraction(columns[i].getTitle());
                specs[i+2].setTip(columns[i].getTooltip());
            }
            HtmlTable<Key.RevFloat> table = new HtmlTable<>(specs);
            // Now we create a row for each feature.
            for (RnaData.Row dataRow : this.data) {
                // Get the feature for this row.
                RnaData.FeatureData feat = dataRow.getFeat();
                // Get the sort key for this row.
                Key.RevFloat rowKey = new Key.RevFloat(columns[this.sortCol].getValue(feat));
                // Create the row.
                HtmlTable<Key.RevFloat>.Row tableRow = table.new Row(rowKey);
                tableRow.add(ColumnDescriptor.fidLink(feat.getId()));
                tableRow.add(feat.getGene());
                // Now fill in the numbers.
                for (int i = 0; i < columns.length; i++)
                    tableRow.add(columns[i].getValue(feat));
            }
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
     * @return HTML for the various forms required
     *
     * @param columns	columns currently present
     * @param cookies	cookie file containing the column configurations
     *
     * @throws IOException
     */
    private DomContent buildForms(ColumnDescriptor[] columns, CookieFile cookies) throws IOException {
        // First we build the main form.
        HtmlForm form = new HtmlForm("rna", "columns", this);
        // Get the list of samples and add the sample selectors.
        List<String> samples = this.data.getSamples().stream().map(x -> x.getName()).collect(Collectors.toList());
        samples.add("");
        form.addChoiceBox("sample1", "Primary RNA Sampling", "", samples);
        form.addChoiceBox("sample2", "Optional Denominator Sample", "", samples);
        // Add the sort column specifier.
        List<String> sortCols = Arrays.stream(columns).map(x -> x.getTitle()).collect(Collectors.toList());
        String defaultCol = (sortCols.size() > 0 ? sortCols.get(this.sortCol) : null);
        form.addChoiceBoxIndexed("sortCol", "Column for sorting", defaultCol, sortCols);
        // Add the checkboxes.
        form.addCheckBoxWithDefault("reset", "Remove existing columns", false);
        form.addCheckBoxWithDefault("raw", "display raw FPKM numbers", this.rawFlag);
        // Add a hidden field to maintain the configuration name.
        form.addHidden("name", this.configuration);
        // Now create the load form.
        HtmlForm lForm = new HtmlForm("rna", "columns", this);
        // Get a list of the existing configuration names.
        List<String> configs = Arrays.stream(cookies.getKeys()).filter(x -> StringUtils.startsWith(x, COLUMNS_PREFIX))
                .map(x -> StringUtils.removeStart(x, COLUMNS_PREFIX)).sorted().collect(Collectors.toList());
        // Build the form.
        lForm.addChoiceBox("name", "Configuration to Load", this.configuration, configs);
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
        DomContent retVal = div(h2(metaLinkHtml), h2("Add New Column / Configure").withClass("form"), form.output(),
                h2("Save Configuration").withClass("form"), sForm.output(),
                iFrame, h2("Load Configuration").withClass("form"), lForm.output()
                ).withClass("center");
        return retVal;
    }


}
