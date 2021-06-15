/**
 *
 */
package org.theseed.web.rna;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.utils.ParseFailureException;
import org.theseed.web.ColumnProcessor;
import org.theseed.web.ColumnSaveProcessor;
import org.theseed.web.CookieFile;

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
public class ColumnManageProcessor extends ManageProcessor {



    // COMMAND-LINE OPTIONS

    @Override
    protected String getCookieName() {
        return ColumnProcessor.RNA_COLUMN_COOKIE_FILE;
    }

    @Override
    protected String renameConfiguration(CookieFile cookies) throws ParseFailureException {
        String oldConfig = ColumnProcessor.COLUMNS_PREFIX + this.renameConfig;
        String oldValue = cookies.getString(oldConfig);
        if (oldValue == null)
            throw new IllegalArgumentException("Cannot find configuration " + this.renameConfig + " to rename.");
        String newConfigFixed = ColumnSaveProcessor.computeNewName(this.newConfigName);
        String newConfig = ColumnProcessor.COLUMNS_PREFIX + newConfigFixed;
        if (cookies.getString(newConfig) != null)
            throw new IllegalArgumentException("Configuration name " + this.newConfigName + " already exists.");
        // Here the rename is safe.
        cookies.delete(oldConfig);
        cookies.put(newConfig, oldValue);
        log.info("Configuration {} renamed to {}.", this.renameConfig, this.newConfigName);
        return newConfigFixed;
    }

    @Override
    protected String getCommand() {
        return "manage";
    }

    @Override
    protected DomContent describeConfiguration(CookieFile cookies, String config) {
        String cookieString = cookies.get(ColumnProcessor.COLUMNS_PREFIX + config, "");
        String[] columns = ColumnDescriptor.getSpecStrings(cookieString);
        DomContent columnString = ul().with(Arrays.stream(columns)
                .map(x -> li(StringUtils.replaceChars(StringUtils.removeEnd(x, ","), ',', '/'))));
        return columnString;
    }

    @Override
    protected String computeViewUrl(String config) throws UnsupportedEncodingException {
        return this.getPageWriter().local_url("/rna.cgi/columns?name=" +
                URLEncoder.encode(config, StandardCharsets.UTF_8.toString()),
                this.getWorkSpace());
    }

    @Override
    protected void deleteConfiguration(CookieFile cookies, String curr) {
        cookies.delete(ColumnProcessor.COLUMNS_PREFIX + curr);
    }

    @Override
    protected List<String> getConfigurations(CookieFile cookies) {
        List<String> configs = ColumnProcessor.getConfigurations(cookies);
        log.info("{} configurations found in cookie file.", configs.size());
        return configs;
    }

    @Override
    public String getManageCommand() {
        return "manage";
    }


}
