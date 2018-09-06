package net.stargraph.core.query.filter;

import net.stargraph.model.Document;
import net.stargraph.model.PassageExtraction;
import net.stargraph.model.date.TimeRange;
import net.stargraph.rank.ModifiableRankParams;
import net.stargraph.rank.Rankers;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FilterQueryEngine {

    private static class MatchResult {
        private int referenceId;
        private int matchedId;
        private double score;

        public MatchResult(int referenceId, int matchedId, double score) {
            this.referenceId = referenceId;
            this.matchedId = matchedId;
            this.score = score;
        }

        public int getReferenceId() {
            return referenceId;
        }

        public int getMatchedId() {
            return matchedId;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            return "MatchResult{" +
                    "referenceId=" + referenceId +
                    ", matchedId=" + matchedId +
                    ", score=" + score +
                    '}';
        }
    }

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("query");


    private double termMatchScore(List<String> referenceTerms, List<String> terms, ModifiableRankParams rankParams) {
        double score = 0d;

        for (String referenceTerm : referenceTerms) {
            double referenceHighScore = 0d;
            for (String term : terms) {
                double sim = Rankers.similarity(referenceTerm, term, rankParams);
                if (sim > referenceHighScore) {
                    referenceHighScore = sim;
                }
            }
            score += referenceHighScore;
        }
        score = score *1./referenceTerms.size();
        return score;
    }


    private List<MatchResult> matchTerms(List<String> referenceTerms, List<String> terms, ModifiableRankParams rankParams, boolean allowMultiMatch) {
        List<MatchResult> res = new ArrayList<>();
        Set<Integer> matchedTexts = new HashSet<>();

        for (int i = 0; i < referenceTerms.size(); i++) {
            String referenceText = referenceTerms.get(i);

            // try to match reference text
            for (int j = 0; j < terms.size(); j++) {
                if (allowMultiMatch || !matchedTexts.contains(j)) {
                    String term = terms.get(j);

                    Double sim = Rankers.matchSimilarity(term, referenceText, rankParams);
                    if (sim != null) {
                        res.add(new MatchResult(i, j, sim));
                        matchedTexts.add(j);
                        break;
                    }
                }
            }
        }

        return res;
    }

    private List<MatchResult> matchTemporals(List<TimeRange> referenceTemporals, List<TimeRange> temporals, boolean allowMultiMatch) {
        List<MatchResult> res = new ArrayList<>();
        Set<Integer> matchedTemporals = new HashSet<>();

        for (int i = 0; i < referenceTemporals.size(); i++) {
            TimeRange referenceTemporal = referenceTemporals.get(i);

            // try to match reference text
            for (int j = 0; j < temporals.size(); j++) {
                if (allowMultiMatch || !matchedTemporals.contains(j)) {
                    TimeRange temporal = temporals.get(j);

                    if (referenceTemporal.containsInterval(temporal)) {
                        res.add(new MatchResult(i, j, 1.0));
                        matchedTemporals.add(j);
                        break;
                    }
                }
            }
        }

        return res;
    }

    private FilterResult matchFilters(Document document, List<PassageExtraction> queryFilters, ModifiableRankParams relationRankParams, ModifiableRankParams termRankParams) {
        FilterResult filterResult = new FilterResult(queryFilters, document.getId(), document.getEntity());
        for (FilterResult.SingleFilterResult sfr : filterResult.getSingleFilterResults()) {
            PassageExtraction f = sfr.getFilter();

            logger.debug(marker, "Check query passage-extraction: {}", f);

            // rank document's extractions to the target extraction
            Scores passageExScores = new Scores(document.getPassageExtractions().stream().map(pe -> new Score(pe, 0)).collect(Collectors.toList()));
            Scores ranked = Rankers.apply(passageExScores, relationRankParams, f);

            // TODO implement that "recuperate" can also match with "recuperation" (which is not a relation)


            if (ranked.size() > 0) {

                // matched relation
                PassageExtraction matchedEx = (PassageExtraction) ranked.get(0).getEntry();
                sfr.setMatchedRelation(matchedEx.getRelation(), ranked.get(0).getValue());

                // try match term arguments
                List<MatchResult> relationTermMatches = matchTerms(f.getTerms(), matchedEx.getTerms(), termRankParams, true);
                for (MatchResult m : relationTermMatches) {
                    sfr.addMatchedTerm(m.getReferenceId(), matchedEx.getTerms().get(m.getMatchedId()), m.getScore());
                }

                // try match temporal arguments
                List<MatchResult> relationTemporalMatches = matchTemporals(f.getTemporals(), matchedEx.getTemporals(), true);
                for (MatchResult m : relationTemporalMatches) {
                    sfr.addMatchedTemporal(m.getReferenceId(), matchedEx.getTemporals().get(m.getMatchedId()), m.getScore());
                }
            }

            logger.debug(marker, sfr.toString());
        }

        return filterResult;
    }



    private double determineFilterScore(FilterResult filterResult) {
        double maxScore = 0;
        double score = 0;

        for (FilterResult.SingleFilterResult sfr : filterResult.getSingleFilterResults()) {

            // relation
            maxScore += 1;
            score += (sfr.isMatchedRelation()) ? 1 * sfr.getMatchedRelation().getValue() : 0;

            // terms
            maxScore += sfr.getFilter().getTerms().size();
            for (Score s : sfr.getMatchedTerms()) {
                if (s != null) {
                    score += s.getValue();
                }
            }

            // temporals
            maxScore += sfr.getFilter().getTemporals().size();
            for (Score s : sfr.getMatchedTemporals()) {
                if (s != null) {
                    score += s.getValue();
                }
            }
        }

        return (maxScore == 0)? 0 : (score / maxScore);
    }


    private List<String> splitStr(String str) {
        return Arrays.asList(str.split("\\s+"));
    }

    public Score rerankDocuments(Score documentScore, List<PassageExtraction> queryFilters, ModifiableRankParams relationRankParams, ModifiableRankParams termRankParams, List<FilterResult> filterResults) {
        Document document = (Document) documentScore.getEntry();

        // score wrt to terms
        logger.debug(marker, "Score document {} with respect to terms:");
        List<String> queryPhrases = new ArrayList<>();
        List<String> queryTerms = new ArrayList<>();
        for (PassageExtraction queryFilter : queryFilters) {
            queryPhrases.add(queryFilter.getRelation());
            for (String s : splitStr(queryFilter.getRelation())) {
                queryTerms.add(s);

            }
            for (String term : queryFilter.getTerms()) {
                queryPhrases.add(term);
                for (String s : splitStr(term)) {
                    queryTerms.add(s);
                }
            }
        }


        List<String> docPhrases = new ArrayList<>();
        List<String> docTerms = new ArrayList<>();
        for (String term : document.getTerms()) {
            docPhrases.add(term);
            for (String s : splitStr(term)) {
                docTerms.add(s);
            }
        }


        double termScore = termMatchScore(queryTerms, docTerms, termRankParams);
        double phraseScore = termMatchScore(queryPhrases, docPhrases, termRankParams);

        logger.info(marker, "Phrases {} to match with {}", docPhrases, queryPhrases);
        logger.info(marker, "Score: {}", phraseScore);

        logger.info(marker, "Terms {} to match with {}", docTerms, queryTerms);
        logger.info(marker, "Score: {}", termScore);




        // score wrt. to filters
//        logger.debug(marker, "Score document {} with respect to filters", document.getId());
//        FilterResult filterResult = matchFilters(document, queryFilters, relationRankParams, termRankParams);
//        filterResults.add(filterResult);
//        double filterScore = determineFilterScore(filterResult);
        double filterScore = 0;


        // calculate final score (take into account the old document score, a small factor is sufficient to have at least some ranking even if no filters match)
        final int oldScoreBoost = 0;
        final int phraseScoreBoost = 10;
        final int termScoreBoost = 1;
        final int filterScoreBoost = 0;

        final int sum = oldScoreBoost + phraseScoreBoost + termScoreBoost + filterScoreBoost;

        double finalScore = (documentScore.getValue() * (oldScoreBoost * 1./sum)) + + (phraseScore * (phraseScoreBoost * 1./sum)) + (termScore * (termScoreBoost * 1./sum)) + (filterScore * (filterScoreBoost * 1. / sum));

        return new Score(document, finalScore);
    }
}
