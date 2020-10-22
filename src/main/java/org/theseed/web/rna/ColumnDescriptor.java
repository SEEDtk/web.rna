/**
 *
 */
package org.theseed.web.rna;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;
import org.theseed.reports.LinkObject;
import org.theseed.rna.RnaData;
import org.theseed.rna.RnaData.FeatureData;

import j2html.tags.DomContent;

/**
 * This object describes an output column for the RNA data.  It provides methods for converting to and from a string
 * (for storage) and abstract display methods for the heading cell and the data cells.  These are overridden by the
 * concrete subclasses.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ColumnDescriptor {

    /**
     *
     */
    private static final String COL_SEP_CHAR = ";";
    // FIELDS
    /** RNA data repository */
    private RnaData data;
    /** primary sample name */
    private String sample1;
    /** linker for HTML */
    private static final LinkObject linker = new LinkObject.Patric();

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
        retVal.init();
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
     */
    protected abstract void init();

    /**
     * @return the RNA repository column index for the specified sample
     *
     * @param sample	name of the target sample
     */
    protected int getColIdx(String sample) {
        return this.data.getColIdx(sample);
    }

    /**
     * @return the repository row for a feature
     *
     * @param feat	feature for the row
     */
    public RnaData.Row getRow(RnaData.FeatureData feat) {
        return this.data.getRow(feat);
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
    public abstract String getTitle();

    /**
     * @return the tooltip for this column
     */
    public abstract String getTooltip();

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
        String retVal;
        if (newColumn.length() <= 1) {
            // Here there is no new column.
            retVal = cookieString;
        } else if (cookieString.isEmpty()) {
            // Here there is ONLY the new column.
            retVal = newColumn;
        } else if (StringUtils.endsWith(cookieString, newColumn)) {
            // Here we are repeating the same column.  This is usually a mistake by the user.
            retVal = cookieString;
        } else {
            // Here we are really adding a new column to existing columns.
            retVal = cookieString + COL_SEP_CHAR + newColumn;
        }
        return retVal;
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
        String[] columns = StringUtils.split(cookieString, COL_SEP_CHAR);
        ColumnDescriptor[] retVal = new ColumnDescriptor[columns.length];
        for (int i = 0; i < columns.length; i++)
            retVal[i] = ColumnDescriptor.create(columns[i], data);
        return retVal;
    }

    /**
     * @return a hyperlink for the specified feature
     *
     * @param id	feature ID to link
     */
    public static DomContent fidLink(String id) {
        return linker.featureLink(id);
    }

}
