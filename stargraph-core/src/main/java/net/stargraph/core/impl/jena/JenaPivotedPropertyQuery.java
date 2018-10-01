package net.stargraph.core.impl.jena;

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

import net.stargraph.core.search.SearchQueryGenerator;

import java.util.List;

public class JenaPivotedPropertyQuery implements JenaQuery {
    private String subjectId;
    private SearchQueryGenerator.PropertyTypes propertyTypes;
    private List<String> classProperties;

    public JenaPivotedPropertyQuery(String subjectId, SearchQueryGenerator.PropertyTypes propertyTypes, List<String> classProperties) {
        this.subjectId = subjectId;
        this.propertyTypes = propertyTypes;
        this.classProperties = classProperties;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public SearchQueryGenerator.PropertyTypes getPropertyTypes() {
        return propertyTypes;
    }

    public List<String> getClassProperties() {
        return classProperties;
    }
}
