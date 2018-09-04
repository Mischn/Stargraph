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

import net.stargraph.core.annotation.pos.POSAnnotator;
import net.stargraph.core.query.nli.ClueAnalyzer;
import net.stargraph.query.InteractionMode;
import net.stargraph.query.Language;

import java.util.Objects;

import static net.stargraph.query.InteractionMode.NLI;

public final class InteractionModeSelector {
    private POSAnnotator posAnnotator;
    private Language language;

    public static final String ENTITY_SIMILARITY_PATTERN = "^(?:\\s*\\w+)(?:\\s+\\w+){0,6} (?:similar to|similar \\w+ for|(?:is|are|was|were) like) ([^\\s]+).*$";
    public static final String FILTER_PATTERN = "^(?:\\s*\\w+)(?:\\s+\\w+){0,4} (?<obj>objects) (?<expr>\\w+) .*$";
    public static final String DEFINITION_PATTERN =
            "(?:^(?:\\s*\\w+)(?:\\s+\\w+){0,4} definition of ([^\\s]+).*$)"
            + "||"
            + "(?:^\\s*describe ([^\\s]+).*$)";
//            + "||"
//            + "(?:^\\s*(?:who|what) (?:is|are|was|were) ([^\\s]+).*$)";

    public InteractionModeSelector(POSAnnotator posAnnotator, Language language) {
        this.posAnnotator = Objects.requireNonNull(posAnnotator);
        this.language = Objects.requireNonNull(language);
    }

    public InteractionMode detect(String queryString) {
        InteractionMode mode = NLI;

//        // TODO
//        if (queryString.trim().startsWith("#")) {
//            return InteractionMode.LIKE_THIS;
//        }

        if (queryString.contains("SELECT") || queryString.contains("ASK") || queryString.contains("CONSTRUCT")) {
            if (queryString.contains("PREFIX ") || queryString.contains("http:")) {
                mode = InteractionMode.SPARQL;
            } else {
                mode = InteractionMode.SPARQL;
                //mode = InteractionMode.SA_SPARQL; //TODO re-activate?
            }
        } else {
            if (isEntitySimilarityQuery(queryString)) {
                mode = InteractionMode.ENTITY_SIMILARITY;
            } else if (isFilterQuery(queryString)) {
                mode = InteractionMode.FILTER;
            } else if (isDefinitionQuery(queryString)) {
                mode = InteractionMode.DEFINITION;
            }

            // TODO
//            if (queryString.contains("http:")) {
//                mode = InteractionMode.SIMPLE_SPARQL;
//            } else if (queryString.contains(":")) {
//                mode = InteractionMode.SA_SIMPLE_SPARQL;
//            } else if (isClueQuery(queryString)) {
//                mode = InteractionMode.CLUE;
//            }
        }

        //TODO: other types will require configurable rules per language.

        return mode;
    }

    private boolean isEntitySimilarityQuery(String queryString){
        final String q = queryString.trim().toLowerCase();
        return q.matches(ENTITY_SIMILARITY_PATTERN);
    }

    private boolean isFilterQuery(String queryString) {
        final String q = queryString.trim().toLowerCase();
        return q.matches(FILTER_PATTERN);
    }

    private boolean isDefinitionQuery(String queryString){
        final String q = queryString.trim().toLowerCase();
        return q.matches(DEFINITION_PATTERN);
    }

    private boolean isClueQuery(String queryString){

        ClueAnalyzer clueAnalyzer = new ClueAnalyzer(posAnnotator);
        return clueAnalyzer.isClue(queryString);
    }

}
