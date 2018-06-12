package net.stargraph.core.impl.nquads;

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
import net.stargraph.core.graph.BaseGraphModel;
import net.stargraph.core.graph.BatchFileGraphSource;
import net.stargraph.core.graph.FileGraphSource;
import net.stargraph.core.graph.batch.BatchFileGenerator;

import java.io.File;
import java.util.List;

/**
 * This is used since NQuadsFileGraphSource needs to load data in memory.
 */
public class NQuadsBatchFileGraphSource extends BatchFileGraphSource {
    private boolean includeDefaultGraph;
    private List<String> graphNames;

    public NQuadsBatchFileGraphSource(Stargraph stargraph, String dbId, String resource, String storeFilename, boolean required, long maxTriplesInFile, boolean includeDefaultGraph, List<String> graphNames) {
        super(stargraph, dbId, resource, storeFilename, required, maxTriplesInFile);
        this.includeDefaultGraph = includeDefaultGraph;
        this.graphNames = graphNames;
    }

    @Override
    protected BatchFileGenerator createBatchFileGenerator(File directory, long maxEntriesInFile, String fileNamePrefix) {
        return new NQuadsBatchFileGenerator(directory, maxEntriesInFile, fileNamePrefix);
    }

    @Override
    protected FileGraphSource createBatchFileGraphSource(Stargraph stargraph, String dbId, File file) {
        return new NQuadsFileGraphSource(stargraph, dbId, file.getAbsolutePath(), "", true, includeDefaultGraph, graphNames);
    }
}
