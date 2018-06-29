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
import net.stargraph.core.graph.*;
import net.stargraph.core.graph.batch.BatchFileGenerator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

import java.io.File;
import java.io.IOException;

public class HDTFileGraphSource extends BatchFileGraphSource {
    private boolean useIndex;

    public HDTFileGraphSource(Stargraph stargraph, String dbId, String resource, String storeFilename, boolean required, int batchMB, long maxEntriesInBatchFile, boolean useIndex) {
        super(stargraph, dbId, resource, storeFilename, required, batchMB, maxEntriesInBatchFile);
        this.useIndex = useIndex;
    }

    @Override
    protected BatchFileGenerator createBatchFileGenerator() {
        return new HDTtoNtriplesBatchFileGenerator(stargraph, dbId);
    }

    @Override
    protected FileGraphSource createBatchFileGraphSource(File file) {
        return new DefaultFileGraphSource(stargraph, dbId, file.getAbsolutePath(), null, true, -1, 10_000_000);
    }

    @Override
    public void _extend(BaseGraphModel graphModel, File file) throws IOException {
        HDT hdt = useIndex ? HDTManager.mapIndexedHDT(file.getAbsolutePath(), null) : HDTManager.loadHDT(file.getAbsolutePath(), null);
        HDTGraph graph = new HDTGraph(hdt);
        Model other = ModelFactory.createModelForGraph(graph);

        MGraphModel otherModel = new MGraphModel(other);
        graphModel.add(otherModel);
    }
}
