package net.stargraph.core.graph.batch;

import net.stargraph.StarGraphException;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts an RDF-file into a batch of (smaller) RDF-files.
 */
public abstract class BaseBatchStreamRDF implements StreamRDF {
    private static final String DEFAULT_DATATYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";
    private static Logger logger = LoggerFactory.getLogger(BaseBatchStreamRDF.class);
    private static Marker marker = MarkerFactory.getMarker("core");

    private final File directory;
    private final long maxEntriesInFile;
    private final String fileNamePrefix;
    private int fileCounter;

    private long fileEntryCounter;
    private long entryCounter;
    private PrintWriter printWriter;
    private List<File> outFiles;

    public BaseBatchStreamRDF(File directory, long maxEntriesInFile, String fileNamePrefix) {
        this.directory = directory;
        this.maxEntriesInFile = maxEntriesInFile;
        this.fileNamePrefix = fileNamePrefix;
        this.outFiles = new ArrayList<>();
    }

    private static String escapeLexicalNT(String lexical) {
        return lexical.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replaceAll("(?<!\\\\)\\\\(?![\\\\\\\"nrt])", "\\\\\\\\");
    }

    protected static String formatNT(Node node) {
        if (node.isBlank()) {
            return "_:" + node.toString();
        }
        if (node.isURI()) {
            return "<" + node.toString() + ">";
        }
        if (node.isVariable()) {
            throw new AssertionError("Variables should not occur");
        }
        if (node.isLiteral()) {
            String lexicalStr = "\"" + escapeLexicalNT(node.getLiteralLexicalForm()) + "\"";
            String languageStr = (node.getLiteralLanguage().length() <= 0)? "" : "@" + node.getLiteralLanguage();
            String datatypeStr = (node.getLiteralDatatypeURI().length() <= 0 || node.getLiteralDatatypeURI().toLowerCase().equals(DEFAULT_DATATYPE.toLowerCase()))? "" : "^^<" + node.getLiteralDatatypeURI() + ">";

            return lexicalStr + languageStr + datatypeStr;
        }

        return node.toString();
    }

    protected abstract String getFileExtension();

    // use this in inherited classes
    protected void write(String line) {
        printWriter.println(line);
        ++fileEntryCounter;
        ++entryCounter;

        if (fileEntryCounter >= maxEntriesInFile) {
            flush(false);
        }
    }

    private void flush(boolean keepClosed) {
        try {
            if (printWriter != null) {
                printWriter.flush();
                printWriter.close();
                logger.info(marker, "Stored {} triples into file '{}'", fileEntryCounter, outFiles.get(outFiles.size()-1).getAbsolutePath());
                fileEntryCounter = 0;
            }
            if (!keepClosed) {
                String ext = (getFileExtension().startsWith("."))? getFileExtension() : "." + getFileExtension();
                File outFile = Paths.get(directory.getAbsolutePath(), String.format("%s-%d%s", fileNamePrefix, ++fileCounter, ext)).toFile();
                printWriter = new PrintWriter(outFile, "UTF-8");
                outFiles.add(outFile);
            }
        } catch (Exception e) {
            throw new StarGraphException(e);
        }
    }

    @Override
    public void start() {
        fileCounter = 0;
        fileEntryCounter = 0;
        entryCounter = 0;
        printWriter = null;
        outFiles.clear();
        flush(false);
    }

    @Override
    public void finish() {
        flush(true);
        printWriter = null;
    }

    public List<File> getOutFiles() {
        return outFiles;
    }

    public long getEntryCounter() {
        return entryCounter;
    }
}
