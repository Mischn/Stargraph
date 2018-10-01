package net.stargraph.rest;

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

import net.stargraph.query.InteractionMode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AnswerSetUserResponse extends UserResponse {

    // Answers
    private List<EntityEntry> entityAnswers;
    private List<DocumentEntry> documentAnswers;
    private List<String> textAnswers;

    // Evidences/Explanation
    private List<String> queryPlans;
    private List<String> sparqlQueries;
    private EntityEntry coreEntity;
    private List<String> docTypes;
    private List<FilterEntry> queryFilters;
    private List<FilterResultEntry> filterResults;

    private Map<String, String> bindings;
    private Map<String, Map<String, Set<EntityEntry>>> possibleMappings;
    private Map<String, Map<String, Set<EntityEntry>>> mappings;

    public AnswerSetUserResponse(String query, InteractionMode interactionMode) {
        super(query, interactionMode);
    }

    public void setEntityAnswers(List<EntityEntry> entityAnswers) {
        this.entityAnswers = entityAnswers;
    }

    public void setDocumentAnswers(List<DocumentEntry> documentAnswers) {
        this.documentAnswers = documentAnswers;
    }

    public void setTextAnswers(List<String> textAnswers) {
        this.textAnswers = textAnswers;
    }

    public void setQueryPlans(List<String> queryPlans) {
        this.queryPlans = queryPlans;
    }

    public void setSparqlQueries(List<String> sparqlQueries) {
        this.sparqlQueries = sparqlQueries;
    }

    public void setCoreEntity(EntityEntry coreEntity) {
        this.coreEntity = coreEntity;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }

    public void setQueryFilters(List<FilterEntry> queryFilters) {
        this.queryFilters = queryFilters;
    }

    public void setFilterResults(List<FilterResultEntry> filterResults) {
        this.filterResults = filterResults;
    }

    public void setBindings(Map<String, String> bindings) {
        this.bindings = bindings;
    }

    public void setPossibleMappings(Map<String, Map<String, Set<EntityEntry>>> possibleMappings) {
        this.possibleMappings = possibleMappings;
    }

    public void setMappings(Map<String, Map<String, Set<EntityEntry>>> mappings) {
        this.mappings = mappings;
    }


    public List<EntityEntry> getEntityAnswers() {
        return entityAnswers;
    }

    public List<DocumentEntry> getDocumentAnswers() {
        return documentAnswers;
    }

    public List<String> getTextAnswers() {
        return textAnswers;
    }

    public List<String> getQueryPlans() {
        return queryPlans;
    }

    public List<String> getSparqlQueries() {
        return sparqlQueries;
    }

    public EntityEntry getCoreEntity() {
        return coreEntity;
    }

    public List<String> getDocTypes() {
        return docTypes;
    }

    public List<FilterEntry> getQueryFilters() {
        return queryFilters;
    }

    public List<FilterResultEntry> getFilterResults() {
        return filterResults;
    }

    public Map<String, String> getBindings() {
        return bindings;
    }

    public Map<String, Map<String, Set<EntityEntry>>> getPossibleMappings() {
        return possibleMappings;
    }

    public Map<String, Map<String, Set<EntityEntry>>> getMappings() {
        return mappings;
    }
}
