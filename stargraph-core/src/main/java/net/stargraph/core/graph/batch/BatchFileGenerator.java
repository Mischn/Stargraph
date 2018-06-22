package net.stargraph.core.graph.batch;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface BatchFileGenerator {
    void start() throws IOException;
    List<File> generateBatches(File file, long maxEntriesInFile) throws Exception;
    void end() throws IOException;
}
