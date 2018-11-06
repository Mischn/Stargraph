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

        sparqlCreator.resetNewVarCounter();
        List<String> statements = new ArrayList<>();

        String subjStmt = null;
        if (subjIdList != null && subjIdList.size() > 0) {

            // add new binding statement
            subjStmt = sparqlCreator.varBindingStmt("?s", subjIdList.stream().map(x -> namespace.expandURI(x)).collect(Collectors.toList()));
        }

        String objStmt = null;
        if (objIdList != null && objIdList.size() > 0) {

            // add new binding statement
            objStmt = sparqlCreator.varBindingStmt("?o", objIdList.stream().map(x -> namespace.expandURI(x)).collect(Collectors.toList()));
        }

        for (String classUri : classURIs.stream().map(u -> namespace.expandURI(u)).collect(Collectors.toList())) {
            List<String> stmts = new ArrayList<>();

            String predRepr = SparqlCreator.formatURI(classUri);
            if (subjStmt != null) {
                stmts.add(subjStmt);
            }
            if (objStmt != null) {
                stmts.add(objStmt);
            }
            stmts.add(sparqlCreator.createStmt("?s", predRepr, "?o"));
            stmts.add(sparqlCreator.createBindStmt("?p", Arrays.asList(classUri)));

            statements.add(sparqlCreator.stmtJoin(stmts, false));
        }

        String query = "SELECT ?s ?p ?o { " + sparqlCreator.stmtUnionJoin(statements, true) + "} " + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());

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



        sparqlCreator.resetNewVarCounter();
        List<String> statements = new ArrayList<>();

        String subjRepr = "?s";
        if (inSubject) {
            subjRepr = SparqlCreator.formatURI(id);
        }

        String objRepr = "?o";
        if (inObject) {
            objRepr = SparqlCreator.formatURI(id);
        }

        if (propertyTypes.equals(PropertyTypes.TYPE_ONLY)) {
            for (String classUri : classURIs.stream().map(u -> namespace.expandURI(u)).collect(Collectors.toList())) {
                List<String> stmts = new ArrayList<>();

                String predRepr = SparqlCreator.formatURI(classUri);
                stmts.add(sparqlCreator.createStmt(subjRepr, predRepr, objRepr));
                stmts.add(sparqlCreator.createBindStmt("?p", Arrays.asList(classUri)));

                statements.add(sparqlCreator.stmtJoin(stmts, false));
            }
        } else if (propertyTypes.equals(PropertyTypes.NON_TYPE_ONLY)) {
            statements.add(sparqlCreator.createStmt(subjRepr, "?p", objRepr));
            statements.add(sparqlCreator.createEqualsFilterStmt("?p", classURIs.stream().map(u -> namespace.expandURI(u)).collect(Collectors.toList()), true));
        } else {
            statements.add(sparqlCreator.createStmt(subjRepr, "?p", objRepr));
        }

        String query = "SELECT ?s ?p ?o { " + sparqlCreator.stmtUnionJoin(statements, true) + "} " + sparqlCreator.createLimit(searchParams.getSearchSpaceLimit());

        return new JenaQueryHolder(new JenaSPARQLQuery(query), searchParams);
    }

    @Override
    public SearchQueryHolder findSimilarDocuments(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
