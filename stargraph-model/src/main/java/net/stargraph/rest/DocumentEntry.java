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

import java.util.List;

public class DocumentEntry {
    private String dbId;
    private String type;
    private String entity;
    private String id;
    private String title;
    private String summary;
    private String text;
    private List<EntityEntry> entities;
    private double score;

    public DocumentEntry(String dbId, String type, String entity, String id, String title, String summary, String text, List<EntityEntry> entities) { this(dbId, type, entity, id, title, summary, text, entities, 1); }
    public DocumentEntry(String dbId, String type, String entity, String id, String title, String summary, String text, List<EntityEntry> entities, double score) {
        this.dbId = dbId;
        this.type = type;
        this.entity = entity;
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.text = text;
        this.entities = entities;
        this.score = score;
    }

    public String getDbId() {
        return dbId;
    }

    public String getType() {
        return type;
    }

    public String getEntity() {
        return entity;
    }

    public String getId() {
        return id;
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

    public List<EntityEntry> getEntities() {
        return entities;
    }

    public double getScore() {
        return score;
    }
}
