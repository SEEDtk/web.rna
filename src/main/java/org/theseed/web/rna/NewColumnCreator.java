/**
 *
 */
package org.theseed.web.rna;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.reports.NaturalSort;
import org.theseed.utils.IDescribable;

/**
 * This is the base class for adding new columns to the cookie string.
 *
 * @author Bruce Parrello
 *
 */
public abstract class NewColumnCreator {

    /**
     * Enum for new-column creation strategies
     */
    public static enum Type implements IDescribable {
        TIME1("iterate through all times for Primary"),
        TIMES("compare same times for Primary over Optional"),
        SINGLE("add one column");

        private String description;

        private Type(String desc) {
            this.description = desc;
        }

        /**
         * Create a new-column creation object for this strategy.
         *
         * @param samp1	ID of first sample
         * @param samp2	ID of second sample
         * @param samps	list of samples
         */
        public NewColumnCreator create(String samp1, String samp2, List<String> samps) {
            NewColumnCreator retVal = null;
            switch (this) {
            case TIME1 :
                retVal = new Time1NewColumnCreator(samp1, samp2, samps);
                break;
            case TIMES :
                retVal = new TimeBothNewColumnCreator(samp1, samp2, samps);
                break;
            case SINGLE:
                retVal = new SingleNewColumnCreator(samp1, samp2, samps);
                break;
            }
            return retVal;
        }

        @Override
        public String getDescription() {
            return this.description;
        }
    }

    // FIELDS
    /** ID of the first sample */
    private String sample1;
    /** ID of the second sample */
    private String sample2;
    /** list of samples */
    private List<String> samples;
    /** array index of time point */
    private static final int TIME_INDEX = 8;

    /**
     * Construct a new column creator.
     *
     * @param samp1	ID of first sample
     * @param samp2	ID of second sample
     * @param samps	list of samples
     */
    public NewColumnCreator(String samp1, String samp2, List<String> samps) {
        this.sample1 = samp1;
        this.sample2 = samp2;
        this.samples = samps;
    }

    /**
     * Get all the times for a specified sample.
     *
     * @param samp	ID of the sample of interest
     *
     * @return a map of all the represented times to the appropriate samples
     */
    protected SortedMap<String, String> getAllTimes(String samp) {
        SortedMap<String, String> retVal = new TreeMap<String, String>(new NaturalSort());
        // Create a pattern to match this sample's time points.
        String[] parts = StringUtils.split(samp, '_');
        parts[TIME_INDEX] = "([0-9p]+)";
        Pattern pattern = Pattern.compile(StringUtils.join(parts, '_'));
        for (String sample : this.samples) {
            Matcher m = pattern.matcher(sample);
            if (m.matches())
                retVal.put(m.group(1), sample);
        }
        return retVal;
    }

    /**
     * @return the first sample's ID
     */
    protected String getSample1() {
        return this.sample1;
    }

    /**
     * @return the second sample's ID
     */
    protected String getSample2() {
        return this.sample2;
    }

    /**
     * @return the list of column specifiers to add to the cookie string
     */
    public abstract List<String> getNewColumns();

}
