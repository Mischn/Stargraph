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

    // Answers
    private List<EntityEntry> entityAnswers;
    private List<DocumentEntry> documentAnswers;
    private List<String> textAnswers;

    // Evidences/Explanation
    private String sparqlQuery;
    private EntityEntry coreEntity;
    private List<String> docTypes;

    private Map<String, List<EntityEntry>> mappings;

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

    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    public void setCoreEntity(EntityEntry coreEntity) {
        this.coreEntity = coreEntity;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }

    public void setMappings(Map<String, List<EntityEntry>> mappings) {
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

    public EntityEntry getCoreEntity() {
        return coreEntity;
    }

    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public List<String> getDocTypes() {
        return docTypes;
    }

    public Map<String, List<EntityEntry>> getMappings() {
        return mappings;
    }
}
