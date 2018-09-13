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
import net.stargraph.core.annotation.binding.BindAnnotator;
import net.stargraph.core.annotation.binding.Binding;
import net.stargraph.core.annotation.binding.BindingPattern;
import net.stargraph.core.annotation.pos.Word;
import net.stargraph.core.query.QueryType;
import net.stargraph.query.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QuestionAnalysis {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("nli");

    private static final Pattern PUNCT_PATTERN = Pattern.compile(".*(([(){},.;!?<>%])).*");

    private Language language;
    private QueryType queryType;
    private String question;

    private BindAnnotator<DataModelType> bindAnnotator;
    private List<Word> annotatedWords;
    private List<Binding<DataModelType>> currBindings;
    private QueryPlannerPattern queryPlannerPattern;

    QuestionAnalysis(Language language, String question, QueryType queryType) {
        this.language = language;
        this.queryType = Objects.requireNonNull(queryType);
        this.question = Objects.requireNonNull(question);
        this.bindAnnotator = new BindAnnotator<>(null, null, null);
        logger.debug(marker, "Analyzing '{}', detected type is '{}'", question, queryType);
    }

    public void setAnnotatedWords(List<Word> annotatedWords) {
        this.annotatedWords = Objects.requireNonNull(annotatedWords);
    }

    public void resolveDataModelBindings(List<BindingPattern<DataModelType>> bindingPatterns) {
        logger.info(marker, "RESOLVE DATA MODEL BINDINGS");
        this.bindAnnotator.setBindingPatterns(bindingPatterns);
        this.currBindings = bindAnnotator.extractBindings(annotatedWords);
    }

    public void clean() {
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

    public void determineQueryPlans(List<QueryPlannerPattern> rules) {
        String planId = currBindings.stream().map(b -> (b.isBound())? b.getPlaceHolder() : b.getBoundText()).collect(Collectors.joining(" "));

        logger.debug(marker, "Check for plan with planId: '" + planId + "'");

        this.queryPlannerPattern = rules.stream()
                .filter(p -> p.match(planId))
                .findFirst()
                .orElseThrow(() -> new StarGraphException("No plans for '" + planId + "'"));

        logger.debug(marker, "Matched plans for '{}' are:\n{}", planId, queryPlannerPattern.getQueryPlans().stream().map(p -> p.toString()).collect(Collectors.joining("\n")));
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public String getQuestion() {
        return question;
    }

    public List<DataModelBinding> getBindings() {
        return currBindings.stream()
                .filter(b -> b.isBound()) // return only bound bindings
                .map(b -> new DataModelBinding(b.getObject(), b.getPlaceHolder(), b.getBoundText()))
                .collect(Collectors.toList());
    }

    public DataModelBinding getBinding(String placeHolder) {
        return getBindings().stream()
                .filter(b -> b.getPlaceHolder().equals(placeHolder))
                .findFirst()
                .orElseThrow(() -> new StarGraphException("Unbounded '" + placeHolder + "'"));
    }

    public QueryPlannerPattern getQueryPlannerPattern() {
        return queryPlannerPattern;
    }
}
