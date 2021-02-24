/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;

import org.theseed.rna.RnaData;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This command displays the meta-data for the RNA sequence samples.  This data is encoded in the sample records
 * of the main RNA data file ("fpkm.ser" in the CoreSEED directory).
 *
 * The positional parameters, as always, are the name of the coreSEED data directory and the name of the user workspace.
 *
 * @author Bruce Parrello
 *
 */
public class RnaMetaProcessor extends WebProcessor {

    @Override
    protected void setWebDefaults() {
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
        // This is a very simple web page:  we just build a table from the sample records.
        File dataFile = new File(this.getCoreDir(), ColumnProcessor.RNA_DATA_FILE_NAME);
        RnaData data = RnaData.load(dataFile);
        log.info("{} samples in RNA dataset {}.", data.size(), dataFile);
        // Create a table for the meta-data.
        HtmlTable<Key.Null> table = new HtmlTable<>(new ColSpec.Normal("sample_id"), new ColSpec.Fraction("Thr g/l"),
                new ColSpec.Num("OD"), new ColSpec.Normal("original_name"), new ColSpec.Num("reads"), new ColSpec.Num("size"),
                new ColSpec.Num("pct_qual"), new ColSpec.Normal("process_date"));
        // Run through the samples, adding rows.
        for (RnaData.JobData sample : data.getSamples()) {
            new Row<Key.Null>(table, Key.NONE).add(sample.getName()).add(sample.getProduction())
                    .add(sample.getOpticalDensity()).add(sample.getOldName()).add(sample.getReadCount())
                    .add(sample.getBaseCount()).add(sample.getQuality()).add(sample.getProcessingDate().toString());
        }
        DomContent tableHtml = this.getPageWriter().highlightBlock(table.output());
        this.getPageWriter().writePage("RNA Seq Metadata", text("Table of Samples"), tableHtml);
    }

}
