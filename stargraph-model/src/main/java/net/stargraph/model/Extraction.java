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
import net.stargraph.rank.Rankable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An OpenIE extraction.
 */
public final class Extraction implements Rankable, Hashable {
    private String id;
    private String relation;
    private List<String> arguments;

    public Extraction(String relation) {
        this(relation, new ArrayList<>());
    }

    public Extraction(String id, String relation, List<String> arguments) {
        this.id = id;
        this.relation = relation;
        this.arguments = arguments;
    }

    public Extraction(String relation, List<String> arguments) {
        this(IDGenerator.generateUUID(), relation, arguments);
    }

    public String getRelation() {
        return relation;
    }

    public void addArgument(String argument) {
        if (!this.arguments.contains(argument)) {
            this.arguments.add(argument);
        }
    }

    public List<String> getArguments() {
        return arguments;
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
        Extraction document = (Extraction) o;
        return id.equals(document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Extraction{" +
                "id='" + id + '\'' +
                ", relation='" + relation + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
