/**
 *
 */
package org.theseed.web.rna;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;
import org.theseed.web.CookieFile;
import org.theseed.web.HtmlForm;
import org.theseed.web.ProductionProcessor;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This command manages the saved configurations for the production/prediction page.  Each configuration is stored in a separate
 * file.
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
public class ProductionManageProcessor extends ManageProcessor {

    // FIELDS
    /** map of configuration names to files */
    private SortedMap<String, File> fileMap;
    /** buffer for creating descriptions */
    private TextStringBuilder descriptionBuffer;

    @Override
    protected void init() {
        // Here we get the configuration names and map them to files.
        String[] workFileNames = this.getWorkSpaceDir().list();
        this.fileMap = new TreeMap<String, File>();
        // Loop through the workspace directory file names, keeping the saved configurations.
        // The default is named "1".
        String defaultName = CookieFile.cookieFileName(HtmlForm.formCookieName("rna", "production"));
        for (String fileName : workFileNames) {
            if (fileName.contentEquals(defaultName))
                this.fileMap.put("1", new File(this.getWorkSpaceDir(), fileName));
            else {
                Matcher m = ProductionProcessor.RNA_PRODUCTION_CONFIG.matcher(fileName);
                if (m.matches())
                    this.fileMap.put(m.group(1), new File(this.getWorkSpaceDir(), fileName));
            }
        }
        // Create the description buffer.
        this.descriptionBuffer = new TextStringBuilder(100);
    }

    @Override
    protected String renameConfiguration(CookieFile cookies) {
        if (ProductionProcessor.badConfigName(this.newConfigName))
            throw new IllegalArgumentException("New configuration name contains invalid characters.");
        else {
            File oldFile = this.fileMap.get(this.renameConfig);
            File newFile = CookieFile.computeFile(this.getWorkSpaceDir(), ProductionProcessor.getConfigName(this.newConfigName));
            if (! oldFile.exists())
                throw new IllegalArgumentException("Configuration name " + this.renameConfig + " does not exist.");
            try {
                if (newFile.exists())
                    FileUtils.forceDelete(newFile);
                FileUtils.moveFile(oldFile, newFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // Update the file map.
            this.fileMap.remove(this.renameConfig);
            this.fileMap.put(this.newConfigName, newFile);
        }
        return this.newConfigName;
    }

    @Override
    protected void deleteConfiguration(CookieFile cookies, String curr) {
        try {
            File currFile = this.fileMap.get(curr);
            FileUtils.forceDelete(currFile);
            // Update the file mape.
            this.fileMap.remove(curr);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected List<String> getConfigurations(CookieFile cookies) {
        return new ArrayList<String>(this.fileMap.keySet());
    }

    @Override
    protected DomContent describeConfiguration(CookieFile cookies, String config) {
        ContainerTag retVal = ul();
        try (CookieFile otherCookies = new CookieFile(this.fileMap.get(config))) {
            this.descriptionBuffer.clear().append("Allowing:");
            int allowCount = 0;
            for (String name : ProductionProcessor.FRAGMENT_NAMES) {
                String value = otherCookies.get(name, "");
                if (! value.isEmpty()) {
                    this.descriptionBuffer.append(' ').append(StringUtils.replaceChars(value, ',', ' '));
                    allowCount++;
                }
            }
            if (allowCount == 0) this.descriptionBuffer.append(" none");
            retVal.with(li(this.descriptionBuffer.toString()));
            retVal.with(li(String.format("Prediction range %s to %s.", otherCookies.get("minPred", "0.0"),
                    otherCookies.get("maxPred", "5.0"))));
            retVal.with(li("Compare by " + otherCookies.get("compare", "(none)")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return retVal;
    }

    @Override
    protected String getCommand() {
        return "production";
    }

    @Override
    protected String computeViewUrl(String config) throws UnsupportedEncodingException {
        return this.getPageWriter().local_url("/rna.cgi/production?saved=" +
                URLEncoder.encode(config, StandardCharsets.UTF_8.toString()),
                this.getWorkSpace());
    }

    @Override
    protected String getCookieName() {
        return "rna.prodManage";
    }

    @Override
    public String getManageCommand() {
        return "predManage";
    }

}
