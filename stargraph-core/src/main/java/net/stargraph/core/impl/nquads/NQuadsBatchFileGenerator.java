package net.stargraph.core.impl.nquads;

import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.batch.BaseBatchFileGenerator;
import net.stargraph.core.graph.batch.BaseBatchStreamRDF;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Quad;

import java.io.File;
import java.util.List;

public class NQuadsBatchFileGenerator extends BaseBatchFileGenerator {


    public NQuadsBatchFileGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
    }

    private BaseBatchStreamRDF createBatchStreamRDF(File directory, long maxEntriesInFile, String batchFileNamePrefix) {
        return new BaseBatchStreamRDF(directory, maxEntriesInFile, batchFileNamePrefix) {
            private String formatTriple(Triple triple) {
                return formatNT(triple.getSubject()) + " " + formatNT(triple.getPredicate()) + " " + formatNT(triple.getObject()) + " .";
            }
            private String formatQuad(Quad quad) {
                String graphStr = (quad.getGraph() != null)? " " + formatNT(quad.getGraph()) : "";
                return formatNT(quad.getSubject()) + " " + formatNT(quad.getPredicate()) + " " + formatNT(quad.getObject()) + graphStr + " .";
            }

            @Override
            protected String getFileExtension() {
                return ".nq";
            }

            @Override
            public void triple(Triple triple) {
                write(formatTriple(triple));
            }

            @Override
            public void quad(Quad quad) {
                write(formatQuad(quad));
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
