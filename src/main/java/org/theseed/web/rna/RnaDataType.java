/**
 *
 */
package org.theseed.web.rna;

import org.theseed.utils.IDescribable;

/**
 * This enumeration controls access to the various RNA Seq databases.  The client
 * chooses a database type, and the enum provides the important characteristics
 * of the data.
 *
 * @author Bruce Parrello
 *
 */
public enum RnaDataType implements IDescribable {
    ENGINEERED {
        @Override
        public String getDescription() {
            return "Threonine Project Samples";
        }

        @Override
        public boolean isEngineered() {
            return true;
        }

        @Override
        public String getFileName() {
            return "tpm.ser";
        }

    }, BASELINE {
        @Override
        public String getDescription() {
            return "NCBI E coli Samples";
        }

        @Override
        public boolean isEngineered() {
            return false;
        }

        @Override
        public String getFileName() {
            return "ecoli.ser";
        }
    };

    /**
     * @return the base name of the file containing these samples
     */
    public abstract String getFileName();

    /**
     * @return TRUE if this database contains engineered samples using sample IDs
     */
    public abstract boolean isEngineered();

}
