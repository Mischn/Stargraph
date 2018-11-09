package net.stargraph.core.rank;

import net.stargraph.core.model.ModelUtils;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.Entity;
import net.stargraph.rank.ModifiableRankParams;
import net.stargraph.rank.Rankable;
import net.stargraph.rank.Rankers;
import net.stargraph.rank.Scores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.stream.Collectors;

public final class RankersExt {
    private static Logger logger = LoggerFactory.getLogger(RankersExt.class);
    private static Marker marker = MarkerFactory.getMarker("rank");

    public static Scores apply(EntitySearcher entitySearcher, Scores inputScores, ModifiableRankParams rankParams, String target) {
        ModelUtils.bulkLookup(entitySearcher, inputScores.stream().map(s -> s.getEntry()).filter(e -> e instanceof Entity).map(e -> (Entity)e).collect(Collectors.toList()));
        return Rankers.apply(inputScores, rankParams, target);
    }

    public static Scores apply(EntitySearcher entitySearcher, Scores inputScores, ModifiableRankParams rankParams, Rankable target) {
        ModelUtils.bulkLookup(entitySearcher, inputScores.stream().map(s -> s.getEntry()).filter(e -> e instanceof Entity).map(e -> (Entity)e).collect(Collectors.toList()));
        return Rankers.apply(inputScores, rankParams, target);
    }

    public static double similarity(String t1, String t2, ModifiableRankParams rankParams) {
        return Rankers.similarity(t1, t2, rankParams);
    }

    // will return null if the similarity is below the threshold
    public static Double matchSimilarity(String t1, String t2, ModifiableRankParams rankParams) {
        return Rankers.matchSimilarity(t1, t2, rankParams);
    }
}
