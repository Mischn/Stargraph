package net.stargraph.core.query;

/*-
 * ==========================License-Start=============================
 * stargraph-core
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

import net.stargraph.model.PassageExtraction;
import net.stargraph.model.date.TimeRange;
import net.stargraph.rank.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilterResult {
    public static class SingleFilterResult {
        private PassageExtraction filter;

        private Score matchedRelation;
        private List<Score> matchedTerms;
        private List<Score> matchedTemporals;

        private static List generateNullList(int size) {
            List res = new ArrayList();
            for (int i = 0; i < size; i++) {
                res.add(null);
            }
            return res;
        }

        public SingleFilterResult(PassageExtraction filter) {
            this.filter = filter;

            this.matchedTerms = generateNullList(filter.getTerms().size());
            this.matchedTemporals = generateNullList(filter.getTemporals().size());
        }

        public void setMatchedRelation(String matchedRelation, double score) {
            this.matchedRelation = new Score(matchedRelation, score);
        }

        public void addMatchedTerm(int idx, String match, double score) {
            this.matchedTerms.set(idx, new Score(match, score));
        }

        public void addMatchedTemporal(int idx, TimeRange match, double score) {
            this.matchedTemporals.set(idx, new Score(match, score));
        }

        public boolean isMatchedRelation() {
            return matchedRelation != null;
        }

        public Score getMatchedRelation() {
            return matchedRelation;
        }

        public PassageExtraction getFilter() {
            return filter;
        }

        // these may contain null values if not matched
        public List<Score> getMatchedTerms() {
            return matchedTerms;
        }

        // these may contain null values if not matched
        public List<Score> getMatchedTemporals() {
            return matchedTemporals;
        }

        @Override
        public String toString() {
            String relStr = (matchedRelation != null)? matchedRelation.getEntry().toString() : "null";
            String argStr = matchedTerms.stream().map(s -> (s != null)? s.getEntry().toString() : "null").collect(Collectors.joining(" "));
            String tmpStr = matchedTemporals.stream().map(s -> (s != null)? s.getEntry().toString() : "null").collect(Collectors.joining(" "));

            return relStr + ": " + argStr + " / " + tmpStr;
        }
    }

    private String docId;
    private String entityId;
    private List<SingleFilterResult> singleFilterResults;

    public FilterResult(List<PassageExtraction> queryFilters, String docId, String entityId) {
        this.singleFilterResults = queryFilters.stream().map(f -> new SingleFilterResult(f)).collect(Collectors.toList());
        this.docId = docId;
        this.entityId = entityId;
    }

    public List<SingleFilterResult> getSingleFilterResults() {
        return singleFilterResults;
    }

    public String getDocId() {
        return docId;
    }

    public String getEntityId() {
        return entityId;
    }
}
