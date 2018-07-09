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

import net.stargraph.IDGenerator;
import net.stargraph.data.processor.Hashable;
import net.stargraph.model.date.TimeRange;
import net.stargraph.rank.Rankable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a passage-extraction that can be digested by the StarGraph database.
 */
public class PassageExtraction implements Rankable, Hashable {
    private String id;
    private String relation;
    private List<String> terms; // might be (linked) named entities
    private List<TimeRange> temporals;

    public PassageExtraction(String relation, List<String> terms, List<TimeRange> temporals) {
        this(IDGenerator.generateUUID(), relation, terms, temporals);
    }

    public PassageExtraction(String id, String relation, List<String> terms, List<TimeRange> temporals) {
        this.id = id;
        this.relation = relation;
        this.terms = terms;
        this.temporals = temporals;
    }

    public String getRelation() {
        return relation;
    }

    public List<String> getTerms() {
        return terms;
    }

    public List<TimeRange> getTemporals() {
        return temporals;
    }

    public boolean hasArguments() {
        return terms.size() > 0 || temporals.size() > 0;
    }

    @Override
    public String getValue() {
        return relation;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassageExtraction passageExtraction = (PassageExtraction) o;
        return id.equals(passageExtraction.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PassageExtraction{" +
                "id='" + id + '\'' +
                ", relation='" + relation + '\'' +
                ", terms=" + terms +
                ", temporals=" + temporals +
                '}';
    }
}
