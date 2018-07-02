package net.stargraph.core.graph.batch;

import net.stargraph.core.Stargraph;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseBatchFileGenerator implements BatchFileGenerator {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected Marker marker = MarkerFactory.getMarker("graph");

    protected Stargraph stargraph;
    protected String dbId;
    protected boolean started;
    protected File batchDirectory;
    protected List<File> batchFiles;

    public BaseBatchFileGenerator(Stargraph stargraph, String dbId) {
        this(stargraph, dbId, null);
    }

    public BaseBatchFileGenerator(Stargraph stargraph, String dbId, File batchDirectory) {
        this.stargraph = stargraph;
        this.dbId = dbId;
        this.started = false;
        this.batchDirectory = batchDirectory;
        this.batchFiles = new ArrayList<>();
    }

    private File createTmpDir() throws IOException {
        Path tmpPath = Files.createTempFile("stargraph-", "-graphBatchDir");
        logger.info(marker, "Create batch-dir '{}'", tmpPath.toString());
        Files.delete(tmpPath);
        Files.createDirectories(tmpPath);

        return tmpPath.toFile();
    }

    @Override
    public void start() throws IOException {
        if (batchDirectory == null) {
            batchDirectory = createTmpDir();
        }

        if (!batchDirectory.exists()) {
            logger.info(marker, "Create batch-dir '{}'", batchDirectory.toString());
            batchDirectory.mkdirs();
        }

        logger.info(marker, "Use batch-dir '{}'", batchDirectory.toString());
        batchFiles.clear();
        started = true;
    }

    protected abstract List<File> generateBatches(File batchDirectory, File file, long maxEntriesInFile) throws Exception;

    @Override
    public List<File> generateBatches(File file, long maxEntriesInFile) throws Exception {
        if (!started) {
            start();
        }
        batchFiles = generateBatches(batchDirectory, file, maxEntriesInFile);
        return batchFiles;
    }

    public File getBatchDirectory() {
        return batchDirectory;
    }

    public List<File> getBatchFiles() {
        return batchFiles;
    }

    @Override
    public void end() throws IOException {
        started = false;

        // remove batch-files
        batchFiles.forEach(f -> {
            logger.info(marker, "Delete batch-file '{}'", f.getAbsolutePath().toString());
            try {
                Files.delete(f.toPath());
            } catch (IOException e) {
                logger.error(marker, "Failed to delete batch-file '{}'", f.getAbsolutePath().toString());
            }
        });

    }
}
