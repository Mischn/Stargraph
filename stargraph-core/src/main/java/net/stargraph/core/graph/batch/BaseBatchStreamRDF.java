package net.stargraph.core.graph.batch;

import org.apache.jena.riot.system.StreamRDF;

import java.io.File;

/**
 * Converts an RDF-file into a batch of (smaller) RDF-files.
 */
public abstract class BaseBatchStreamRDF extends BaseBatchStream implements StreamRDF {
    public BaseBatchStreamRDF(File directory, long maxEntriesInFile, String fileNamePrefix) {
        super(directory, maxEntriesInFile, fileNamePrefix);
    }
}
