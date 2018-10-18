package net.stargraph.core.query.nli;

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

import net.stargraph.StarGraphException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TriplePattern {
    public static class BoundTriple {
        TriplePattern triplePattern;
        private DataModelBinding s;
        private DataModelBinding p;
        private DataModelBinding o;

        public BoundTriple(TriplePattern triplePattern, DataModelBinding s, DataModelBinding p, DataModelBinding o) {
            this.triplePattern = triplePattern;
            this.s = s;
            this.p = p;
            this.o = o;
        }

        public List<DataModelBinding> getBindings() {
            return Arrays.asList(this.s, this.p, this.o);
        }

        public TriplePattern getTriplePattern() {
            return triplePattern;
        }

        public DataModelBinding getS() {
            return s;
        }

        public DataModelBinding getP() {
            return p;
        }

        public DataModelBinding getO() {
            return o;
        }

        @Override
        public String toString() {
            return String.format("<%s %s %s> (%s = '%s', %s = '%s', %s = '%s')", s.getPlaceHolder(), p.getPlaceHolder(), o.getPlaceHolder(),
                    s.getPlaceHolder(), s.getTerm(),
                    p.getPlaceHolder(), p.getTerm(),
                    o.getPlaceHolder(), o.getTerm());
        }
    }

    private String pattern;

    public TriplePattern(String pattern) {
        this.pattern = Objects.requireNonNull(pattern);
    }

    public String getPattern() {
        return pattern;
    }

    public BoundTriple toBoundTriple(Map<String, DataModelBinding> bindings) {
        String[] components = pattern.split("\\s");
        return new BoundTriple(this, map(components[0], bindings), map(components[1], bindings), map(components[2], bindings));
    }

    private DataModelBinding map(String placeHolder, Map<String, DataModelBinding> bindings) {
        if (placeHolder.startsWith("?VAR")) {
            return new DataModelBinding(DataModelType.VARIABLE, placeHolder, placeHolder);
        }
        if (placeHolder.startsWith("TYPE")) {
            return new DataModelBinding(DataModelType.TYPE, placeHolder, placeHolder);
        }
        if (placeHolder.startsWith("EQUALS")) {
            return new DataModelBinding(DataModelType.EQUALS, placeHolder, placeHolder);
        }

        if (bindings.containsKey(placeHolder)) {
            return bindings.get(placeHolder);
        } else {
            throw new StarGraphException("Unmapped placeholder '" + placeHolder + "'");
        }
    }


    @Override
    public String toString() {
        return pattern;
    }


}
