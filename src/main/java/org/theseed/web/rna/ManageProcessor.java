/**
 *
 */
package org.theseed.web.rna;

import static j2html.TagCreator.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.web.ColSpec;
import org.theseed.web.CookieFile;
import org.theseed.web.HtmlForm;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.Row;
import org.theseed.web.WebProcessor;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

/**
 * This is the base class for configuration management pages.  A host of abstract methods customize the peculiarity of how the
 * configurations are stored and displayed.  The basic forms allow renaming and deletion, with links to viewing the configuration.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ManageProcessor extends WebProcessor {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ColumnManageProcessor.class);
    /** configurations to delete */
    @Option(name = "--delete", usage = "configuration name(s) to delete")
    private List<String> deleteConfigs;
    /** configuration to rename (if any) */
    @Option(name = "--rename", usage = "configuration to rename")
    protected String renameConfig;
    /** new name for configuration (if any) */
    @Option(name = "--newName", usage = "new configuration name")
    protected String newConfigName;

    /**
     *
     */
    public ManageProcessor() {
        super();
    }

    @Override
    protected void setWebDefaults() {
        this.deleteConfigs = new ArrayList<String>();
        this.renameConfig = "";
        this.newConfigName = "";
    }

    @Override
    protected final boolean validateWebParms() throws IOException {
        return true;
    }

    /**
     * Perform any necessary initialization.
     */
    protected void init() { }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Perform any necessary initialization.
        this.init();
        // Get all of the current configurations.
        List<String> configs = getConfigurations(cookies);
        // Process the deletions.
        if (this.deleteConfigs.size() > 0) {
            Set<String> deletions = new HashSet<String>(this.deleteConfigs);
            Iterator<String> iter = configs.iterator();
            while (iter.hasNext()) {
                String curr = iter.next();
                if (deletions.contains(curr)) {
                    deleteConfiguration(cookies, curr);
                    iter.remove();
                    log.info("Configuration {} deleted.", curr);
                }
            }
        }
        // Process the rename.
        if (! this.renameConfig.isEmpty()) {
            String newConfigFixed = renameConfiguration(cookies);
            // Update the configuration list.
            configs.remove(this.renameConfig);
            configs.add(newConfigFixed);
        }
        // Now we build the delete form.
        ContainerTag deleteForm = buildDeleteForm(cookies, configs);
        // Next we build the rename form.
        ContainerTag renameForm = buildRenameForm(configs);
        // Construct the web page body.
        DomContent mainPage = this.getPageWriter().highlightBlock(h2("Configuration List"), deleteForm,
                h2("Rename a Configuration"), renameForm);
        // Write the page.
        this.getPageWriter().writePage("Configuration Management", text("Column Configurations"),
                mainPage);
    }

    /**
     * Execute the configuration rename.
     *
     * @param cookies	cookie file
     *
     * @return the new configuration name (corrected)
     *
     * @throws ParseFailureException
     */
    protected abstract String renameConfiguration(CookieFile cookies) throws ParseFailureException;

    /**
     * Delete a configuration.
     *
     * @param cookies	cookie file
     * @param curr		configuration to delete
     */
    protected abstract void deleteConfiguration(CookieFile cookies, String curr);

    /**
     * @return a list of configuration names
     *
     * @param cookies	cookie file
     */
    protected abstract List<String> getConfigurations(CookieFile cookies);

    /**
     * Build the form for renaming a configuration.
     *
     * @param configs	list of current configurations
     *
     * @return the form HTML
     *
     * @throws IOException
     */
    private ContainerTag buildRenameForm(List<String> configs) throws IOException {
        HtmlForm retVal = new HtmlForm("rna", getManageCommand(), this);
        Collections.sort(configs);
        retVal.addChoiceRow("rename", "Configuration to rename", "", configs);
        retVal.addTextRow("newName", "New name for configuration", "");
        return retVal.output();
    }

    /**
     * @return the name of the configuration management command
     */
    public abstract String getManageCommand();

    /**
     * Build the configuration deletion form.
     *
     * @param cookies	incoming cookie file
     * @param configs	precomputed list of configurations
     *
     * @return the form HTML
     *
     * @throws UnsupportedEncodingException
     */
    public ContainerTag buildDeleteForm(CookieFile cookies, List<String> configs) throws UnsupportedEncodingException {
        HtmlTable<Key.Mixed> formTable = new HtmlTable<>(new ColSpec.Normal("Configuration"),
                new ColSpec.Normal("Columns"));
        for (String config : configs) {
            Row<Key.Mixed> row = new Row<Key.Mixed>(formTable, new Key.Mixed(config));
            // Build a URL for viewing this configuration.
            String url = computeViewUrl(config);
            // The first cell contains the checkbox and the configuration name linked to the configuration itself.
            // Note that if you delete the default configuration, it will be recreated as empty.  We allow this.
            DomContent configCell = join(input().withType("checkbox").withName("delete").withValue(config),
                    a(config).withHref(url));
            row.add(configCell);
            // The second cell contains the column list.
            DomContent columnString = describeConfiguration(cookies, config);
            row.add(columnString);
        }
        // Render the form.
        String command = this.getManageCommand();
        ContainerTag retVal = form().withMethod("POST")
                .withAction(this.getPageWriter().local_url("/rna.cgi/" + command))
                .withClass("web")
                .with(input().withType("hidden").withName("workspace").withValue(this.getWorkSpace()))
                .with(formTable.output())
                .with(p().with(input().withType("submit").withClass("submit").withValue("DELETE SELECTED")));
        return retVal;
    }

    /**
     * @return a description of the specified configuration
     *
     * @param cookies	cookie file
     * @param config	configuration name
     */
    protected abstract DomContent describeConfiguration(CookieFile cookies, String config);

    /**
     * @return the command for this management processor
     */
    protected abstract String getCommand();

    /**
     * @return the URL for viewing a configuration
     *
     * @param config	configuration name
     *
     * @throws UnsupportedEncodingException
     */
    protected abstract String computeViewUrl(String config) throws UnsupportedEncodingException;

}
