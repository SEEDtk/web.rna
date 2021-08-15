/**
 *
 */
package org.theseed.web.rna;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.rna.RnaFeatureData;
import org.theseed.utils.SetUtils;
import org.theseed.web.ColumnProcessor;

/**
 * This object uses a list of gene names to filter the rows displayed.
 *
 * @author Bruce Parrello
 *
 */
public class GeneListFilter extends RowFilter {

    // FIELDS
    /** set of gene names to use, or NULL to use all */
    private Set<String> names;

    public GeneListFilter(ColumnProcessor processor) {
        // Get the comma-delimited list of gene names.
        String geneNames = processor.getFilterGenes().toLowerCase();
        if (geneNames.isEmpty())
            this.names = null;
        else
            this.names = SetUtils.newFromArray(StringUtils.split(geneNames, ','));
    }

    @Override
    public boolean isRowDisplayable(RnaFeatureData feat) {
        return SetUtils.isMember(this.names, feat.getGene().toLowerCase());
    }

}
