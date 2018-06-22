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
import java.util.List;

/**
 * Converts an RDF-file into a batch of (smaller) RDF-files and incrementally extends the graph model with these smaller files.
 */
public abstract class BatchFileGraphSource extends FileGraphSource {
    private final int batchMB; // if the file-size is >= batchMB, batching is activated
    private final long maxEntriesInBatchFile;

    public BatchFileGraphSource(Stargraph stargraph, String dbId, String resource, String storeFilename, boolean required, int batchMB, long maxEntriesInBatchFile) {
        super(stargraph, dbId, resource, storeFilename, required);
        this.batchMB = batchMB;
        this.maxEntriesInBatchFile = maxEntriesInBatchFile;
    }

    protected abstract BatchFileGenerator createBatchFileGenerator();
    protected abstract FileGraphSource createBatchFileGraphSource(File file);
    protected abstract void _extend(BaseGraphModel graphModel, File file) throws Exception;

    @Override
    public void extend(BaseGraphModel graphModel, File file) throws Exception {
        BatchFileGenerator batchFileGenerator = createBatchFileGenerator();
        if (batchFileGenerator == null) {
            logger.info(marker, "No batch-file-generator specified");
            _extend(graphModel, file);
            return;
        }

        double fileMB = ((file.length() / 1024.) / 1024.);
        if (fileMB >= batchMB && batchMB >= 0) {
            // use batch-loading
            logger.info(marker, "File size {} exceeds the specified size {}. Use batch-loading.", fileMB, batchMB);

            try {
                batchFileGenerator.start();
                List<File> batchFiles = batchFileGenerator.generateBatches(file, maxEntriesInBatchFile);
                batchFiles.forEach(f -> {
                    try {
                        FileGraphSource fileGraphSource = createBatchFileGraphSource(f);
                        if (fileGraphSource == null) {
                            throw new StarGraphException("No FileGraphSource specified");
                        }
                        logger.info(marker, "Loading batch-file {}.", f.getAbsolutePath());
                        fileGraphSource.extend(graphModel, f);
                    } catch (Exception e) {
                        logger.error(marker, "Failed to extend graph model with batch file {}", f);
                        throw new StarGraphException(e);
                    }
                });
            } catch (Exception e) {
                throw new StarGraphException(e);
            } finally {
                batchFileGenerator.end();
            }
        } else {
            _extend(graphModel, file);
        }
    }
}
