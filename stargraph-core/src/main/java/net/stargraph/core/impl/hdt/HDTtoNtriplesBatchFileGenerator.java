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

import net.stargraph.StarGraphException;
import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.batch.BaseBatchFileGenerator;
import net.stargraph.core.impl.hdt.batch.BaseBatchStreamHDT;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class HDTtoNtriplesBatchFileGenerator extends BaseBatchFileGenerator {
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
    private static String formatTriple(TripleString tripleString, StringBuilder strb) throws Exception {
        strb.delete(0, strb.length());

        char t0 = tripleString.toString().charAt(0);
        if (t0 == '#') {
            // ERROR
            throw new StarGraphException("Invalid triple: " + tripleString.toString());
        }

        char s0 = tripleString.getSubject().charAt(0);
        if (s0 != '_' && s0 != '<') {
            strb.append('<').append(tripleString.getSubject()).append('>');
        } else {
            strb.append(tripleString.getSubject());
        }

        char p0 = tripleString.getPredicate().charAt(0);
        if (p0 == '<') {
            strb.append(' ').append(tripleString.getPredicate()).append(' ');
        } else {
            strb.append(" <").append(tripleString.getPredicate()).append("> ");
        }

        char o0 = tripleString.getObject().charAt(0);
        if (o0 == '"') {
            // the HDT's UnicodeEscape.escapeString(object.toString()) produced incorrect escape sequences!
            String objectStr = tripleString.getObject().toString();
            String lex = objectStr.substring(objectStr.indexOf("\"")+1, objectStr.lastIndexOf("\""));

            strb.append("\"" + escapeLexicalNT(lex) + "\"").append(" .");
        } else if (o0 != '_' && o0 != '<') {
            strb.append('<').append(tripleString.getObject()).append("> .");
        } else {
            strb.append(tripleString.getObject()).append(" .");
        }

        return strb.toString();
    }

    private BaseBatchStreamHDT createBatchStreamHDT(File directory, long maxEntriesInFile, String batchFileNamePrefix) {
        return new BaseBatchStreamHDT(directory, maxEntriesInFile, batchFileNamePrefix) {
            private StringBuilder strb = new StringBuilder();

            @Override
            public void triple(TripleString tripleString) {
                try {
                    dumpLine(formatTriple(tripleString, strb));
                } catch (Exception e) {
                    logger.error(marker, "Could not format and dump Triple: {}", tripleString.toString());
                }
            }

            @Override
            protected String getFileExtension() {
                return ".nt";
            }
        };
    }

    private class MyProgressListener implements ProgressListener {

        @Override
        public void notifyProgress(float level, String message) {
            System.out.println(message + "\t"+ Float.toString(level));
        }
    }

    @Override
    protected List<File> generateBatches(File batchDirectory, File file, long maxEntriesInFile) throws Exception {
        BaseBatchStreamHDT baseBatchStreamHDT = createBatchStreamHDT(batchDirectory, maxEntriesInFile, file.getName());
        baseBatchStreamHDT.start();

        HDT hdt;
        final boolean loadInMemory = false;
        if(loadInMemory) {
            hdt = HDTManager.loadIndexedHDT(file.getAbsolutePath(), new MyProgressListener());
        } else {
            hdt= HDTManager.mapIndexedHDT(file.getAbsolutePath(), new MyProgressListener());
        }

        try {
            IteratorTripleString it = hdt.search("","","");
            while (it.hasNext()) {
                TripleString tripleString = it.next();
                baseBatchStreamHDT.triple(tripleString);
            }
        } finally {
            if (hdt!=null){
                hdt.close();
            }
            baseBatchStreamHDT.finish();
        }

        return baseBatchStreamHDT.getOutFiles();
    }
}
