package net.stargraph.core.impl.ntriples;

import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.batch.BaseBatchFileGenerator;
import net.stargraph.core.graph.batch.BaseBatchStreamRDF;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Quad;

import java.io.File;
import java.util.List;

public class NTriplesBatchFileGenerator extends BaseBatchFileGenerator {


    public NTriplesBatchFileGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
    }

    private BaseBatchStreamRDF createBatchStreamRDF(File directory, long maxEntriesInFile, String batchFileNamePrefix) {
        return new BaseBatchStreamRDF(directory, maxEntriesInFile, batchFileNamePrefix) {
            private String formatTriple(Triple triple) {
                return formatNodeNT(triple.getSubject()) + " " + formatNodeNT(triple.getPredicate()) + " " + formatNodeNT(triple.getObject()) + " .";
            }

            @Override
            protected String getFileExtension() {
                return ".nt";
            }

            @Override
            public void triple(Triple triple) {
                dumpLine(formatTriple(triple));
            }

            @Override
            public void quad(Quad quad) {
                // WARNING: this ignores the named graphs
                dumpLine(formatTriple(quad.asTriple()));
            }

            @Override
            public void base(String s) {

            }

            @Override
            public void prefix(String s, String s1) {

            }
        };
    }

    @Override
    protected List<File> generateBatches(File batchDirectory, File file, long maxEntriesInFile) {
        BaseBatchStreamRDF baseBatchStreamRDF = createBatchStreamRDF(batchDirectory, maxEntriesInFile, file.getName());
        RDFDataMgr.parse(baseBatchStreamRDF, file.getAbsolutePath());

        return baseBatchStreamRDF.getOutFiles();
    }
}
