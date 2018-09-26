package net.stargraph.rank.impl;

/*-
 * ==========================License-Start=============================
 * stargraph-model
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

import net.stargraph.rank.ModifiableIndraParams;
import net.stargraph.rank.Rankable;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.lambda3.indra.client.RelatednessRequest;
import org.lambda3.indra.client.RelatednessResponse;
import org.lambda3.indra.client.ScoredTextPair;
import org.lambda3.indra.client.TextPair;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

public final class IndraRanker extends BaseRanker {
    private static final Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
        public int compare(String str1, String str2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
            if (res == 0) {
                res = str1.compareTo(str2);
            }
            return res;
        }
    };

    private ModifiableIndraParams params;
    private Client client;


    public IndraRanker(ModifiableIndraParams params) {
        this.params = Objects.requireNonNull(params);
        client = ClientBuilder.newClient().register(JacksonFeature.class);
    }

    @Override
    public double similarity(CharSequence t1, CharSequence t2) {
        TextPair textPair = new TextPair(t1.toString(), t2.toString());

        RelatednessRequest request = new RelatednessRequest()
                .corpus(params.getCorpus())
                .language(params.getLanguage())
                .scoreFunction(params.getScoreFunction())
                .model(params.getRankingModel().name())
                .pairs(Arrays.asList(textPair));

        WebTarget webTarget = client.target(params.getUrl());
        RelatednessResponse response = webTarget.request()
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), RelatednessResponse.class);

        List<ScoredTextPair> scoredTextPairs = new ArrayList<>(response.getPairs());
        return scoredTextPairs.get(0).score;
    }


    private List<ScoredTextPair> scoreTextPairs(List<TextPair> pairs) {
        RelatednessRequest request = new RelatednessRequest()
                .corpus(params.getCorpus())
                .language(params.getLanguage())
                .scoreFunction(params.getScoreFunction())
                .model(params.getRankingModel().name())
                .pairs(pairs);

        WebTarget webTarget = client.target(params.getUrl());
        RelatednessResponse response = webTarget.request()
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), RelatednessResponse.class);

        return response.getPairs().stream().collect(Collectors.toList());
    }

    private String createHashKey(String term1, String term2) {
        List<String> terms = Arrays.asList(term1, term2);
        Collections.sort(terms, ALPHABETICAL_ORDER);
        return terms.get(0) + "|" + terms.get(1);
    }

    private double bestAvg(List<List<String>> alternatives1, List<List<String>> alternatives2, Map<String, Double> scoreMap) {
        double res = 0;
        for (List<String> terms1 : alternatives1) {
            for (List<String> terms2 : alternatives2) {
                double s = avgPairwise(terms1, terms2, scoreMap);
                if (s > res) {
                    res = s;
                }
            }
        }
        return res;
    }

    private double avgPairwise(List<String> terms1, List<String> terms2, Map<String, Double> scoreMap) {
        double res = 0;
        int n = 0;
        for (String term1 : terms1) {
            for (String term2 : terms2) {
                res += scoreMap.get(createHashKey(term1, term2));
                n += 1;
            }
        }
        return res *1./n;
    }

    @Override
    Scores doScore(Scores inputScores, Rankable target) {
        if (inputScores.size() <= 0) {
            return inputScores;
        }

        // create unique text pairs
        Map<String, TextPair> uniqueTextPairs = new HashMap<>();
        List<String> targetTerms = target.getRankableValues().stream().flatMap(List::stream).distinct().collect(Collectors.toList());
        List<String> checkTerms = inputScores.stream().map(s -> s.getRankableView().getRankableValues()).flatMap(List::stream).flatMap(List::stream).distinct().collect(Collectors.toList());
        for (String targetTerm : targetTerms) {
            for (String checkTerm : checkTerms) {
                uniqueTextPairs.put(createHashKey(checkTerm, targetTerm), new TextPair(checkTerm, targetTerm));
            }
        }

        // score Pairs
        List<ScoredTextPair> scoredPairs = scoreTextPairs(uniqueTextPairs.values().stream().collect(Collectors.toList()));

        // create a lookup table of scores
        Map<String, Double> scoreMap = new HashMap<>();
        scoredPairs.forEach(p -> scoreMap.put(createHashKey(p.t1, p.t2), p.score));


        Scores rescored = new Scores(inputScores.size());

        inputScores.forEach(score -> {
            double s = bestAvg(score.getRankableView().getRankableValues(), target.getRankableValues(), scoreMap);
            rescored.add(new Score(score.getEntry(), s));
        });

        rescored.sort(true);

        return rescored;
    }
}
