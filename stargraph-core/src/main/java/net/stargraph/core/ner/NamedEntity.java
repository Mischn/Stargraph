package net.stargraph.core.ner;

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

import net.stargraph.model.LabeledEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NamedEntity {

    private String value;
    private String cat;
    private int start;
    private int end;
    private List<LinkedEntityScore> entities;

    public NamedEntity(String value, String cat, int start, int end) {
        this(value, cat, start, end, new ArrayList<>(), -1);
    }

    public NamedEntity(String value, String cat, int start, int end, List<LinkedEntityScore> entities, double score) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("NamedEntity `value` can't be null or empty");
        }
        this.value = value;
        this.cat = cat;
        this.start = start;
        this.end = end;
        this.entities = entities;
    }

    public String getValue() {
        return value;
    }

    public String getCat() {
        return cat;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isLinked() {
        return entities.size() > 0;
    }

    public List<LinkedEntityScore> getEntities() {
        return entities;
    }


    public void reverseValue() {
        if (cat.equalsIgnoreCase("I-PER")) { // TODO: make 'I-PER' constant, align with all classifiers
            List<String> nameTokens = Arrays.asList(value.split("\\s+"));
            if (nameTokens.size() > 1) {
                value = nameTokens.get(nameTokens.size() - 1)
                        + ", " + String.join(" ", nameTokens.subList(0, nameTokens.size() - 1));
            }
        }
    }

    public void merge(String value, int newEndPos) {
        this.value = value;
        this.end = newEndPos;
    }

    public void addLink(LabeledEntity entity, String entityDbId, double score) {
        this.entities.add(new LinkedEntityScore(entity, entityDbId, score));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedEntity that = (NamedEntity) o;
        return start == that.start &&
                end == that.end &&
                Objects.equals(entities, that.entities) &&
                Objects.equals(cat, that.cat) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entities, start, end, cat, value);
    }

    @Override
    public String toString() {
        if (!isLinked()) {
            return "Unlinked{" +
                    "value='" + value + '\'' +
                    ", cat='" + cat + '\'' +
                    '}';
        }
        return "Linked{" +
                "entities='" + entities + '\'' +
                ", value='" + value + '\'' +
                ", cat='" + cat + '\'' +
                '}';
    }
}
