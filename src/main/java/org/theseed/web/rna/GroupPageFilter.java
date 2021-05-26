/**
 *
 */
package org.theseed.web.rna;

import java.util.BitSet;

import org.theseed.utils.IDescribable;

/**
 * This is the base class for filtering the group page features.  We can filter by group name, by changes in certain
 * genome subsets, or by changes in all genomes.
 *
 * @author Bruce Parrello
 *
 */
public abstract class GroupPageFilter {

    // FIELDS
    /** controlling processor */
    private IParms processor;

    /**
     * This interface must be provided by the controlling processor for extracting parameters.
     */
    public interface IParms {

        /**
         * @return the ID of the group of interest
         */
        public String getGroupId();

        /**
         * @return the set of column indices of interest
         */
        public BitSet getColumns();

        /**
         * @return the region of interest
         */
        public int getRegionIndex();

    }

    /**
     * This enum describes the filter types.
     */
    public static enum Type implements IDescribable {
        ALL_CHANGE {
            @Override
            public GroupPageFilter create(IParms processor) {
                return new AllGroupPageFilter(processor);
            }

            @Override
            public String getDescription() {
                return "Show features with changes in all displayed genomes.";
            }
        }, ANY_CHANGE {
            @Override
            public GroupPageFilter create(IParms processor) {
                return new AnyGroupPageFilter(processor);
            }

            @Override
            public String getDescription() {
                return "Show features with changes in any displayed genome.";
            }
        }, GROUP {
            @Override
            public GroupPageFilter create(IParms processor) {
                return new GroupGroupPageFilter(processor);
            }

            @Override
            public String getDescription() {
                return "Show features in selected group.";
            }
        };

        /**
         * @return a group page filter of the specified type
         *
         * @param processor		controlling processor
         */
        public abstract GroupPageFilter create(IParms processor);

    }

    /**
     * Construct a group page filter.
     *
     * @param processor		controlling processor
     */
    public GroupPageFilter(IParms processor) {
        this.processor = processor;
    }

    /**
     * @return TRUE if we should display this feature, else FALSE
     *
     * @param featureSpec	feature record to check
     */
    public abstract boolean isDisplay(String[] featureSpec);

    /**
     * @return the controlling processor
     */
    protected IParms getProcessor() {
        return this.processor;
    }

}
