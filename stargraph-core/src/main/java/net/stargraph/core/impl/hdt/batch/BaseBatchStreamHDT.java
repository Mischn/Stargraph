package net.stargraph.core.impl.hdt.batch;

import net.stargraph.core.graph.batch.BaseBatchStream;
import org.apache.jena.riot.system.StreamRDF;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;

/**
 * Converts an HDT-file into a batch of (smaller) RDF-files.
 */
public abstract class BaseBatchStreamHDT extends BaseBatchStream {
    public BaseBatchStreamHDT(File directory, long maxEntriesInFile, String fileNamePrefix) {
        super(directory, maxEntriesInFile, fileNamePrefix);
    }

    public abstract void triple(TripleString tripleString);
}
