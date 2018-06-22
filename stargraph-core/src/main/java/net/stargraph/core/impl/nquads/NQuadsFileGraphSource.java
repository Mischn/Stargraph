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
import net.stargraph.core.graph.MGraphModel;
import net.stargraph.core.graph.batch.BatchFileGenerator;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NQuadsFileGraphSource extends BatchFileGraphSource {
    private boolean includeDefaultGraph;
    private List<String> graphNames; // if null, then all data is loaded

    public NQuadsFileGraphSource(Stargraph stargraph, String dbId, String resource, String storeFilename, boolean required, int batchMB, long maxEntriesInBatchFile, boolean includeDefaultGraph, List<String> graphNames) {
        super(stargraph, dbId, resource, storeFilename, required, batchMB, maxEntriesInBatchFile);
        this.includeDefaultGraph = includeDefaultGraph;
        this.graphNames = graphNames;
    }


    private List<String> getGraphNames(Dataset dataset) {
        List<String> res = new ArrayList<>();
        Iterator<String> it = dataset.listNames();
        while (it.hasNext()) {
            res.add(it.next());
        }
        return res;
    }

    @Override
    protected BatchFileGenerator createBatchFileGenerator() {
        return new NQuadsBatchFileGenerator(stargraph, dbId);
    }

    @Override
    protected FileGraphSource createBatchFileGraphSource(File file) {
        return new NQuadsFileGraphSource(stargraph, dbId, file.getAbsolutePath(), null, true, -1, 10_000_000, includeDefaultGraph, graphNames);
    }

    @Override
    public void _extend(BaseGraphModel graphModel, File file) {

        // Attention: The NQuads file is fully loaded into memory
        Dataset dataset = RDFDataMgr.loadDataset(file.getAbsolutePath());

        List<String> allGraphNames = getGraphNames(dataset);
        logger.info(marker, "Found {} named graphs: {}", allGraphNames.size(), allGraphNames);

        // determine graph names to use
        List<String> usedGraphNames = (graphNames == null)? allGraphNames : graphNames;

        if (includeDefaultGraph) {
            logger.info(marker, "Adding default graph..");
            Model other = dataset.getDefaultModel();

            MGraphModel otherModel = new MGraphModel(other);
            graphModel.add(otherModel);
        }

        for (String graphName : usedGraphNames) {
            if (!dataset.containsNamedModel(graphName)) {
                logger.warn(marker, "Named graph '{}' does not exist.", graphName);
            } else {
                logger.info(marker, "Adding named graph {}..", graphName);
                Model other = dataset.getNamedModel(graphName);

                MGraphModel otherModel = new MGraphModel(other);
                graphModel.add(otherModel);
            }
        }
    }
}
