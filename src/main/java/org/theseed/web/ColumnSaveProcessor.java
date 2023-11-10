/**
 *
 */
package org.theseed.web;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.web.rna.ColumnDescriptor;

import j2html.tags.DomContent;

import static j2html.TagCreator.*;

/**
 * This command copies a column configuration.
 *
 * The positional parameters, as always, are the name of the coreSEED data directory and the name of the user workspace.
 *
 * The command-line options are as follows.
 *
 * --old	name of the configuration to copy
 * --name	name of the new configuration to create from it
 *
 * @author Bruce Parrello
 *
 */
public class ColumnSaveProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ColumnSaveProcessor.class);
    /** list of valid characters in configuration names */
    private static final String VALID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";

    // COMMAND-LINE OPTIONS

    /** source configuration name */
    @Option(name = "--old", usage = "source configuration name")
    protected String sourceConfig;

    /** target configuration name */
    @Option(name = "--name", usage = "target configuration name")
    protected String targetConfig;

    @Override
    protected void setWebDefaults() {
        this.targetConfig = "";
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
        // This will be the output message.
        DomContent message;
        // Verify the save value.
        if (this.targetConfig.isEmpty())
            message = p("ERROR: no save name specified.");
        else {
            // Get the old configuration and determine its size.
            String cookieString = cookies.get(ColumnProcessor.COLUMNS_PREFIX + this.sourceConfig, "");
            int size = ColumnDescriptor.getSpecStrings(cookieString).length;
            // Insure the new name is valid.
            String newName = computeNewName(this.targetConfig);
            // Store it under the new name.
            cookies.put(ColumnProcessor.COLUMNS_PREFIX + newName, cookieString);
            // Format and write the result message.
            String cols = String.format("%d-column", size);
            message = p(join(cols, "configuration", b(this.sourceConfig),
                    "saved to", b(newName), "."));
        }
        this.getPageWriter().writePage("Configuration Save", null, message);
    }

    /**
     * This method fixes an incoming configuration name and insures it is valid.
     *
     * @param targetConfig	proposed configuration name
     *
     * @return the name for a configuration
     *
     * @throws ParseFailureException
     */
    public static String computeNewName(String targetConfig) throws ParseFailureException {
        String retVal = StringUtils.replaceChars(targetConfig, ' ', '_');
        if (! StringUtils.containsOnly(retVal, VALID_CHARS))
            throw new ParseFailureException("Invalid configuration name.  Can only contain letters, digits, and underscores.");
        return retVal;
    }

}
