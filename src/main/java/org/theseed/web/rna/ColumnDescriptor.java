/**
 *
 */
package org.theseed.web.rna;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;
import org.theseed.reports.LinkObject;
import org.theseed.rna.RnaData;
import org.theseed.rna.RnaData.FeatureData;
import org.theseed.web.Key;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This object describes an output column for the RNA data.  It provides methods for converting to and from a string
 * (for storage) and abstract display methods for the heading cell and the data cells.  These are overridden by the
 * concrete subclasses.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ColumnDescriptor {

    // FIELDS
    /** RNA data repository */
    private RnaData data;
    /** primary sample name */
    private String sample1;
    /** linker for HTML */
    private static final LinkObject linker = new LinkObject.Patric();
    /** separator character for sort column index */
    private static final String SORT_SEP_CHAR = "|";
    /** separator character for column specifications */
    private static final String COL_SEP_CHAR = ";";

    /**
     * Create a column descriptor from a save string.
     *
     * @param saveString	incoming save string
     * @param data
     * @return
     */
    public static ColumnDescriptor create(String saveString, RnaData data) {
        // Split the save string.
        String[] parts = StringUtils.split(saveString, ',');
        // Create the descriptor.
        ColumnDescriptor retVal = null;
        if (parts.length > 1)
            retVal = new DifferentialColumnDescriptor(parts[1]);
        else
            retVal = new SimpleColumnDescriptor();
        // Fill in the constant data.
        retVal.sample1 = parts[0];
        retVal.data = data;
        // Initialize the descriptor.
        if (! retVal.init())
            retVal = null;
        return retVal;
    }

    /**
     * @return the weight value for a specified sample column and a specified feature
     *
     * @param feat		feature of interest
     * @param colIdx	index of the sample column
     */
    protected double getWeight(RnaData.FeatureData feat, int colIdx) {
        RnaData.Row row = this.getRow(feat);
        RnaData.Weight weight = row.getWeight(colIdx);
        double retVal = 0.0;
        if (weight != null)
            retVal = weight.getWeight();
        return retVal;
    }

    /**
     * Initialize the private data of the descriptor.
     *
     * @return TRUE if the column is valid, else FALSE
     */
    protected abstract boolean init();

    /**
     * @return the RNA repository column index for the specified sample, or -1 if it is not found
     *
     * @param sample	name of the target sample
     */
    protected int getColIdx(String sample) {
        Integer retVal = this.data.findColIdx(sample);
        if (retVal == null) retVal = -1;
        return (int) retVal;
    }

    /**
     * @return the repository row for a feature
     *
     * @param feat	feature for the row
     */
    public RnaData.Row getRow(RnaData.FeatureData feat) {
        return this.data.getRow(feat.getId());
    }

    /**
     * @return the descriptor for a sample
     *
     * @param colIdx	column index of the sample
     */
    public RnaData.JobData getSample(int colIdx) {
        List<RnaData.JobData> jobs = this.data.getSamples();
        return jobs.get(colIdx);
    }

    /**
     * @return the value for the specified feature in this column.
     *
     * @fid		ID of the feature of interest
     */
    public abstract double getValue(FeatureData feat);

    /**
     * @return the title for this column
     */
    public abstract DomContent getTitle();

    /**
     * @return the title string for this column
     */
    public abstract String getTitleString();

    /**
     * @return the tooltip for this column
     */
    public abstract String getTooltip();

    /**
     * @return the table sort key for the specified feature in this column.
     *
     * @fid		ID of the feature of interest
     */
    public abstract Key.RevRatio getKey(FeatureData feat);

    /**
     * @return the primary sample name
     */
    public String getSample1() {
        return this.sample1;
    }

    /**
     * Add a new column to a cookie string.
     *
     * @return the new cookie string
     */
    public static String addColumn(String cookieString, String newColumn) {
        // Remove the sort column index.
        String retVal = StringUtils.substringBefore(cookieString, SORT_SEP_CHAR);
        // Check the nature of the new column.
        if (newColumn.length() <= 1) {
            // Here there is no new column.
        } else if (retVal.isEmpty()) {
            // Here there is ONLY the new column.
            retVal = newColumn;
        } else if (StringUtils.endsWith(retVal, newColumn)) {
            // Here we are repeating the same column.  This is usually a mistake by the user.
        } else {
            // Here we are really adding a new column to existing columns.
            retVal = retVal + COL_SEP_CHAR + newColumn;
        }
        return retVal;
    }

    /**
     * Delete a column from a cookie string.
     *
     * @param cookieString	cookie string to modify
     * @param idx			index of column to delete
     *
     * @return the modified cookie string
     */
    public static String deleteColumn(String cookieString, int idx) {
        String columns[] = getSpecStrings(cookieString);
        TextStringBuilder retVal = new TextStringBuilder(cookieString.length());
        for (int i = 0; i < columns.length; i++) {
            if (i != idx)
                retVal.appendSeparator(';').append(columns[i]);
        }
        return retVal.toString();
    }


    /**
     * @return the tooltip string for the specified sample
     *
     * @param colIdx	column index of the target sample
     */
    protected String tipStringOf(int colIdx) {
        RnaData.JobData sample = this.getSample(colIdx);
        TextStringBuilder retVal = new TextStringBuilder(100);
        if (! Double.isNaN(sample.getProduction()))
            retVal.append("%2.4f g/l", sample.getProduction());
        if (! Double.isNaN(sample.getOpticalDensity())) {
            retVal.appendSeparator(", ");
            retVal.append("%4.2f OD.", sample.getOpticalDensity());
        }
        if (retVal.length() == 0)
            retVal.append("No production.");
        return retVal.toString();
    }

    /**
     * @return an array of column descriptors for all the columns described in the specified cookie string
     *
     * The cookie string contains the column definitions separated by semicolons.
     *
     * @param cookieString		column definition string
     * @param data				RNA data repository
     */
    public static ColumnDescriptor[] parse(String cookieString, RnaData data) {
        String[] columns = getSpecStrings(cookieString);
        List<ColumnDescriptor> buffer = new ArrayList<ColumnDescriptor>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            ColumnDescriptor column = ColumnDescriptor.create(columns[i], data);
            if (column != null)
                buffer.add(column);
        }
        ColumnDescriptor[] retVal = new ColumnDescriptor[buffer.size()];
        return buffer.toArray(retVal);
    }

    /**
     * @return the specification strings for the columns in the specified definition string
     *
     * @param cookieString		column definition string
     */
    public static String[] getSpecStrings(String cookieString) {
        String columnPart = StringUtils.substringBefore(cookieString, SORT_SEP_CHAR);
        return StringUtils.split(columnPart, COL_SEP_CHAR);
    }

    /**
     * @return the sort column for the specified definition string
     *
     * @param cookieString		column definition string
     */
    public static int getSortCol(String cookieString) {
        int retVal = 0;
        String sortPart = StringUtils.substringAfter(cookieString, SORT_SEP_CHAR);
        if (! sortPart.isEmpty())
            retVal = Integer.valueOf(sortPart);
        return retVal;
    }

    /**
     * @return a hyperlink for the specified feature
     *
     * @param id	feature ID to link
     */
    public static ContainerTag fidLink(String id) {
        return linker.featureLink(id);
    }

    /**
     * Format the information about the current configuration.
     *
     * @param cookieString		column string
     * @param sortCol			sort column
     */
    public static String savecookies(String cookieString, int sortCol) {
        return String.format("%s%s%d", cookieString, SORT_SEP_CHAR, sortCol);
    }

    /**
     * @return the HTML for displaying a sample name
     *
     * @param colIdx	column index in the RNA database of the sample
     */
    public DomContent computeName(int colIdx) {
        DomContent retVal;
        String name = this.getSample(colIdx).getName();
        name = StringUtils.replaceChars(name, '_', ' ');
        if (this.getSample(colIdx).isSuspicious()) {
            retVal = em(name);
        } else {
            retVal = strong(name);
        }
        return retVal;
    }

}
