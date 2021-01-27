/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.reports.LinkObject;
import org.theseed.utils.ParseFailureException;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This is a simple web command that displays the subsystem row for a GTO.  As always, the
 * positional parameters are the name of the CoreSEED data directory and the name of the
 * user's workspace.  The command-line options are as follows.
 *
 * --name		name of the subsystem to display
 * --genome		name of the genome to display; the default is "MG1655-wild.gto"
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemPageProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SubsystemPageProcessor.class);
    /** subsystem row to display */
    private SubsystemRow subRow;
    /** link creator */
    private LinkObject linker;

    // COMMAND-LINE OPTONS

    /** name of desired subsystem */
    @Option(name = "--name", metaVar = "Histidine BioSynthesis", usage = "name of subsystem to display",
            required = true)
    protected String subName;

    /** name of the genome to display (in the CoreSEED data directory) */
    @Option(name = "--genome", metaVar = "83333.1.gto", usage = "genome file name")
    protected String genomeName;

    @Override
    protected void setWebDefaults() {
        this.genomeName = "MG1655-wild.gto";
    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        // Read in the genome.
        File gFile = new File(this.getCoreDir(), this.genomeName);
        if (! gFile.canRead())
            throw new FileNotFoundException("Target genome " + this.genomeName + " not found or unreadable.");
        Genome genome = new Genome(gFile);
        // Get the link creator.
        this.linker = genome.getLinker();
        // Make sure we can find the subsystem.
        this.subRow = genome.getSubsystem(this.subName);
        if (this.subRow == null)
            throw new ParseFailureException("Cannot find subsystem \"" + this.subName + "\" in genome "
                    + genome.toString());
        return true;
    }

    @Override
    protected String getCookieName() {
        return "rna.subsystem";
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Create a table of the subsystem roles.
        HtmlTable<Key.Null> roleTable = new HtmlTable<Key.Null>(new ColSpec.Normal("Role"),
                new ColSpec.Normal("Features"));
        for (SubsystemRow.Role role : this.subRow.getRoles()) {
            Row<Key.Null> roleRow = new Row<Key.Null>(roleTable, Key.NONE);
            roleRow.add(role.getName());
            DomContent fids = rawHtml(role.getFeatures().stream()
                    .map(f -> this.linker.featureLink(f.getId()).render())
                    .collect(Collectors.joining(", ")));
            roleRow.add(fids);
        }
        // The page title is the subsystem name.  We list the three classification strings and then
        // output the role table.
        List<DomContent> classes = this.subRow.getClassifications().stream().filter(x -> ! x.isEmpty())
                .map(x -> li(x)).collect(Collectors.toList());
        ContainerTag classList;
        if (classes.size() == 0)
            classList = p("Subsystem is unclassified.");
        else
            classList = ul().with(classes);
        ContainerTag roleBlock = this.getPageWriter().highlightBlock(classList, roleTable.output());
        this.getPageWriter().writePage(this.subName, h1(this.subName), roleBlock);
    }

}
