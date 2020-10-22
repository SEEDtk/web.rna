/**
 *
 */
package org.theseed.web.rna;

import org.apache.commons.lang3.StringUtils;
import org.theseed.rna.RnaData;

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
        if (dem == 0.0)
            retVal = Double.POSITIVE_INFINITY;
        else {
            double num = this.getWeight(feat, this.colIdx1);
            retVal = num / dem;
        }
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
    protected void init() {
        this.colIdx1 = this.getColIdx(this.getSample1());
        this.colIdx2 = this.getColIdx(this.sample2);
    }

}
