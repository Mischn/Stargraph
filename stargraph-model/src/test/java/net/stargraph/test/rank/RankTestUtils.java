package net.stargraph.test.rank;

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

import net.stargraph.rank.Rankable;
import net.stargraph.rank.Score;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class RankTestUtils {

    static Rankable createRankable(String value) {
        return createRankable(Arrays.asList(Arrays.asList(value)));
    }

    static Rankable createRankable(List<List<String>> values) {
        return new Rankable() {
            @Override
            public String toString() {
                return values.toString();
            }

            @Override
            public List<List<String>> getRankableValues() {
                return values;
            }

            @Override
            public String getId() {
                return values.stream().map(v -> v.stream().collect(Collectors.joining("+"))).collect(Collectors.joining("||"));
            }
        };
    }

    static Score createScore(String v, double d) {
        return new Score(createRankable(v), d);
    }

    static Score createScore(List<List<String>> vss, double d) {
        return new Score(createRankable(vss), d);
    }
}
