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

import net.stargraph.rank.Ranker;
import net.stargraph.rank.impl.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public final class RankersTest {

    @Test
    public void testRankers() {
        List<Ranker> rankers = Arrays.asList(new LevenshteinRanker(), new FuzzyRanker(), new JarowinklerRanker(), new JaccardRanker(), new LCSRanker());
        for (Ranker ranker : rankers) {
            System.out.println("# " + ranker.getClass().getSimpleName() + ":");

            System.out.println("Paris <-> Paris : " + ranker.similarity("Paris", "Paris"));
            Assert.assertEquals(ranker.similarity("Paris", "Paris"), 1.);

            System.out.println("Paris <-> Baris : " + ranker.similarity("Paris", "Baris"));
            Assert.assertTrue(ranker.similarity("Paris", "Baris") < ranker.similarity("Paris", "Paris"));

            System.out.println("Paris <-> Peter : " + ranker.similarity("Paris", "Peter"));
            Assert.assertTrue(ranker.similarity("Paris", "Peter") < ranker.similarity("Paris", "Paris"));

            System.out.println("Paris <-> x : " + ranker.similarity("Paris", "x"));
            Assert.assertTrue(ranker.similarity("Paris", "x") < ranker.similarity("Paris", "Peter"));

            System.out.println("Paris <-> sssParisss : " + ranker.similarity("Paris", "sssParisss"));
        }
    }
}
