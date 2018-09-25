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

    @Override
    Scores doScore(Scores inputScores, Rankable target) {
        if (inputScores.size() <= 0) {
            return inputScores;
        }

        Map<String, List<Score>> map = new HashMap<>(); // maps rankable values to the corresponding scores
        List<TextPair> pairs = new ArrayList<>(); // rankable value <-> rankable value of target

        String targetValue = target.getRankableValue();
        inputScores.stream().forEach(score -> {
            String value = score.getRankableView().getRankableValue();
            map.computeIfAbsent(value, (v) -> new ArrayList<>()).add(score);
        });
        for (String v : map.keySet()) {
            pairs.add(new TextPair(v, targetValue));
        }

        RelatednessRequest request = new RelatednessRequest()
                .corpus(params.getCorpus())
                .language(params.getLanguage())
                .scoreFunction(params.getScoreFunction())
                .model(params.getRankingModel().name())
                .pairs(pairs);

        WebTarget webTarget = client.target(params.getUrl());
        RelatednessResponse response = webTarget.request()
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), RelatednessResponse.class);

        List<ScoredTextPair> scoredPairs = response.getPairs().stream().collect(Collectors.toList());
        Comparator<ScoredTextPair> comparator = Comparator.comparingDouble(x -> x.score);
        scoredPairs.sort(comparator.reversed());

        Scores rescored = new Scores();
        for (ScoredTextPair pair : scoredPairs) {
            if (map.containsKey(pair.t1)) {
                rescored.addAll(map.get(pair.t1).stream().map(s -> new Score(s.getEntry(), pair.score)).collect(Collectors.toList()));
            }
        }

        return rescored;
    }

    private Score find(Scores scores, List<Score> found, String text, double v) {
        for (Score score : scores) {
            if (!found.contains(score) && score.getRankableView().getRankableValue().equals(text)) {
                Score res = new Score(score.getRankableView(), v);
                found.add(score);
                return res;
            }
        }
        return null;
    }
}
