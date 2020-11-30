/**
 *
 */
package org.theseed.web.rna;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.web.ColSpec;
import org.theseed.web.ColumnProcessor;
import org.theseed.web.CookieFile;
import org.theseed.web.HtmlForm;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;
import org.theseed.web.WebProcessor;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This command is used to manage column configurations.  Each configuration is displayed along with its
 * column list, and the user may select one or more for deletion, may rename one, or may select one to load.
 *
 * The positional parameters are the name of the CoreSEED data directory and the name of the user's workspace.
 *
 * The command-line options are as follows.
 *
 * --delete		name of a configuration to delete; this parameter may occur more than once
 * --rename		name of a configuration to rename
 * --newName	new name of the configuration being renamed
 *
 * @author Bruce Parrello
 *
 */
public class ColumnManageProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ColumnManageProcessor.class);

    // COMMAND-LINE OPTIONS

    /** configurations to delete */
    @Option(name = "--delete", usage = "configuration name(s) to delete")
    private List<String> deleteConfigs;

    /** configuration to rename (if any) */
    @Option(name = "--rename", usage = "configuration to rename")
    private String renameConfig;

    /** new name for configuration (if any) */
    @Option(name = "--newName", usage = "new configuration name")
    private String newConfigName;


    @Override
    protected void setWebDefaults() {
        this.deleteConfigs = new ArrayList<String>();
        this.renameConfig = "";
        this.newConfigName = "";
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
        // Get all of the current configurations.
        List<String> configs = ColumnProcessor.getConfigurations(cookies);
        log.info("{} configurations found in cookie file.", configs.size());
        // Process the deletions.
        if (this.deleteConfigs.size() > 0) {
            Set<String> deletions = new HashSet<String>(this.deleteConfigs);
            Iterator<String> iter = configs.iterator();
            while (iter.hasNext()) {
                String curr = iter.next();
                if (deletions.contains(curr)) {
                    cookies.delete(ColumnProcessor.COLUMNS_PREFIX + curr);
                    iter.remove();
                    log.info("Configuration {} deleted.", curr);
                }
            }
        }
        // Process the rename.
        if (! this.renameConfig.isEmpty()) {
            String oldConfig = ColumnProcessor.COLUMNS_PREFIX + this.renameConfig;
            String oldValue = cookies.getString(oldConfig);
            if (oldValue == null)
                throw new IllegalArgumentException("Cannot find configuration " + this.renameConfig + " to rename.");
            String newConfigFixed = StringUtils.replaceChars(this.newConfigName, ' ', '_');
            String newConfig = ColumnProcessor.COLUMNS_PREFIX + newConfigFixed;
            if (cookies.getString(newConfig) != null)
                throw new IllegalArgumentException("Configuration name " + this.newConfigName + " already exists.");
            // Here the rename is safe.
            cookies.delete(oldConfig);
            cookies.put(newConfig, oldValue);
            log.info("Configuration {} renamed to {}.", this.renameConfig, this.newConfigName);
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
     * Build the form for renaming a configuration.
     *
     * @param configs	list of current configurations
     *
     * @return the form HTML
     *
     * @throws IOException
     */
    private ContainerTag buildRenameForm(List<String> configs) throws IOException {
        HtmlForm retVal = new HtmlForm("rna", "manage", this);
        Collections.sort(configs);
        retVal.addChoiceRow("rename", "Configuration to rename", "", configs);
        retVal.addTextRow("newName", "New name for configuration", "");
        return retVal.output();
    }

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
            HtmlTable<Key.Mixed>.Row row = formTable.new Row(new Key.Mixed(config));
            // Build a URL for viewing this configuration.
            String url = this.getPageWriter().local_url("/rna.cgi/columns?name=" +
                    URLEncoder.encode(config, StandardCharsets.UTF_8.toString()),
                    this.getWorkSpace());
            // The first cell contains the checkbox and the configuration name linked to the configuration itself.
            // Note that if you delete the default configuration, it will be recreated as empty.  We allow this.
            DomContent configCell = join(input().withType("checkbox").withName("delete").withValue(config),
                    a(config).withHref(url));
            row.add(configCell);
            // The second cell contains the column list.
            String cookieString = cookies.get(ColumnProcessor.COLUMNS_PREFIX + config, "");
            String[] columns = ColumnDescriptor.getSpecStrings(cookieString);
            DomContent columnString = ul().with(Arrays.stream(columns)
                    .map(x -> li(StringUtils.replaceChars(StringUtils.removeEnd(x, ","), ',', '/'))));
            row.add(columnString);
        }
        // Render the form.
        ContainerTag retVal = form().withMethod("POST")
                .withAction(this.getPageWriter().local_url("/rna.cgi/manage"))
                .withClass("web")
                .with(input().withType("hidden").withName("workspace").withValue(this.getWorkSpace()))
                .with(formTable.output())
                .with(p().with(input().withType("submit").withClass("submit").withValue("DELETE SELECTED")));
        return retVal;
    }


}
