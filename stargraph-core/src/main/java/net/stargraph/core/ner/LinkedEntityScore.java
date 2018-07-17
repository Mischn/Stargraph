package net.stargraph.core.ner;

import net.stargraph.model.LabeledEntity;
import net.stargraph.rank.Score;

public class LinkedEntityScore extends Score {
    private String dbId;

    public LinkedEntityScore(LabeledEntity entry, String dbId, double score) {
        super(entry, score);
        this.dbId = dbId;
    }

    public String getDbId() {
        return dbId;
    }
}
