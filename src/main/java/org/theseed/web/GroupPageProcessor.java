/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.io.LineReader;
import org.theseed.utils.ParseFailureException;

import j2html.tags.ContainerTag;
import static j2html.TagCreator.*;

/**
 * This page displays the features in a single group.  For each feature, we show the ID, the function, and the gene name.
 * The ID is linked to the PATRIC page for the gene.
 *
 * This page is called from a non-web location, so it has to operate without a workspace.  It does, however, still have
 * access to the CoreSEED directory.  The group information needed is in the file "groups.snips.tbl" in that directory.
 *
 * The positional parameter is the name of the CoreSEED directory.
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --group		name of group to display
 * --title		display title for the group, with spaces converted to underscores
 * --genome		name of the base genome; the default is "MG1655-wild.gto"
 * --groupFile	name of the group file; the default is "groups.snips.tbl"
 *
 *
 * @author Bruce Parrello
 *
 */
public class GroupPageProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GroupPageProcessor.class);
    /** base genome */
    private Genome baseGenome;
    /** group snips file */
    private File groupFile;

    // COMMAND-LINE OPTIONS

    /** name of the group to display */
    @Option(name = "--group", metaVar = "groupName", usage = "name of the group (modulon, regulon) to display")
    protected String groupName;

    /** title of the group to display */
    @Option(name = "--title", metaVar = "groupTitle", usage = "long-format title for the group, with spaces as underscores")
    protected String groupTitle;

    /** name of the genome of interest (in the CoreSEED data directory) */
    @Option(name = "--genome", metaVar = "83333.1.gto", usage = "genome file name")
    protected String genomeName;

    /** name of the groups file (in the CoreSEED data directory) */
    @Option(name = "--groupFile", metaVar = "snipSummary.tbl", usage = "group snips file name")
    protected String groupFileName;

    @Override
    protected void setWebDefaults() {
        this.needsWorkspace = false;
        this.genomeName = "MG1655-wild.gto";
        this.groupFileName = "groups.snips.tbl";
        this.groupTitle = null;
    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        // Get the base genome.
        File genomeFile = new File(this.getCoreDir(), this.genomeName);
        if (! genomeFile.canRead())
            throw new FileNotFoundException("Genome " + this.genomeName + " in Core directory is not found or unreadable.");
        this.baseGenome = new Genome(genomeFile);
        // Verify the group snips file.
        this.groupFile = new File(this.getCoreDir(), this.groupFileName);
        if (! this.groupFile.canRead())
            throw new FileNotFoundException("Group snips file " + this.groupFileName + " in Core directory is not found or unreadable.");
        // Fix up the group title.
        if (this.groupTitle == null)
            this.groupTitle = groupName;
        else
            this.groupTitle = StringUtils.replaceChars(this.groupTitle, '_', ' ');
        return true;
    }

    @Override
    protected String getCookieName() {
        return null;
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // We need to create the table.  The first two columns are the feature ID and function.
        // Then there is one column for each aligned genome.  The table is sorted by feature location.
        HtmlTable<Key.Feature> table;
        // The aligned genomes are listed in the input file.
        try (LineReader groupStream = new LineReader(this.groupFile)) {
            List<ColSpec> cols = new ArrayList<ColSpec>(20);
            cols.add(new ColSpec.Normal("Feature"));
            cols.add(new ColSpec.Normal("Function"));
            for (String[] genomeSpec : groupStream.new Section("//")) {
                ColSpec col = new ColSpec.Centered(genomeSpec[0]).setTip(genomeSpec[1]);
                cols.add(col);
            }
            ColSpec[] colSpecs = cols.stream().toArray(ColSpec[]::new);
            table = new HtmlTable<Key.Feature>(colSpecs);
            // Now we are positioned in the group file on the first feature.  Each feature's record consists of
            // a feature ID, a comma-delimited list of group names, and then flags for the snip changes.
            for (String[] featureSpec : groupStream.new Section(null)) {
                String fid = featureSpec[0];
                // Check to see if this feature is in our group.
                boolean groupFound = Arrays.stream(StringUtils.split(featureSpec[1], ',')).anyMatch(x -> x.contentEquals(this.groupName));
                if (groupFound) {
                    // Here the feature is in the group of interest.
                    Feature feat = this.baseGenome.getFeature(fid);
                    Row<Key.Feature> row = new Row<>(table, new Key.Feature(feat));
                    // Column 1 is the feature ID, linked to PATRIC.
                    row.add(this.baseGenome.featureLink(fid));
                    // Column 2 is the function.
                    row.add(feat.getPegFunction());
                    // The remaining columns are copied from the input line.
                    for (int i = 2; i < featureSpec.length; i++)
                        row.add(featureSpec[i]);
                }
            }
        }
        // Now we are ready to write the page.
        ContainerTag mainTable = this.getPageWriter().highlightBlock(table.output());
        this.getPageWriter().writePage(this.groupName, h2("Feature group " + this.groupTitle), mainTable);
    }

}
