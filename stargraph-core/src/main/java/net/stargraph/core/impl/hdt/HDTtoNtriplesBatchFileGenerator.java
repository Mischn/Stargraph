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
                triple.dumpNtriple(build);
                out.print(build);
            }
            out.close();
        } finally {
            if (hdt!=null){
                hdt.close();
            }
        }
    }

    /*
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
