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

public final class AnswerSetUserResponse extends UserResponse {
    private List<EntityEntry> entityAnswers;
    private List<String> textAnswers;
    private String sparqlQuery;
    private List<DocumentEntry> documents;
    private Map<String, List<EntityEntry>> mappings;

    public AnswerSetUserResponse(String query, InteractionMode interactionMode) {
        super(query, interactionMode);
    }

    public void setEntityAnswers(List<EntityEntry> entityAnswers) {
        this.entityAnswers = entityAnswers;
    }

    public void setTextAnswers(List<String> textAnswers) {
        this.textAnswers = textAnswers;
    }

    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    public void setDocuments(List<DocumentEntry> documents) {
        this.documents = documents;
    }

    public void setMappings(Map<String, List<EntityEntry>> mappings) {
        this.mappings = mappings;
    }

    public List<EntityEntry> getEntityAnswers() {
        return entityAnswers;
    }

    public List<String> getTextAnswers() {
        return textAnswers;
    }

    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public List<DocumentEntry> getDocuments() {
        return documents;
    }

    public Map<String, List<EntityEntry>> getMappings() {
        return mappings;
    }
}
