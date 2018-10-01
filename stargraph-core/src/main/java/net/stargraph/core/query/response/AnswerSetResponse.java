package net.stargraph.core.query.response;

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

import net.stargraph.core.query.QueryResponse;
import net.stargraph.core.query.QueryType;
import net.stargraph.core.query.filter.FilterResult;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.query.nli.DataModelBindingContext;
import net.stargraph.core.query.nli.QueryPlan;
import net.stargraph.model.PassageExtraction;
import net.stargraph.query.InteractionMode;
import net.stargraph.rank.Score;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AnswerSetResponse extends QueryResponse {

    // Answers
    private List<Score> entityAnswers;
    private List<Score> documentAnswers;
    private List<String> textAnswers;

    // Evidences/Explanation
    private List<QueryPlan> queryPlans;
    private List<String> sparqlQueries; // created SPARQL-Queries
    private Score coreEntity;
    private QueryType sparqlQueryType;
    private List<String> docTypes;
    private List<PassageExtraction> queryFilters;
    private List<FilterResult> filterResults;

    private Map<String, DataModelBinding> bindings;
    private Map<String, Map<DataModelBindingContext, Set<Score>>> possibleMappings;
    private Map<String, Map<DataModelBindingContext, Set<Score>>> mappings;

    public AnswerSetResponse(InteractionMode mode, String userQuery) {
        super(mode, userQuery);
    }

    public AnswerSetResponse(InteractionMode mode, String userQuery, QueryType sparqlQueryType) {
        super(mode, userQuery);
        this.sparqlQueryType = Objects.requireNonNull(sparqlQueryType);
    }

    public void setEntityAnswers(List<Score> entityAnswers) {
        this.entityAnswers = Objects.requireNonNull(entityAnswers);
    }

    public void setDocumentAnswers(List<Score> documentAnswers) {
        this.documentAnswers = documentAnswers;
    }

    public void setTextAnswers(List<String> textAnswers) {
        this.textAnswers = textAnswers;
    }

    public void setQueryPlans(List<QueryPlan> queryPlans) {
        this.queryPlans = queryPlans;
    }

    public void setSparqlQueries(List<String> sparqlQueries) {
        this.sparqlQueries = sparqlQueries;
    }

    public void setCoreEntity(Score coreEntity) {
        this.coreEntity = coreEntity;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }

    public void setQueryFilters(List<PassageExtraction> queryFilters) {
        this.queryFilters = queryFilters;
    }

    public void setFilterResults(List<FilterResult> filterResults) {
        this.filterResults = filterResults;
    }

    public void setBindings(Map<String, DataModelBinding> bindings) {
        this.bindings = bindings;
    }

    public void setPossibleMappings(Map<String, Map<DataModelBindingContext, Set<Score>>> possibleMappings) {
        this.possibleMappings = possibleMappings;
    }

    public void setMappings(Map<String, Map<DataModelBindingContext, Set<Score>>> mappings) {
        this.mappings = mappings;
    }


    public List<Score> getEntityAnswers() {
        return entityAnswers;
    }

    public List<Score> getDocumentAnswers() {
        return documentAnswers;
    }

    public List<String> getTextAnswers() {
        return textAnswers;
    }

    public List<QueryPlan> getQueryPlans() {
        return queryPlans;
    }

    public List<String> getSparqlQueries() {
        return sparqlQueries;
    }

    public Score getCoreEntity() {
        return coreEntity;
    }

    public List<String> getDocTypes() {
        return docTypes;
    }

    public QueryType getSparqlQueryType() {
        return sparqlQueryType;
    }

    public Map<String, DataModelBinding> getBindings() {
        return bindings;
    }

    public Map<String, Map<DataModelBindingContext, Set<Score>>> getPossibleMappings() {
        return possibleMappings;
    }

    public Map<String, Map<DataModelBindingContext, Set<Score>>> getMappings() {
        return mappings;
    }


    public List<PassageExtraction> getQueryFilters() {
        return queryFilters;
    }

    public List<FilterResult> getFilterResults() {
        return filterResults;
    }

    @Override
    public String toString() {
        return "AnswerSetResponse{" +
                "entityAnswers=" + entityAnswers +
                ", documentAnswers=" + documentAnswers +
                ", textAnswers=" + textAnswers +
                ", queryPlans='" + queryPlans + '\'' +
                ", coreEntity=" + coreEntity +
                ", docTypes=" + docTypes +
                '}';
    }
}
