/**
 *
 */
package org.theseed.web.rna;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.theseed.rna.RnaFeatureData;
import org.theseed.web.ColumnProcessor;

/**
 * This filter returns TRUE if the feature is in the specified operon, regulon, or modulon.
 *
 * @author Bruce Parrello
 *
 */
public class GroupRowFilter extends RowFilter {

    // FIELDS
    /** filtering group */
    private String groupName;
    /** group number for atomic regulons */
    private int groupNum;
    /** pattern for atomic regulon group names */
    private static final Pattern AR_PATTERN = Pattern.compile("AR(\\d+)");

    public GroupRowFilter(ColumnProcessor processor) {
        this.groupName = processor.getFilterGroup();
        Matcher m = AR_PATTERN.matcher(this.groupName);
        if (m.matches())
            this.groupNum = Integer.valueOf(m.group(1));
        else
            this.groupNum = -1;
    }

    @Override
    public boolean isRowDisplayable(RnaFeatureData feat) {
        boolean retVal;
        if (this.groupNum >= 0)
            retVal = feat.getAtomicRegulon() == this.groupNum;
        else {
            retVal = feat.getOperon().contentEquals(this.groupName);
            if (! retVal)
                retVal = Arrays.stream(feat.getiModulons()).anyMatch(x -> x.contentEquals(this.groupName));
        }
        return retVal;
    }

}
