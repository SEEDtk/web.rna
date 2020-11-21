/**
 *
 */
package org.theseed.web.rna;

import org.theseed.utils.IDescribable;

/**
 * This enumeration determines how the tabular report on a scatter page is sorted.
 *
 * @author Bruce Parrello
 *
 */
public enum ScatterSort implements IDescribable {
    PRODUCTION("Sort by production"), PREDICTION("Sort by prediction");

    private String description;

    private ScatterSort(String desc) {
        this.description = desc;
    }

    /**
     * @return the sort value for a point
     */
    public double sortValue(double x, double y) {
        return (this == PRODUCTION ? x : y);
    }

    @Override
    public String getDescription() {
        return this.description;
    }

}
