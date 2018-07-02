package net.stargraph.core.impl.hdt;

/*-
 * ==========================License-Start=============================
 * stargraph-core
 * --------------------------------------------------------------------
 * Copyright (C) 2017 Lambda^3
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ==========================License-End===============================
 */

import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.batch.BaseBatchFileGenerator;
import net.stargraph.core.impl.ntriples.NTriplesBatchFileGenerator;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;

public class HDTtoNtriplesBatchFileGenerator extends BaseBatchFileGenerator {
    private NTriplesBatchFileGenerator nTriplesBatchFileGenerator;

    public HDTtoNtriplesBatchFileGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
    }

    private static String escapeLexicalNT(String lexical) {
        return lexical.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replaceAll("\\\\(?![\\\"nrt])", "\\\\\\\\");
    }

    // Corrected version of HDT's TripleString.dumpNtriple(Appendable out)
    private static void formatTriple(TripleString tripleString, Appendable out) throws IOException {
        final String subject = tripleString.getSubject().toString().trim();
        final String predicate = tripleString.getPredicate().toString().trim();
        final String object = tripleString.getObject().toString().trim();

        char s0 = subject.charAt(0);
        if (s0 != '_' && s0 != '<') {
            out.append('<').append(subject).append('>');
        } else {
            out.append(subject);
        }

        char p0 = predicate.charAt(0);
        if (p0 == '<') {
            out.append(' ').append(predicate).append(' ');
        } else {
            out.append(" <").append(predicate).append("> ");
        }

        char o0 = object.charAt(0);
        if (o0 == '"') {
            // the HDT's UnicodeEscape.escapeString(object.toString()) produced incorrect escape sequences!
            String lex = object.substring(object.indexOf("\"")+1, object.lastIndexOf("\""));
            out.append("\"" + escapeLexicalNT(lex) + "\"").append(" .\n");
        } else if (o0 != '_' && o0 != '<') {
            out.append('<').append(object).append("> .\n");
        } else {
            out.append(object).append(" .\n");
        }
    }

    public void convertToNTFile(File HDTFile, File outFile) throws Exception {
        logger.info(marker, "Fully convert HDT file {} into NTriple file {}", HDTFile.getAbsolutePath(), outFile.getAbsolutePath());
        PrintStream out = new PrintStream(outFile, "UTF-8");

        HDT hdt = HDTManager.mapHDT(HDTFile.getAbsolutePath(), new ProgressListener() {
            @Override
            public void notifyProgress(float level, String message) {
                System.out.println(message + "\t"+ Float.toString(level));
            }
        });
        try {
            IteratorTripleString it = hdt.search("","","");
            StringBuilder build = new StringBuilder(1024);
            while(it.hasNext()) {
                TripleString triple = it.next();

                build.delete(0, build.length());
                try {
                    formatTriple(triple, build);
                } catch (Exception e) {
                    logger.error(marker, "Could not format Triple: {}", triple.toString());
                }
                out.print(build.toString());
            }
            out.close();
        } finally {
            if (hdt!=null){
                hdt.close();
            }
        }
    }

    /*
    // Problem using this method: resulting NT-file contained error-triples (when loading dbpedia-dump).
    // E.g: <#> <http://dbpedia.org/resource/Étienne_de_Boré> <http://xmlns.com/foaf/0.1/depiction> <BAD URI: Illegal character in path at index 58: http://commons.wikimedia.org/wiki/Special:FilePath/Etienne de Bor%C3%A9.gif> .
    private void convertToNTFile2(File HDTFile, File outFile, boolean useIndex) throws IOException {
        logger.info(marker, "Fully convert HDT file {} into NTriple file {}", HDTFile.getAbsolutePath(), outFile.getAbsolutePath());
        HDT hdt = useIndex ? HDTManager.mapIndexedHDT(HDTFile.getAbsolutePath(), null) : HDTManager.loadHDT(HDTFile.getAbsolutePath(), null);
        HDTGraph graph = new HDTGraph(hdt);
        //Model other = ModelFactory.createModelForGraph(graph);

        FileOutputStream fos = new FileOutputStream(outFile);
        RDFDataMgr.write(fos, graph, RDFFormat.NT);
    }
    */

    @Override
    public void start() throws IOException {
        nTriplesBatchFileGenerator = new NTriplesBatchFileGenerator(stargraph, dbId);
        nTriplesBatchFileGenerator.start();
        super.start();
    }

    @Override
    protected List<File> generateBatches(File batchDirectory, File file, long maxEntriesInFile) throws Exception {
        // fully convert HDT to NTriples
        File ntriplesFile = Paths.get(batchDirectory.getAbsolutePath(), "HDTtriples.nt").toFile();

        //convertToNTFile2(file, ntriplesFile, useIndex);
        convertToNTFile(file, ntriplesFile);

        // batch-load nt-file
        return nTriplesBatchFileGenerator.generateBatches(ntriplesFile, maxEntriesInFile);
    }

    @Override
    public void end() throws IOException {
        super.end();
        nTriplesBatchFileGenerator.end();

    }
}
