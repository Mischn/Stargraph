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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

        List<TextPair> pairs = inputScores.stream()
                .map(score -> new TextPair(score.getRankableView().getRankableValue(), target.getRankableValue()))
                .collect(Collectors.toList());

        RelatednessRequest request = new RelatednessRequest()
                .corpus(params.getCorpus())
                .language(params.getLanguage())
                .scoreFunction(params.getScoreFunction())
                .model(params.getRankingModel().name())
                .pairs(pairs);

        WebTarget webTarget = client.target(params.getUrl());
        RelatednessResponse response = webTarget.request()
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), RelatednessResponse.class);

        Scores rescored = new Scores(inputScores.size());
        List<Score> found = new ArrayList<>();
        response.getPairs().forEach(p -> {rescored.add(find(inputScores, found, p.t1, p.score));});
        rescored.sort(true);

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
