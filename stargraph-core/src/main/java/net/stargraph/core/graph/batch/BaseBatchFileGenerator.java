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
import java.util.List;

public abstract class BaseBatchFileGenerator implements BatchFileGenerator {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected Marker marker = MarkerFactory.getMarker("graph");

    protected Stargraph stargraph;
    protected String dbId;
    private Path tmpPath;

    public BaseBatchFileGenerator(Stargraph stargraph, String dbId) {
        this.stargraph = stargraph;
        this.dbId = dbId;
    }

    @Override
    public void start() throws IOException {
        // create tmp-dir
        tmpPath = Files.createTempFile("stargraph-", "-graphBatchDir");
        logger.info(marker, "Created tmp-dir '{}'", tmpPath.toString());
        Files.delete(tmpPath);
        Files.createDirectories(tmpPath);
    }

    protected abstract List<File> generateBatches(File batchDirectory, File file, long maxEntriesInFile) throws Exception;

    @Override
    public List<File> generateBatches(File file, long maxEntriesInFile) throws Exception {
        return generateBatches(tmpPath.toFile(), file, maxEntriesInFile);
    }

    @Override
    public void end() throws IOException {
        // remove tmp-dir
        logger.info(marker, "Delete tmp-dir '{}'", tmpPath.toString());
        FileUtils.deleteDirectory(tmpPath.toFile());
    }
}
