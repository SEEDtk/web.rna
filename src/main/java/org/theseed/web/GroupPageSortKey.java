/**
 *
 */
package org.theseed.web;

import java.util.Comparator;

import org.theseed.locations.Location;

/**
 * This is a key type that permits sorting by location or count, depending on a static parameter.
 *
 * @author Bruce Parrello
 *
 */
public class GroupPageSortKey extends Key implements Comparable<GroupPageSortKey> {

    // FIELDS
    /** comparator for sorting */
    private static Comparator<GroupPageSortKey> COMPARATOR = Order.LOCATION;
    /** location of feature for this table row */
    private Location loc;
    /** ID of feature for this table row */
    private String fid;
    /** number of snip changes */
    private int changes;

    /**
     * Construct a sort key for a specified feature and change count.
     *
     * @param feat		feature for this table row
     * @param marks		number of significant changes to the feature
     */
    public GroupPageSortKey(org.theseed.genome.Feature feat, int marks) {
        this.loc = feat.getLocation();
        this.fid = feat.getId();
        this.changes = marks;
    }

    /**
     * This enum determines the ordering types.
     */
    public static enum Order implements Comparator<GroupPageSortKey> {
        LOCATION {
            @Override
            public int compare(GroupPageSortKey o1, GroupPageSortKey o2) {
                // Sort by location and then feature ID.
                int retVal = o1.loc.compareTo(o2.loc);
                if (retVal == 0)
                    retVal = o1.fid.compareTo(o2.fid);
                return retVal;
            }
        }, CHANGES {
            @Override
            public int compare(GroupPageSortKey o1, GroupPageSortKey o2) {
                // Sort by number of changes (highest first), then feature ID.
                int retVal = o2.changes - o1.changes;
                if (retVal == 0)
                    retVal = o1.fid.compareTo(o2.fid);
                return retVal;
            }
        };

        @Override
        public abstract int compare(GroupPageSortKey o1, GroupPageSortKey o2);

    }

    @Override
    public void store(CellContent cell, ColSpec col) {
        col.store(cell, this.fid);
    }

    @Override
    public int compareTo(GroupPageSortKey o) {
        return COMPARATOR.compare(this, o);
    }

    /**
     * Specify the sort ordering.
     *
     * @param sortOrder		new sort ordering
     */
    public static void setOrder(Order sortOrder) {
        COMPARATOR = sortOrder;
    }

}
