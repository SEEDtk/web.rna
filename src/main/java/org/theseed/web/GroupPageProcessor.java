/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.io.LineReader;
import org.theseed.reports.HtmlUtilities;
import org.theseed.utils.IDescribable;
import org.theseed.web.rna.GroupPageFilter;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

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
 * --filterType	filtering criterion (ALL, ANY, GROUP)
 * --genomes	list of IDs of genomes to display
 * --sort		sort order for features (LOCATION, CHANGES)
 * --region		region of interest in the feature (UPSTREAM, INSTREAM)
 *
 * @author Bruce Parrello
 *
 */
public class GroupPageProcessor extends WebProcessor implements GroupPageFilter.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GroupPageProcessor.class);
    /** base genome */
    private Genome baseGenome;
    /** group snips file */
    private File groupFile;
    /** filtering engine */
    private GroupPageFilter filter;
    /** list of column indices of interest */
    private BitSet genomeCols;
    /** index of region type (0 = upstream, 1 = instream) */
    private int typeIndex;
    /** location of the group page */
    private static final String GROUP_URL = "/rna.cgi/groups?group=";


    // COMMAND-LINE OPTIONS

    /** sort order for output */
    @Option(name = "--sort", usage = "sort order for output")
    private GroupPageSortKey.Order sortOrder;

    /** region of interest in feature */
    @Option(name = "--region", usage = "region of interest")
    private RegionType regionArea;

    /** type of filtering */
    @Option(name = "--filterType", usage = "type of filtering to perform")
    protected GroupPageFilter.Type filterType;

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

    /** IDs of the genomes to display (comma-delimited, default is all) */
    @Option(name = "--genomes", metaVar = "511145.183,511145.184", usage = "comma-delimited list of genomes to display")
    protected String genomes;

    /**
     * This enum defines the two regions of interest.
     */
    private static enum RegionType implements IDescribable {
        UPSTREAM {
            @Override
            public int getIdx() {
                return 0;
            }

            @Override
            public String getDescription() {
                return "Upstream";
            }
        }, INSTREAM {
            @Override
            public int getIdx() {
                return 1;
            }

            @Override
            public String getDescription() {
                return "Protein";
            }
        };

        /**
         * @return the position in the mark string of the region's mark
         */
        public abstract int getIdx();
    }

    @Override
    protected void setWebDefaults() {
        this.needsWorkspace = false;
        this.genomeName = "MG1655-wild.gto";
        this.groupFileName = "groups.snips.tbl";
        this.groupTitle = null;
        this.genomes = null;
        this.filterType = GroupPageFilter.Type.GROUP;
        this.sortOrder = GroupPageSortKey.Order.LOCATION;
        this.regionArea = RegionType.INSTREAM;
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
        if (this.filterType != GroupPageFilter.Type.GROUP)
            this.groupTitle = "All Features With " + this.regionArea.getDescription() + " Changes";
        else {
            if (this.groupTitle == null)
                this.groupTitle = "Feature Group " + groupName;
            else
                this.groupTitle = "Feature Group " + StringUtils.replaceChars(this.groupTitle, '_', ' ');
            this.groupTitle += " Showing " + this.regionArea.getDescription() + " Changes";
        }
        // Save the sort order and the type index.
        GroupPageSortKey.setOrder(this.sortOrder);
        this.typeIndex = this.regionArea.getIdx();
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
        HtmlTable<GroupPageSortKey> table;
        // Here we will keep the list of genome IDs found.
        List<String> colGenomes = new ArrayList<String>();
        // The aligned genomes are listed in the input file.
        try (LineReader groupStream = new LineReader(this.groupFile)) {
            List<ColSpec> cols = new ArrayList<ColSpec>(20);
            // ColGenomes contains the genome corresponding to each input column.  Since the function
            // is not in the input, it does not have a colGenomes entry.
            cols.add(new ColSpec.Normal("Feature"));
            colGenomes.add("");
            cols.add(new ColSpec.Normal("Function"));
            cols.add(new ColSpec.Normal("Groups"));
            colGenomes.add("");
            for (String[] genomeSpec : groupStream.new Section("//")) {
                ColSpec col = new ColSpec.Centered(genomeSpec[0]).setTip(genomeSpec[1]);
                cols.add(col);
                colGenomes.add(genomeSpec[0]);
            }
            // Now we know the genomes in each column.  Form the bitmap of columns to display.  We will always display
            // the first four.  Only the others matter.
            this.createGenomeCols(colGenomes);
            // With the genome column list specified, we can create the filter.
            this.filter = this.filterType.create(this);
            // Now build the table.
            ColSpec[] colSpecs = IntStream.range(0, colGenomes.size() + 1).filter(i -> (i < 4 || this.genomeCols.get(i-1)))
                    .mapToObj(i -> cols.get(i)).toArray(ColSpec[]::new);
            table = new HtmlTable<GroupPageSortKey>(colSpecs);
            // Now we are positioned in the group file on the first feature.  Each feature's record consists of
            // a feature ID, a comma-delimited list of group names, and then flags for the snip changes.  We will
            // store the flags in here.
            List<String> outCols = new ArrayList<String>(colGenomes.size());
            for (String[] featureSpec : groupStream.new Section(null)) {
                String fid = featureSpec[0];
                // Check to see if this feature is in our group.
                boolean groupFound = this.filter.isDisplay(featureSpec);
                if (groupFound) {
                    // Here the feature is in the group of interest.
                    Feature feat = this.baseGenome.getFeature(fid);
                    // Build the flag column list.
                    outCols.clear();
                    int marks = 0;
                    for (int i = 3; i < featureSpec.length; i++) {
                        if (this.genomeCols.get(i)) {
                            char mark = featureSpec[i].charAt(this.typeIndex);
                            if (mark == ' ')
                                outCols.add("");
                            else {
                                marks++;
                                outCols.add(Character.toString(mark));
                            }
                        }
                    }
                    Row<GroupPageSortKey> row = new Row<>(table, new GroupPageSortKey(feat, marks));
                    // Column 1 is the feature ID, linked to PATRIC.
                    row.add(this.baseGenome.featureLink(fid));
                    // Column 2 is the function.
                    row.add(a(feat.getPegFunction()).withHref("/html/align2.html#peg_" + StringUtils.substringAfterLast(fid, ".")));
                    // Column 3 is the group list.
                    row.add(this.createGroupList(StringUtils.split(featureSpec[1], ',')));
                    // Column 4 is the base genome.
                    row.add(featureSpec[2]);
                    // The rest are all output columns.
                    for (String outCol : outCols)
                        row.add(outCol);
                }
            }
        }
        // Now we are ready to write the page.
        ContainerTag legend = p("Showing snip changes as M (mutation) or D (deletion).");
        ContainerTag mainTable = this.getPageWriter().highlightBlock(legend, table.output());
        this.getPageWriter().writePage(this.groupTitle, h2(this.groupTitle), mainTable);
    }

    /**
     * @return the display string for all the groups to which this feature belongs
     *
     * @param groups	array of group IDs
     */
    private DomContent createGroupList(String[] groups) {
        List<DomContent> links = new ArrayList<DomContent>(groups.length);
        for (String group : groups) {
            String url = GROUP_URL + group + ";region=" + this.regionArea.toString();
            if (this.genomes != null)
                url += ";genomes=" + this.genomes;
            links.add(a(group).withHref(url).withTarget("_blank"));
        }
        DomContent retVal = HtmlUtilities.joinDelimited(links, ", ");
        return retVal;
    }

    /**
     * Initialize the set of non-base genome columns to display.
     *
     * @param colGenomes	list of genome columns.
     *
     * @throws ParseFailureException
     */
    private void createGenomeCols(List<String> colGenomes) throws ParseFailureException {
        this.genomeCols = new BitSet(colGenomes.size());
        // We need a set for the user-specified genomes.  If none were specified, we allow them all.
        Set<String> genomeSet;
        if (this.genomes == null)
            genomeSet = new TreeSet<String>(colGenomes);
        else
            genomeSet = Arrays.stream(StringUtils.split(this.genomes, ',')).collect(Collectors.toSet());
        // Test the column headers against the set.
        int count = 0;
        for (int i = 0; i < colGenomes.size(); i++) {
            if (genomeSet.contains(colGenomes.get(i))) {
                this.genomeCols.set(i);
                count++;
            }
        }
        if (count < genomeSet.size())
            throw new ParseFailureException("Invalid genome IDs specified in genome list.");
    }

    @Override
    public String getGroupId() {
        return this.groupName;
    }

    @Override
    public BitSet getColumns() {
        return this.genomeCols;
    }

    @Override
    public int getRegionIndex() {
        return this.typeIndex;
    }

}
