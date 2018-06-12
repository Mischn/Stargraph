package net.stargraph.core.graph;

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
import net.stargraph.core.graph.batch.BatchFileGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Converts an RDF-file into a batch of (smaller) RDF-files and incrementally extends the graph model with these smaller files.
 * Note that DefaultFileGraphSource is sufficient in most cases since the model is not completely loaded in memory (using RDFDataMgr.read()).
 */
public abstract class BatchFileGraphSource extends FileGraphSource {
    private long maxTriplesInFile;

    public BatchFileGraphSource(Stargraph stargraph, String dbId, String resource, String storeFilename, boolean required, long maxTriplesInFile) {
        super(stargraph, dbId, resource, storeFilename, required);
        this.maxTriplesInFile = maxTriplesInFile;
    }

    protected abstract BatchFileGenerator createBatchFileGenerator(File directory, long maxEntriesInFile, String fileNamePrefix);
    protected abstract FileGraphSource createBatchFileGraphSource(Stargraph stargraph, String dbId, File file);

    @Override
    public void extend(BaseGraphModel graphModel, File file) throws IOException {

        // create tmp-dir
        Path path = Files.createTempFile("stargraph-", "-graphDir");
        Files.delete(path);
        Files.createDirectories(path);

        try {
            BatchFileGenerator batchFileGenerator = createBatchFileGenerator(path.toFile(), maxTriplesInFile, file.getName());

            logger.info(marker, "Convert '{}' into smaller batch-files (stored in tmp-dir: '{}')", file.getAbsolutePath(), path.toString());
            batchFileGenerator.generateBatches(file.getAbsolutePath());
            List<File> outFiles = batchFileGenerator.getOutFiles();
            logger.info(marker, "Created {} batch-files", outFiles.size());

            // iterate over files
            outFiles.forEach(f -> {
                FileGraphSource source = createBatchFileGraphSource(stargraph, dbId, f);
                source.extend(graphModel);
            });
        } catch (Exception e) {
            throw new StarGraphException(e);
        } finally {
            logger.info(marker, "Delete tmp-dir '{}'", path.toString());
            Files.delete(path);
        }
    }
}
