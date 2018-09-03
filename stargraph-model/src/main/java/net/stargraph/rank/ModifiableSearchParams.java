package net.stargraph.rank;

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

import net.stargraph.model.BuiltInModel;
import net.stargraph.model.KBId;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ModifiableSearchParams {
    public static class Phrase {
        private String text;
        private float boost;

        public Phrase(String term) {
            this(term, 1.0f);
        }

        public Phrase(String text, float boost) {
            this.text = text;
            this.boost = boost;
        }

        public String getText() {
            return text;
        }

        public float getBoost() {
            return boost;
        }

        @Override
        public String toString() {
            return "Phrase{" +
                    "text='" + text + '\'' +
                    ", boost=" + boost +
                    '}';
        }
    }

    private int limit;
    private String dbId;
    private String modelId;
    private List<String> searchTerms;
    private List<Phrase> searchPhrases;
    private boolean lookup;

    private ModifiableSearchParams(String dbId) {
        // defaults
        this.limit = -1;
        this.dbId = dbId;
        this.lookup = true;
    }

    public final ModifiableSearchParams searchTermsFromStr(String searchText) {
        this.searchTerms = Arrays.asList(searchText.split("\\s+"));
        this.searchPhrases = null;
        return this;
    }

    public final ModifiableSearchParams searchPhrases(List<Phrase> searchPhrases) {
        this.searchPhrases = searchPhrases;
        this.searchTerms = null;
        return this;
    }

    public final ModifiableSearchParams searchPhrase(Phrase searchPhrase) {
        return searchPhrases(Arrays.asList(searchPhrase));
    }

    public final ModifiableSearchParams lookup(boolean lookup) {
        this.lookup = lookup;
        return this;
    }

    public final ModifiableSearchParams limit(int maxEntries) {
        this.limit = maxEntries;
        return this;
    }

    public final ModifiableSearchParams model(String id) {
        this.modelId = id;
        return this;
    }

    public final ModifiableSearchParams model(BuiltInModel builtInModel) {
        this.modelId = builtInModel.modelId;
        return this;
    }


    public final int getLimit() {
        return limit;
    }

    public final String getDbId() {
        return dbId;
    }

    public final KBId getKbId() {
        return KBId.of(dbId, modelId);
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public List<Phrase> getSearchPhrases() {
        return searchPhrases;
    }

    public boolean hasSearchTerms() { return searchTerms != null; }

    public boolean hasSearchPhrases() {
        return searchPhrases != null;
    }

    public String getRankableStr() {
        if (hasSearchPhrases()) {
            return searchPhrases.stream().map(p -> p.getText()).collect(Collectors.joining(" "));
        }
        if (hasSearchTerms()) {
            return searchTerms.stream().collect(Collectors.joining(" "));
        }
        return null;
    }

    public boolean isLookup() {
        return lookup;
    }

    public static ModifiableSearchParams create(String dbId) {
        return new ModifiableSearchParams(dbId);
    }
}
