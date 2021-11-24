/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.Option;
import org.theseed.reports.PageWriter;
import org.theseed.rna.RnaData;
import org.theseed.web.rna.ColumnDescriptor;
import org.theseed.web.rna.RnaDataType;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This command displays the meta-data for the RNA sequence samples.  This data is encoded in the sample records
 * of the main RNA data file ("tpm.ser" in the CoreSEED directory).
 *
 * The positional parameters, as always, are the name of the coreSEED data directory and the name of the user workspace.
 *
 * The command-line options are as follows.
 *
 * --name	name of the current column configuration
 *
 * @author Bruce Parrello
 *
 */
public class RnaMetaProcessor extends WebProcessor {

    // FIELDS
    /** length of E coli genome */
    public static final int GENOME_LEN = 4638920;

    // COMMAND-LINE OPTIONS

    /** configuration name */
    @Option(name = "--name", usage = "configuration name")
    protected String configuration;

    /** if specified, only quality samples are shown */
    @Option(name = "--all", usage = "show low-quality strains")
    protected boolean allFlag;

    @Override
    protected void setWebDefaults() {
        this.configuration = "Default";
        this.allFlag = false;
    }

    @Override
    protected boolean validateWebParms() throws IOException {
        return true;
    }

    @Override
    protected String getCookieName() {
        return ColumnProcessor.RNA_COLUMN_COOKIE_FILE;
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Get the database type from the cookie string.
        String oldCookieString = cookies.get(ColumnProcessor.COLUMNS_PREFIX + this.configuration, "");
        RnaDataType cookieType = ColumnDescriptor.getDbType(oldCookieString);
        // This is a very simple web page:  we just build a table from the sample records.
        File dataFile = new File(this.getCoreDir(), cookieType.getFileName());
        RnaData data = RnaData.load(dataFile);
        log.info("{} samples in RNA dataset {}.", data.size(), dataFile);
        // Create a table for the meta-data.
        HtmlTable<Key.Null> table = new HtmlTable<>(new ColSpec.Normal("sel"), new ColSpec.Normal("sample_id"),
                new ColSpec.Fraction("Thr g/l"), new ColSpec.Num("OD"), new ColSpec.Normal("original_name"),
                new ColSpec.Num("reads"), new ColSpec.Num("size"), new ColSpec.Num("pct_qual"),
                new ColSpec.Normal("process_date"), new ColSpec.Num("avg_read_len"), new ColSpec.Num("coverage"),
                new ColSpec.Num("pct_expressed"));
        // Run through the samples, adding rows.  Note the first column contains a checkbox.
        for (RnaData.JobData sample : data.getSamples()) {
            DomContent sampleName = text(sample.getName());
            boolean keep = true;
            if (! sample.isGood()) {
                if (! this.allFlag)
                    keep = false;
                else {
                    sampleName = em(sample.getName());
                }
            }
            if (keep) {
                new Row<Key.Null>(table, Key.NONE).add(input().withType("checkbox").withName("sample1").withValue(sample.getName()))
                        .add(sampleName).add(sample.getProduction())
                        .add(sample.getOpticalDensity()).add(sample.getOldName()).add(sample.getReadCount())
                        .add(sample.getBaseCount()).add(sample.getQuality()).add(sample.getProcessingDate().toString())
                        .add(sample.getMeanReadLen()).add(sample.getCoverage(GENOME_LEN)).add(sample.getExpressedPercent());
            }
        }
        // Get the page writer.
        PageWriter writer = this.getPageWriter();
        // Set up a link to switch modes.
        String nameParm = "name=" + this.configuration;
        String[] parms;
        String label;
        if (this.allFlag) {
            parms = new String[] { nameParm };
            label = "Hide low-quality samples.";
        } else {
            parms = new String[] { nameParm, "all=on" };
            label = "Show all samples.";
        }
        ContainerTag switchLink = p(a(label).withHref(this.commandUrl("rna", "meta", parms)));
        // Format the table as a form.
        DomContent submitForm = form().withMethod("POST")
                .withAction(this.commandUrl("rna", "columns"))
                .withClass("web").with(p(join("Add checked samples to RNA Seq page configuration",
                        input().withValue(this.configuration).withType("text").withName("name"),
                        " showing genes ", input().withValue("").withType("text").withName("genes"),
                        input().withType("hidden").withName("rowFilter").withValue("GENES"),
                        input().withType("submit"))))
                .with(table.output());
        DomContent tableHtml = this.getPageWriter().highlightBlock(submitForm);
        writer.writePage("RNA Seq Metadata", text("Table of Samples"), switchLink, tableHtml);
    }

}
