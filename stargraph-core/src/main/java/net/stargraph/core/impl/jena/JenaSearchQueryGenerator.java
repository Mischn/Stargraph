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
import net.stargraph.model.InstanceEntity;
import net.stargraph.query.Language;
import net.stargraph.rank.ModifiableSearchParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
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
    public SearchQueryHolder documentsWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder documentsForEntityIds(List<String> idList, List<String> docTypes, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findClassFacts(List<String> idList, boolean inSubject, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();

        List<String> expandedIdList = idList.stream().map(namespace::expandURI).collect(Collectors.toList());

        String query = "SELECT ?s ?p ?o {"
                + cartesianTripleUnionPattern("?s", "?p", "?o", (inSubject)? expandedIdList: null, classURIs, (!inSubject)? expandedIdList : null, true)
                + "}"
                + createLimit(searchParams);

        return new JenaQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
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
    public SearchQueryHolder findPivotFacts(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject) {
        Namespace namespace = getNamespace();

        String id = namespace.expandURI(pivot.getId());

        String query;
        if (inSubject && inObject) {
            query = "SELECT ?s ?p ?o {"
                    + "{"
                    + cartesianTripleUnionPattern("?s", "?p", "?o", Arrays.asList(id), null, null, true)
                    + "} UNION {"
                    + cartesianTripleUnionPattern("?s", "?p", "?o", null, null, Arrays.asList(id), true)
                    + "}"
                    + "}"
                    + createLimit(searchParams);
        } else if (inSubject) {
            query = "SELECT ?s ?p ?o {"
                    + cartesianTripleUnionPattern("?s", "?p", "?o", Arrays.asList(id), null, null, true)
                    + "}"
                    + createLimit(searchParams);
        } else if (inObject) {
            query = "SELECT ?s ?p ?o {"
                    + cartesianTripleUnionPattern("?s", "?p", "?o", null, null, Arrays.asList(id), true)
                    + "}"
                    + createLimit(searchParams);
        } else {
            query = "SELECT ?s ?p ?o {"
                    + "?s ?p ?o ."
                    + "}"
                    + createLimit(searchParams);
        }

        return new JenaQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findSimilarDocuments(List<String> docTypes, boolean entityDocument, List<String> texts, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static String cartesianTripleUnionPattern(String sVarName, String pVarName, String oVarName, List<String> sURIs, List<String> pURIs, List<String> oURIs, boolean addBindings) {
        List<String> sLst = (sURIs == null || sURIs.isEmpty())? Arrays.asList(sVarName) : sURIs.stream().map(s -> "<" + s + ">").collect(Collectors.toList());
        List<String> pLst = (pURIs == null || pURIs.isEmpty())? Arrays.asList(pVarName) : pURIs.stream().map(s -> "<" + s + ">").collect(Collectors.toList());
        List<String> oLst = (oURIs == null || oURIs.isEmpty())? Arrays.asList(oVarName) : oURIs.stream().map(s -> "<" + s + ">").collect(Collectors.toList());

        StringJoiner stmtJoiner = new StringJoiner("} UNION {", "{", "}");
        cartesianProduct(cartesianProduct(sLst, pLst), oLst).stream().forEach(x -> {
            String[] elems = x.split("\\s+");

            // triple
            String str = String.format("%s %s %s .", elems[0], elems[1], elems[2]);

            // bind
            if (addBindings) {
                if (!elems[0].startsWith("?")) {
                    str += String.format(" BIND(%s AS %s) .", elems[0], sVarName);
                }
                if (!elems[1].startsWith("?")) {
                    str += String.format(" BIND(%s AS %s) .", elems[1], pVarName);
                }
                if (!elems[2].startsWith("?")) {
                    str += String.format(" BIND(%s AS %s) .", elems[2], oVarName);
                }
            }

            stmtJoiner.add(str);
        });
        return stmtJoiner.toString();
    }

    private static List<String> cartesianProduct(List<String> x, List<String> y) {
        List<String> xy = new ArrayList<>();
        x.forEach(s1 -> y.forEach(s2 -> xy.add(s1.trim() + " " + s2.trim())));
        return xy;
    }

    // this was used previously but it should be avoided due to performance (use UNIONs instead)
    public static String createFilter(String variable, List<String> URIs) {
        return "FILTER( " + URIs.stream().map(i ->  variable + " = <" + i + ">").collect(Collectors.joining(" || ")) + " )";
    }

    public static String createLangFilter(String variable, List<Language> languages, boolean includeNotSpecified) {
        List<String> langTags = new ArrayList<>();
        languages.forEach(l -> langTags.add(l.code.toLowerCase()));
        if (includeNotSpecified) {
            langTags.add("");
        }

        return "FILTER( " + langTags.stream().map(lang -> "lang(" + variable + ") = \"" + lang + "\"").collect(Collectors.joining(" || ")) + " )";
    }

    private static String createLimit(ModifiableSearchParams searchParams) {
        return (searchParams.getLimit() >= 0)? "LIMIT " + searchParams.getLimit(): "";
    }
}
