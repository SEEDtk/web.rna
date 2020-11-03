/**
 *
 */
package org.theseed.web.rna;

import org.apache.commons.lang3.StringUtils;
import org.theseed.reports.Key;
import org.theseed.rna.RnaData;
import org.theseed.rna.RnaData.FeatureData;

/**
 * This displays a column that contains the ratio between expression levels for two samples.
 *
 * @author Bruce Parrello
 *
 */
public class DifferentialColumnDescriptor extends ColumnDescriptor {

    // FIELDS
    /** denominator sample */
    private String sample2;
    /** column index of numerator sample */
    private int colIdx1;
    /** column index of denominator sample */
    private int colIdx2;

    /**
     * Construct a differential column.
     *
     * @param sample2	denominator sample for display
     */
    public DifferentialColumnDescriptor(String sample2) {
        this.sample2 = sample2;
    }

    @Override
    public double getValue(RnaData.FeatureData feat) {
        double retVal;
        double dem = this.getWeight(feat, this.colIdx2);
        double num = this.getWeight(feat, this.colIdx1);
        retVal = num / dem;
        return retVal;
    }

    @Override
    public String getTitle() {
        String retVal = this.getSample(this.colIdx1).getName() + " / " +
                this.getSample(this.colIdx2).getName();
        retVal = StringUtils.replaceChars(retVal, '_', ' ');
        return retVal;
    }

    @Override
    public String getTooltip() {
        String numTip = this.tipStringOf(this.colIdx1);
        String demTip = this.tipStringOf(this.colIdx2);
        String retVal = "Numerator: " + numTip + "  Denominator: " + demTip;
        return retVal;
    }

    @Override
    protected boolean init() {
        this.colIdx1 = this.getColIdx(this.getSample1());
        this.colIdx2 = this.getColIdx(this.sample2);
        return (this.colIdx1 >= 0 && this.colIdx2 >= 0);
    }

    @Override
    public Key.RevRatio getKey(FeatureData feat) {
        double dem = this.getWeight(feat, this.colIdx2);
        double num = this.getWeight(feat, this.colIdx1);
        return new Key.RevRatio(num, dem);
    }

}
