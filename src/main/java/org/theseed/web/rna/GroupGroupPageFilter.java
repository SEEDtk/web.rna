/**
 *
 */
package org.theseed.web.rna;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Bruce Parrello
 *
 */
public class GroupGroupPageFilter extends GroupPageFilter {

    // FIELDS
    /** ID of group of interest */
    private String group;

    /**
     * @param processor
     */
    public GroupGroupPageFilter(IParms processor) {
        super(processor);
        this.group = processor.getGroupId();
    }

    @Override
    public boolean isDisplay(String[] featureSpec) {
        boolean retVal = Arrays.stream(StringUtils.split(featureSpec[1], ',')).anyMatch(x -> x.contentEquals(this.group));
        return retVal;
    }

}
