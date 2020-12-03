/**
 *
 */
package org.theseed.web.rna;

import org.theseed.proteins.SampleId;
import org.theseed.web.HtmlTable;
import org.theseed.web.Key;

import j2html.tags.DomContent;

/**
 * This interface is used for operations that create a table of sample production data.
 *
 * @author Bruce Parrello
 *
 */
public interface IProductionTable {

    /**
     * Record a single sample.
     *
     * @param sample		ID of the sample
     * @param production	predicted production of the sample
     * @param actual		actual production of the sample (NaN if none)
     * @param growth		growth of the sample (NaN if none)
     */
    void recordSample(SampleId sample, double production, double actual, double growth);

    /**
     * @return the table produced
     */
    HtmlTable<? extends Key> closeTable();

    /**
     * @return a summary paragraph
     */
    DomContent getSummary();
}
