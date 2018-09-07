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


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModifiableSearchString {
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

    private List<String> searchTerms;
    private List<Phrase> searchPhrases;

    private ModifiableSearchString() {
    }

    public final ModifiableSearchString searchTermsFromStr(String searchText) {
        this.searchTerms = Arrays.asList(searchText.split("\\s+"));
        this.searchPhrases = null;
        return this;
    }

    public final ModifiableSearchString searchPhrases(List<Phrase> searchPhrases) {
        this.searchPhrases = searchPhrases;
        this.searchTerms = null;
        return this;
    }

    public final ModifiableSearchString searchPhrase(Phrase searchPhrase) {
        return searchPhrases(Arrays.asList(searchPhrase));
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

    public static ModifiableSearchString create() {
        return new ModifiableSearchString();
    }
}
