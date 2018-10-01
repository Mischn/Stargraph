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
import net.stargraph.core.SparqlCreator;
import net.stargraph.core.Stargraph;
import net.stargraph.core.search.BaseSearchQueryGenerator;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.ModifiableSearchParams;
import net.stargraph.rank.ModifiableSearchString;

import java.util.*;
import java.util.stream.Collectors;

public class JenaSearchQueryGenerator extends BaseSearchQueryGenerator {
    private List<String> classURIs;
    private SparqlCreator sparqlCreator;

    public JenaSearchQueryGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
        Namespace namespace = getNamespace();
        this.classURIs = stargraph.getClassRelations(dbId).stream().map(namespace::expandURI).collect(Collectors.toList());
        this.sparqlCreator = new SparqlCreator();
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
    public SearchQueryHolder findClassFacts(List<String> subjIdList, List<String> objIdList, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();
        
        String pattern = "?s ?p ?o .";

        Map<String, List<String>> varBindings = new HashMap<>();
        if (subjIdList != null && subjIdList.size() > 0) {
            List<String> expandedSubjIdList = subjIdList.stream().map(namespace::expandURI).collect(Collectors.toList());
            varBindings.put("?s", expandedSubjIdList.stream().map(s -> "<" + s + ">").collect(Collectors.toList()));
        }
        if (objIdList != null && objIdList.size() > 0) {
            List<String> expandedObjIdList = objIdList.stream().map(namespace::expandURI).collect(Collectors.toList());
            varBindings.put("?o", expandedObjIdList.stream().map(s -> "<" + s + ">").collect(Collectors.toList()));
        }
        varBindings.put("?p", classURIs.stream().map(s -> "<" + s + ">").collect(Collectors.toList()));

        String query = "SELECT ?s ?p ?o {"
                + sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varBindings, Arrays.asList("?s", "?p", "?o")), false)
                + "}"
                + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());

        return new JenaQueryHolder(new JenaSPARQLQuery(query), searchParams);
    }

    @Override
    public SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findDocumentInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPivotFacts(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject, PropertyTypes propertyTypes) {
        Namespace namespace = getNamespace();

        String id = namespace.expandURI(pivot.getId());

        // just for better performance
        if (inSubject && !inObject) {
            return new JenaQueryHolder(new JenaPivotedPropertyQuery(id, propertyTypes, classURIs), searchParams);
        }


        String pattern = "?s ?p ?o .";

        Map<String, List<String>> varBindingsS = new HashMap<>();
        varBindingsS.put("?s", Arrays.asList("<" + id + ">"));
        Map<String, List<String>> varBindingsO = new HashMap<>();
        varBindingsO.put("?o", Arrays.asList("<" + id + ">"));

        Map<String, List<String>> varBindingsType = new HashMap<>();
        varBindingsType.put("?p", classURIs.stream().map(s -> "<" + s + ">").collect(Collectors.toList()));

        String nonTypePropertyFilter = (propertyTypes.equals(PropertyTypes.NON_TYPE_ONLY))? " . " + sparqlCreator.createEqualsFilterStr(varBindingsType, true) : "";
        if (propertyTypes.equals(PropertyTypes.TYPE_ONLY)) {
            varBindingsS.put("?p", varBindingsType.get("?p"));
            varBindingsO.put("?p", varBindingsType.get("?p"));
        }

        String query;
        if (inSubject && inObject) {
            query = "SELECT ?s ?p ?o {"
                    + "{"
                    + sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varBindingsS, Arrays.asList("?s", "?p", "?o")).stream().map(s -> s + nonTypePropertyFilter).collect(Collectors.toList()), false)
                    + "} UNION {"
                    + sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varBindingsO, Arrays.asList("?s", "?p", "?o")).stream().map(s -> s + nonTypePropertyFilter).collect(Collectors.toList()), false)
                    + "}"
                    + "}"
                    + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());
        } else if (inSubject) {
            query = "SELECT ?s ?p ?o {"
                    + sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varBindingsS, Arrays.asList("?s", "?p", "?o")).stream().map(s -> s + nonTypePropertyFilter).collect(Collectors.toList()), false)
                    + "}"
                    + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());
        } else if (inObject) {
            query = "SELECT ?s ?p ?o {"
                    + sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varBindingsO, Arrays.asList("?s", "?p", "?o")).stream().map(s -> s + nonTypePropertyFilter).collect(Collectors.toList()), false)
                    + "}"
                    + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());
        } else {
            query = "SELECT ?s ?p ?o {"
                    + "?s ?p ?o ."
                    + "}"
                    + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());
        }

        return new JenaQueryHolder(new JenaSPARQLQuery(query), searchParams);
    }

    @Override
    public SearchQueryHolder findSimilarDocuments(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
