package net.stargraph.model;

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

import net.stargraph.data.processor.Hashable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A Document.
 */
public final class Document implements Hashable {
    private String type;
    private String entity; // the id of an entity, used for entity-similarity search (optional)
    private String id;
    private String title;
    private String summary; // optional
    private String text;
    private List<LabeledEntity> entities;
    private List<Passage> passages;
    private List<PassageExtraction> passageExtractions;
    private List<Extraction> extractions;

    public Document(String id, String title, String summary, String text) {
        this(id, "unknown", title, summary, text, null);
    }

    public Document(String id, String type, String entity, String title, String summary, String text) {
        this(id, type, entity, title, summary, text, null, null, null, null);
    }

    public Document(String id, String type, String entity, String title, String summary, String text, List<LabeledEntity> entities, List<Passage> passages, List<PassageExtraction> passageExtractions, List<Extraction> extractions) {
        this.id = Objects.requireNonNull(id);
        this.type = type;
        this.entity = entity;
        this.title = Objects.requireNonNull(title);
        this.text = Objects.requireNonNull(text);
        this.summary = summary;
        this.entities = (entities != null) ? entities : new ArrayList<>();
        this.passages = (passages != null) ? passages : new ArrayList<>();
        this.passageExtractions = (passageExtractions != null) ? passageExtractions : new ArrayList<>();
        this.extractions = (extractions != null) ? extractions : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getEntity() {
        return entity;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getText() {
        return text;
    }

    public List<LabeledEntity> getEntities() {
        return entities;
    }

    public List<Passage> getPassages() {return passages; }

    public List<PassageExtraction> getPassageExtractions() {
        return passageExtractions;
    }

    public List<Extraction> getExtractions() {
        return extractions;
    }

    @Override
    public String toString() {
        String abbrevSummary = (summary == null) ? "NULL" : (summary.length() > 30)? text.substring(0, 30-3) + "..." : text;
        String abbrevText = (text.length() > 30)? text.substring(0, 30-3) + "..." : text;
        return "Document{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", entity='" + entity + '\'' +
                ", title='" + title + '\'' +
                ", summary='" + abbrevSummary + '\'' +
                ", text='" + abbrevText + '\'' +
                ", entities=" + entities +
                ", passages=" + passages +
                ", passageExtractions=" + passageExtractions +
                ", extractions=" + extractions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return id.equals(document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
