/**
 *
 */
package org.theseed.web;

import java.io.IOException;

import org.theseed.utils.ParseFailureException;

/**
 * This page allows the user to manage the list of gene sets.  An option is provided to add a named gene set, one to delete
 * a named gene set, plus the ability to select an existing gene set.  The user can then jump to the RNA seq sample list and
 * choose samples to display filtered by the gene set.  The gene sets can also be used for filtering on the column display
 * page.
 *
 * The gene sets are stored in a file named "_geneLists.tbl" in the user's workspace.  If the file does not exist,
 * it is created with a special "all" entry predefined.  The file is tab-delimited, with the gene list name in the first
 * column, and a list of the gene names in the second column.  The special constant "(all)" denotes the default of all
 * genes.
 *
 * The positional parameters, as always, are the name of the data directory and the name of the user workspace.
 *
 * The command-line options are as follows.
 *
 * --delete		name of a gene list to delete
 *
 * @author Bruce Parrello
 *
 */
public class GeneListsProcessor extends WebProcessor {



    @Override
    protected void setWebDefaults() {
        // TODO code for setWebDefaults

    }

    @Override
    protected boolean validateWebParms() throws IOException, ParseFailureException {
        // TODO code for validateWebParms
        return false;
    }

    @Override
    protected String getCookieName() {
        // TODO code for getCookieName
        return null;
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // TODO code for runWebCommand

    }
    // FIELDS
    // TODO data members for GeneListsProcessor

    // TODO constructors and methods for GeneListsProcessor
}
