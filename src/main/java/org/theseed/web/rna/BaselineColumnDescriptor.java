/**
 *
 */
package org.theseed.web.rna;

import org.apache.commons.lang3.StringUtils;
import org.theseed.rna.RnaFeatureData;
import org.theseed.web.Key;
import org.theseed.web.Key.RevRatio;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This column class displays a column based on the ratio of a sample's expression to the baseline values.
 *
 * @author Bruce Parrello
 *
 */
public class BaselineColumnDescriptor extends ColumnDescriptor {

    // FIELDS
    /** column being displayed as the numerator */
    private int colIdx;

    @Override
    protected boolean init() {
        this.colIdx = this.getColIdx(this.getSample1());
        return (this.colIdx >= 0);
    }

    @Override
    public double getValue(RnaFeatureData feat) {
        double num = this.getWeight(feat, this.colIdx);
        double retVal = num / feat.getBaseLine();
        return retVal;
    }

    @Override
    public DomContent getTitle() {
        DomContent retVal = join(this.computeName(this.colIdx), " / baseline");
        return retVal;
    }

    @Override
    public String getTitleString() {
         String retVal = this.getSample(this.colIdx).getName() + " / baseline";
         retVal = StringUtils.replaceChars(retVal, '_', ' ');
         return retVal;
    }

    @Override
    public String getTooltip() {
        return this.tipStringOf(this.colIdx);
    }

    @Override
    public RevRatio getKey(RnaFeatureData feat) {
        double dem = feat.getBaseLine();
        double num = this.getWeight(feat, this.colIdx);
        return new Key.RevRatio(num, dem);
    }

}
