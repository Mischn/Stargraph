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
import net.stargraph.core.query.SPARQLQueryBuilder;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.model.Document;
import net.stargraph.model.LabeledEntity;
import net.stargraph.query.InteractionMode;
import net.stargraph.rank.Score;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AnswerSetResponse extends QueryResponse {
    private List<LabeledEntity> entityAnswers;
    private List<String> textAnswers;
    private String sparqlQuery;
    private QueryType sparqlQueryType;
    private List<Document> documents;
    private Map<DataModelBinding, List<Score>> mappings;

    public AnswerSetResponse(InteractionMode mode, String userQuery) {
        super(mode, userQuery);
    }

    public AnswerSetResponse(InteractionMode mode, String userQuery, SPARQLQueryBuilder sparqlQueryBuilder) {
        super(mode, userQuery);
        this.sparqlQueryType = Objects.requireNonNull(sparqlQueryBuilder).getQueryType();
    }

    public void setEntityAnswers(List<LabeledEntity> entityAnswers) {
        this.entityAnswers = Objects.requireNonNull(entityAnswers);
    }

    public void setTextAnswers(List<String> textAnswers) {
        this.textAnswers = textAnswers;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public void setMappings(Map<DataModelBinding, List<Score>> mappings) {
        this.mappings = Objects.requireNonNull(mappings);
    }

    public void setSPARQLQuery(String sparqlQuery) {
        this.sparqlQuery = Objects.requireNonNull(sparqlQuery);
    }

    public List<LabeledEntity> getEntityAnswers() {
        return entityAnswers;
    }

    public List<String> getTextAnswers() {
        return textAnswers;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public QueryType getSparqlQueryType() {
        return sparqlQueryType;
    }

    public Map<DataModelBinding, List<Score>> getMappings() {
        return mappings;
    }

    public String getSparqlQuery() {
        return sparqlQuery;
    }

    @Override
    public String toString() {
        return "AnswerSetResponse{" +
                "entityAnswers=" + entityAnswers +
                ", textAnswers=" + textAnswers +
                ", sparqlQuery='" + sparqlQuery + '\'' +
                '}';
    }
}
