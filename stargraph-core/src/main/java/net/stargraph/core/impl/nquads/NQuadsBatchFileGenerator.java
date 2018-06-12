package net.stargraph.core.impl.nquads;

import net.stargraph.core.graph.batch.BatchFileGenerator;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;

import java.io.File;

/**
 * Converts an NQuads-file into a batch of (smaller) NQuads-files.
 */
public class NQuadsBatchFileGenerator extends BatchFileGenerator {

    public NQuadsBatchFileGenerator(File directory, long maxEntriesInFile, String fileNamePrefix) {
        super(directory, maxEntriesInFile, fileNamePrefix);
    }

    @Override
    protected String getFileExtension() {
        return ".nq";
    }

    private String formatTriple(Triple triple) {
        return formatNT(triple.getSubject()) + " " + formatNT(triple.getPredicate()) + " " + formatNT(triple.getObject()) + " .";
    }

    private String formatQuad(Quad quad) {
        String graphStr = (quad.getGraph() != null)? " " + formatNT(quad.getGraph()) : "";
        return formatNT(quad.getSubject()) + " " + formatNT(quad.getPredicate()) + " " + formatNT(quad.getObject()) + graphStr + " .";
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected void onFinish() {

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
}
