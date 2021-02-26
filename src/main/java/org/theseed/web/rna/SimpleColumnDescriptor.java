/**
 *
 */
package org.theseed.web.rna;

import org.apache.commons.lang3.StringUtils;
import org.theseed.rna.RnaData;
import org.theseed.rna.RnaData.FeatureData;
import org.theseed.web.Key;

import j2html.tags.DomContent;

/**
 * This class contains a column that just displays the expression levels for a sample.
 *
 * @author Bruce Parrello
 *
 */
public class SimpleColumnDescriptor extends ColumnDescriptor {

    // FIELDS
    /** index of relevant data column */
    private int colIdx;

    @Override
    public double getValue(RnaData.FeatureData feat) {
        return this.getWeight(feat, this.colIdx);
    }

    @Override
    public String getTitleString() {
        String retVal = this.getSample(this.colIdx).getName();
        retVal = StringUtils.replaceChars(retVal, '_', ' ');
        return retVal;
    }

    @Override
    public String getTooltip() {
        return this.tipStringOf(this.colIdx);
    }

    @Override
    protected boolean init() {
        this.colIdx = this.getColIdx(this.getSample1());
        return (this.colIdx >= 0);
    }

    @Override
    public Key.RevRatio getKey(FeatureData feat) {
        return new Key.RevRatio(this.getValue(feat), 1.0);
    }

    @Override
    public DomContent getTitle() {
        DomContent retVal = this.computeName(this.colIdx);
        return retVal;
    }


}
