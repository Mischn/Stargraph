package net.stargraph.test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.stargraph.core.IndicesFactory;
import net.stargraph.core.Stargraph;
import net.stargraph.core.impl.lucene.LuceneIndicesFactory;
import net.stargraph.core.impl.lucene.LuceneSearcher;
import net.stargraph.core.index.Indexer;
import net.stargraph.core.search.Searcher;
import net.stargraph.model.KBId;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

public final class LuceneIndexerTest {

    private KBId kbId;
    private Stargraph core;
    private Indexer indexer;
    private Searcher searcher;


    @BeforeClass
    public void beforeClass() {
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.load().getConfig("stargraph");
        File dataRootDir = TestUtils.prepareObamaTestEnv();

        this.core = new Stargraph(config, false);
        core.setDataRootDir(dataRootDir);
        this.core.initialize();
        this.kbId = KBId.of("obama", "entities");

        IndicesFactory factory = new LuceneIndicesFactory();
        indexer = factory.createIndexer(kbId, core);
        indexer.start();
        searcher = factory.createSearcher(kbId, core);
        searcher.start();
    }


    @Test
    public void bulkLoadTest() throws Exception {
        indexer.load(true, -1);
        indexer.awaitLoader();
        searcher = new LuceneSearcher(kbId, core);
        Assert.assertTrue(searcher.countDocuments() > 0);
    }
}
