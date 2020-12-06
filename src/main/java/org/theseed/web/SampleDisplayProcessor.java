/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.TabbedLineReader;
import org.theseed.proteins.SampleId;
import org.theseed.utils.ParseFailureException;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

import static j2html.TagCreator.*;

/**
 * This processor displays a table of related samples.  A single sample ID is provided.  It may have an "X" in place of a fragment
 * as a wild card (e.g. "7_0_0_A_asdO_X_D000_0_4p5_M1").  All of the samples that match the ID are displayed from the big
 * production table.
 *
 * The positional parameters are the name of the CoreSEED data directory and the name of the user's workspace.
 *
 * The command-line options are as follows:
 *
 * --sample		sample ID of the sample to display
 *
 * @author Bruce Parrello
 *
 */
public class SampleDisplayProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SampleDisplayProcessor.class);

    // COMMAND-LINE OPTIONS

    @Option(name = "--sample", usage = "ID (or pattern) of sample to display", required = true)
    protected String sampleId;

    @Override
    protected void setWebDefaults() {
    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        return true;
    }

    @Override
    protected String getCookieName() {
        return "rna.sample";
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // First, we build a breakdown of the sample ID.
        SampleId sample = new SampleId(this.sampleId);
        ContainerTag breakdown = ul();
        for (int i = 0; i < SampleId.FRAGMENT_DESCRIPTIONS.length; i++) {
            String title = SampleId.FRAGMENT_DESCRIPTIONS[i];
            String fragment = sample.getFragment(i);
            breakdown.with(li(join(b(title + ":"), fragment)));
        }
        // Create a search pattern from the sample ID.
        Pattern samplePattern = Pattern.compile(StringUtils.replace(this.sampleId, "X", "[^_]+"));
        // Now we search for matching samples in the big production table and put them in a table.
        HtmlTable<Key.Text> sampleData = new HtmlTable<Key.Text>(new ColSpec.Normal("sample_id"), new ColSpec.Num("production"),
                new ColSpec.Num("growth"), new ColSpec.Centered("bad"), new ColSpec.Num("normalized"), new ColSpec.Num("rate"),
                new ColSpec.Normal("strain_name"), new ColSpec.Normal("origins"));
        File bigFile = new File(this.getCoreDir(), "big_production_table.tbl");
        try (TabbedLineReader bigStream = new TabbedLineReader(bigFile)) {
            int sampleCol = bigStream.findField("sample");
            int prodCol = bigStream.findField("thr_production");
            int growthCol = bigStream.findField("growth");
            int badCol = bigStream.findField("bad");
            int normCol = bigStream.findField("thr_normalized");
            int rateCol = bigStream.findField("thr_rate");
            int strainCol = bigStream.findField("old_strain");
            int originCol = bigStream.findField("origins");
            // Loop through the file.
            for (TabbedLineReader.Line line : bigStream) {
                String inSample = line.get(sampleCol);
                if (samplePattern.matcher(inSample).matches()) {
                    // Here we found a matching sample.
                    new Row<Key.Text>(sampleData, new Key.Text(inSample)).addKey().add(line.getDouble(prodCol))
                            .add(line.getDouble(growthCol)).add(line.getFancyFlag(badCol)).add(line.getDouble(normCol))
                            .add(line.getDouble(rateCol)).add(line.get(strainCol)).add(line.get(originCol));
                }
            }
        }
        // Determine whether or not we have results to show in the table.
        DomContent sampleTable;
        if (sampleData.getHeight() == 0)
            sampleTable = p("No matching samples found for " + this.sampleId + ".");
        else
            sampleTable = div(h2("Samples Matching " + this.sampleId), sampleData.output());
        // Assemble the web page.
        DomContent mainBlock = this.getPageWriter().highlightBlock(h2("Sample ID Breakdown"), breakdown, sampleTable);
        this.getPageWriter().writePage("Sample Information", h1("Sample Information Page"), mainBlock);
    }

}
