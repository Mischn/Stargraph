package net.stargraph.core.query.nli;

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

import net.stargraph.StarGraphException;
import net.stargraph.core.Stargraph;
import net.stargraph.core.annotation.binding.BindAnnotator;
import net.stargraph.core.annotation.binding.Binding;
import net.stargraph.core.annotation.binding.BindingPattern;
import net.stargraph.core.annotation.pos.Word;
import net.stargraph.core.query.QueryType;
import net.stargraph.core.query.SPARQLQueryBuilder;
import net.stargraph.query.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QuestionAnalysis {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("nli");

    private static final Pattern PUNCT_PATTERN = Pattern.compile(".*(([(){},.;!?<>%])).*");

    private Stargraph stargraph;
    private String dbId;
    private Language language;
    private QueryType queryType;
    private String question;

    private BindAnnotator<DataModelType> bindAnnotator;
    private List<Word> annotatedWords;
    private List<Binding<DataModelType>> currBindings;
    private SPARQLQueryBuilder sparqlQueryBuilder;

    QuestionAnalysis(Stargraph stargraph, String dbId, String question, QueryType queryType) {
        this.stargraph = stargraph;
        this.dbId = dbId;
        this.language = stargraph.getKBCore(dbId).getLanguage();
        this.queryType = Objects.requireNonNull(queryType);
        this.question = Objects.requireNonNull(question);
        bindAnnotator = new BindAnnotator<>(null, null, null);
        logger.debug(marker, "Analyzing '{}', detected type is '{}'", question, queryType);
    }

    void setAnnotatedWords(List<Word> annotatedWords) {
        this.annotatedWords = Objects.requireNonNull(annotatedWords);
    }

    void resolveDataModelBindings(List<BindingPattern<DataModelType>> bindingPatterns) {
        logger.info(marker, "RESOLVE DATA MODEL BINDINGS");
        this.bindAnnotator.setBindingPatterns(bindingPatterns);
        this.currBindings = bindAnnotator.extractBindings(annotatedWords);
    }

    void clean() {
        logger.info(marker, "CLEAN DATA MODEL BINDINGS");
        List<Pattern> stopPatterns = Arrays.asList(PUNCT_PATTERN);

        List<BindingPattern<DataModelType>> stopBindingPatterns = stopPatterns.stream()
                .map(p -> new BindingPattern<DataModelType>(p.pattern(), DataModelType.STOP, language))
                .collect(Collectors.toList());
        this.bindAnnotator.setBindingPatterns(stopBindingPatterns);
        this.currBindings = bindAnnotator.extractBindings2(this.currBindings);

        // delete all stop patterns
        this.currBindings = currBindings.stream().filter(b -> !b.isBound() || !b.getObject().equals(DataModelType.STOP)).collect(Collectors.toList());
    }

    void resolveSPARQL(List<QueryPlanPatterns> rules) {
        String planId = currBindings.stream().map(b -> (b.isBound())? b.getPlaceHolder() : b.getBoundText()).collect(Collectors.joining(" "));

        logger.debug(marker, "Check for planID: '" + planId + "'");

        List<DataModelBinding> dataModelBindings = currBindings.stream()
                .filter(b -> b.isBound())
                .map(b -> new DataModelBinding(b.getObject(), b.getPlaceHolder(), b.getBoundText()))
                .collect(Collectors.toList());

        QueryPlanPatterns plan = rules.stream()
                .filter(p -> p.match(planId))
                .findFirst().orElseThrow(() -> new StarGraphException("No plan for '" + planId + "'"));

        logger.debug(marker, "Creating SPARQL Query Builder, matched plan for '{}' is '{}'", planId, plan);

        sparqlQueryBuilder = new SPARQLQueryBuilder(stargraph, dbId, queryType, plan, dataModelBindings);
    }

    public SPARQLQueryBuilder getSPARQLQueryBuilder() {
        if (sparqlQueryBuilder == null) {
            throw new IllegalStateException();
        }
        return sparqlQueryBuilder;
    }

    @Override
    public String toString() {
        return "Analysis{" +
                "q='" + question + '\'' +
                ", queryType'" + queryType + '\'' +
                ", POS=" + annotatedWords +
                ", Bindings='" + currBindings + '\'' +
                '}';
    }
}
