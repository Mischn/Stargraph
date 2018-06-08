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

import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.search.BaseSearchQueryGenerator;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.ResourceEntity;
import net.stargraph.rank.ModifiableSearchParams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JenaSearchQueryGenerator extends BaseSearchQueryGenerator {
    private List<String> classURIs;

    public JenaSearchQueryGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
        this.classURIs = stargraph.getClassRelations();
    }

    @Override
    public SearchQueryHolder entitiesWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder propertiesWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findClassFacts(List<String> idList, boolean inSubject, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();

        List<String> expandedIdList = idList.stream().map(namespace::expandURI).collect(Collectors.toList());
        String query = "SELECT DISTINCT ?s ?p ?o {"
                + "?s ?p ?o ."
                + createFilter("?p", classURIs)
                + createFilter((inSubject)? "?s":"?o", expandedIdList)
                + "}"
                + createLimit(searchParams);

        return new JenaQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findResourceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPivotFacts(ResourceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject) {
        Namespace namespace = getNamespace();

        String id = namespace.expandURI(pivot.getId());

        String query;
        if (inSubject && inObject) {
            query = "SELECT DISTINCT ?s ?p ?o {"
                    + "{"
                    + "?s ?p ?o ."
                    + createFilter("?s", Arrays.asList(id))
                    + "} UNION {"
                    + "?s ?p ?o ."
                    + createFilter("?o", Arrays.asList(id))
                    + "}"
                    + "}"
                    + createLimit(searchParams);
        } else if (inSubject) {
            query = "SELECT DISTINCT ?s ?p ?o {"
                    + "?s ?p ?o ."
                    + createFilter("?s", Arrays.asList(id))
                    + "}"
                    + createLimit(searchParams);
        } else if (inObject) {
            query = "SELECT DISTINCT ?s ?p ?o {"
                    + "?s ?p ?o ."
                    + createFilter("?o", Arrays.asList(id))
                    + "}"
                    + createLimit(searchParams);
        } else {
            query = "SELECT DISTINCT ?s ?p ?o {"
                    + "?s ?p ?o ."
                    + "}"
                    + createLimit(searchParams);
        }

        return new JenaQueryHolder(query, searchParams);
    }



    public static String createFilter(String variable, List<String> idList) {
        return "FILTER( " + idList.stream().map(i ->  variable + " = <" + i + ">").collect(Collectors.joining(" || ")) + " )";
    }

    private static String createLimit(ModifiableSearchParams searchParams) {
        return (searchParams.getLimit() >= 0)? "LIMIT " + searchParams.getLimit(): "";
    }
}
