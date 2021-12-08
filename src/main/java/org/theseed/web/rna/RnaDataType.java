/**
 *
 */
package org.theseed.web.rna;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.utils.IDescribable;

/**
 * This enumeration controls access to the various RNA Seq databases.  The client
 * chooses a database type, and the enum provides the important characteristics
 * of the data.
 *
 * @author Bruce Parrello
 *
 */
public class RnaDataType implements IDescribable {

    /**
     * This class is the file name filter for cluste files.
     */
    public static class ClusterFilter implements FilenameFilter {

        private static Pattern NAME_PATTERN = Pattern.compile("CL\\d+\\.tpm\\.ser");

        @Override
        public boolean accept(File dir, String name) {
             return NAME_PATTERN.matcher(name).matches();
        }

    }

    // FIELDS
    /** base name of the file */
    private String baseName;
    /** engineered flag */
    private boolean engineered;
    /** database description */
    private String description;
    /** ordinal code number */
    private int ordinal;
    /** filter for cluster files */
    private static final FilenameFilter CLUSTER_FILTER = new ClusterFilter();

    /**
     * Construct an RNA data type from a database identifier.
     *
     * @param idCode	ID code for this RNA data type
     */
    public RnaDataType(String idCode) {
        if (idCode == null)
            throw new IllegalArgumentException("NULL specified for RNA data type.");
        this.baseName = idCode;
        switch (idCode) {
        case "tpm.ser" :
            this.engineered = true;
            this.description = "Threonine Project Samples";
            this.ordinal = 0;
            break;
        case "ecoli.ser" :
            this.engineered = false;
            this.description = "NCBI E coli Samples (slow)";
            this.ordinal = 1;
            break;
        default :
            this.engineered = false;
            String base = StringUtils.substringBefore(idCode, ".");
            this.description = "NCBI Cluster " + base;
            this.ordinal = 1 + Integer.valueOf(base.substring(2));
        }
    }

    /**
     * @return a list of the valid RNA data types
     *
     * @param workDir	workspace directory
     */
    public static RnaDataType[] values(File workDir) {
        // Get the cluster names.
        String[] clusters = workDir.list(CLUSTER_FILTER);
        // Prime with the two old values.
        RnaDataType[] retVal = new RnaDataType[clusters.length + 2];
        retVal[0] = new RnaDataType("tpm.ser");
        retVal[1] = new RnaDataType("ecoli.ser");
        // Now add the clusters.
        for (int i = 0; i < clusters.length; i++)
            retVal[i+2] = new RnaDataType(clusters[i]);
        return retVal;
    }

    /**
     * @return the base name of the file containing these samples
     */
    public String getFileName() {
        return this.baseName;
    }

    /**
     * @return TRUE if this database contains engineered samples using sample IDs
     */
    public boolean isEngineered() {
        return this.engineered;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String name() {
        return this.baseName;
    }

    /**
     * @return the ordinal code for this data type
     */
    public int ordinal() {
        return this.ordinal;
    }

}
