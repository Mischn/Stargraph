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

import net.stargraph.rank.*;

public abstract class StringDistanceRanker extends BaseRanker {

    @Override
    public double similarity(CharSequence t1, CharSequence t2) {
        return computeNormStringDistance(t1, t2);
    }



    @Override
    final Scores doScore(Scores inputScores, Rankable target) {
        Scores rescored = new Scores(inputScores.size());

        inputScores.forEach(score -> {
            double dist = computeNormStringDistance(score.getRankableView().getValue(), target.getValue());
            rescored.add(new Score(score.getEntry(), dist));
        });

        rescored.sort(true);

        return rescored;
    }

    private double computeNormStringDistance(CharSequence s1, CharSequence s2) {
        return 1.0 / (computeStringDistance(s1, s2) + 1);
    }

    abstract double computeStringDistance(CharSequence s1, CharSequence s2);
}
