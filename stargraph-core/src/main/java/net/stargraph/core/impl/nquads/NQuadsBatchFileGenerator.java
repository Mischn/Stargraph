package net.stargraph.core.impl.nquads;

import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.batch.BaseBatchFileGenerator;
import net.stargraph.core.graph.batch.BaseBatchStreamRDF;
import org.apache.jena.graph.Node;
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
                return formatNodeNT(triple.getSubject()) + " " + formatNodeNT(triple.getPredicate()) + " " + formatNodeNT(triple.getObject()) + " .";
            }
            private boolean outputGraphSlot(Node g) {
                return (g != null && g != Quad.tripleInQuad && !Quad.isDefaultGraph(g)) ;
            }
            private String formatQuad(Quad quad) {
                if (outputGraphSlot(quad.getGraph())) {
                    return formatNodeNT(quad.getSubject()) + " " + formatNodeNT(quad.getPredicate()) + " " + formatNodeNT(quad.getObject()) + " " + formatNodeNT(quad.getGraph())+ " .";
                } else {
                    return formatNodeNT(quad.getSubject()) + " " + formatNodeNT(quad.getPredicate()) + " " + formatNodeNT(quad.getObject()) + " .";
                }
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
