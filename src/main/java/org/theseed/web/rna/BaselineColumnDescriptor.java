/**
 *
 */
package org.theseed.web.rna;

import org.theseed.rna.RnaFeatureData;
import org.theseed.web.Key.RevRatio;

import j2html.tags.DomContent;

/**
 * This column class displays a column based on the ratio of a sample's expression to the baseline values.
 *
 * @author Bruce Parrello
 *
 */
public class BaselineColumnDescriptor extends ColumnDescriptor {

    @Override
    protected boolean init() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public double getValue(RnaFeatureData feat) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public DomContent getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTitleString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTooltip() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevRatio getKey(RnaFeatureData feat) {
        // TODO Auto-generated method stub
        return null;
    }

}
