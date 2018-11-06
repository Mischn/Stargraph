package net.stargraph.core.query;

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
import net.stargraph.core.Namespace;
import net.stargraph.core.SparqlCreator;
import net.stargraph.core.Stargraph;
import net.stargraph.core.query.nli.*;
import net.stargraph.model.Entity;
import net.stargraph.rank.Score;

import java.util.*;
import java.util.stream.Collectors;

public final class SPARQLQueryBuilder {
    private final SparqlCreator sparqlCreator;
    private final List<String> classURIs;
    private final QueryType queryType;
    private final QueryPlan queryPlan;
    private final Map<String, DataModelBinding> bindings; // maps placeholder to a DataModelBinding
    private Map<String, Map<DataModelBindingContext, Set<Score>>> mappings; // maps placeholder & context to a list of scored entities
    private Namespace namespace;

    public SPARQLQueryBuilder(Stargraph stargraph, String dbId, QueryType queryType, QueryPlan queryPlan, Map<String, DataModelBinding> bindings, Map<String, Map<DataModelBindingContext, Set<Score>>> mappings) {
        this.sparqlCreator = new SparqlCreator();
        this.classURIs = stargraph.getClassRelations(dbId);
        this.queryType = Objects.requireNonNull(queryType);
        this.queryPlan = Objects.requireNonNull(queryPlan);
        this.bindings = Objects.requireNonNull(bindings);
        this.mappings = Objects.requireNonNull(mappings);
        this.namespace = stargraph.getKBCore(dbId).getNamespace();
    }

    // BINDINGS

    public DataModelBinding getBinding(String placeHolder) {
        if (!bindings.containsKey(placeHolder)) {
            throw new StarGraphException("Unbounded '" + placeHolder + "'");
        }
        return bindings.get(placeHolder);
    }

    public Map<String, DataModelBinding> getBindings() {
        return bindings;
    }

    // MAPPINGS

    public Set<Score> getMapping(DataModelBinding binding, DataModelBindingContext context) {
        if (mappings.containsKey(binding.getPlaceHolder()) && mappings.get(binding.getPlaceHolder()).containsKey(context)) {
            return mappings.get(binding.getPlaceHolder()).get(context);
        }
        return Collections.emptySet();
    }

    public Map<String, Map<DataModelBindingContext, Set<Score>>> getMappings() {
        return mappings;
    }

    // OTHER

    public QueryType getQueryType() {
        return queryType;
    }







    public String build() {
        final int LIMIT = 500;
        String limitStr = sparqlCreator.createLimit(LIMIT);

        switch (queryType) {
            case SELECT:
                return String.format("SELECT * WHERE {\n%s\n} " + limitStr, buildStatements());
            case ASK:
                return String.format("ASK {\n%s\n}", buildStatements());
            case AGGREGATE:
                throw new StarGraphException("TBD");
        }

        throw new StarGraphException("Unexpected: " + queryType);
    }

    private String buildStatements() {
        final int varRange = 1; //TODO experiment with this value
        final int typeRange = 1; //TODO experiment with this value

        List<String> statements = new ArrayList<>();
        sparqlCreator.resetNewVarCounter();

        queryPlan.forEach(triplePattern -> {
            TriplePattern.BoundTriple triple = triplePattern.toBoundTriple(bindings);

            // SUBJECT
            String subjRepr;
            DataModelBinding subj = triple.getS();
            if (subj.getModelType().equals(DataModelType.VARIABLE)) {
                subjRepr = subj.getPlaceHolder();
            } else {
                Set<Score> mappings = getMapping(subj, DataModelBindingContext.NON_PREDICATE);
                if (mappings.isEmpty()) {
                    subjRepr = getUnmappedURI(subj);
                } else {
                    // introduce a new variable for the subject
                    subjRepr = sparqlCreator.getNewVar("S");

                    // add new binding statement
                    statements.add(sparqlCreator.varBindingStmt(subjRepr, mappings.stream().map(x -> namespace.expandURI(x.getRankableView().getId())).collect(Collectors.toList())));
                }
            }

            // OBJECT
            String objRepr;
            DataModelBinding obj = triple.getO();
            if (obj.getModelType().equals(DataModelType.VARIABLE)) {
                objRepr = obj.getPlaceHolder();
            } else {
                Set<Score> mappings = getMapping(obj, DataModelBindingContext.NON_PREDICATE);
                if (mappings.isEmpty()) {
                    objRepr = getUnmappedURI(obj);
                } else {
                    // introduce a new variable for the object
                    objRepr = sparqlCreator.getNewVar("O");

                    // add new binding statement
                    statements.add(sparqlCreator.varBindingStmt(objRepr, mappings.stream().map(x -> namespace.expandURI(x.getRankableView().getId())).collect(Collectors.toList())));
                }
            }

            // PREDICATE
            String stmt = null;
            DataModelBinding pred = triple.getP();
            if (pred.getModelType().equals(DataModelType.VARIABLE)) {
                List<String> stmts = new ArrayList<>();
                for (String predRepr : sparqlCreator.createPreds("V", "T", varRange)) {
                    stmts.add(sparqlCreator.createStmt(subjRepr, predRepr, objRepr));
                }
                stmt = sparqlCreator.stmtUnionJoin(stmts, false);
            } else if (pred.getModelType().equals(DataModelType.TYPE)) {
                String predRepr = sparqlCreator.createPred(classURIs.stream().map(u -> namespace.expandURI(u)).collect(Collectors.toList()), typeRange);
                stmt = sparqlCreator.createStmt(subjRepr, predRepr, objRepr);
            } else if (pred.getModelType().equals(DataModelType.EQUALS)) {
                // TODO
                throw new UnsupportedOperationException("Not supported yet!");
            } else {
                Set<Score> mappings = getMapping(pred, DataModelBindingContext.PREDICATE);
                String predRepr;
                if (mappings.isEmpty()) {
                    predRepr = getUnmappedURI(pred);
                } else {
                    predRepr = sparqlCreator.createPred(mappings.stream().map(s -> (Entity) s.getEntry()).map(e -> namespace.expand(e)).collect(Collectors.toList()));
                }
                stmt = sparqlCreator.createStmt(subjRepr, predRepr, objRepr);
            }

            statements.add(stmt);
        });

        return sparqlCreator.stmtJoin(statements, true);
    }

    private String getUnmappedURI(DataModelBinding binding) {
        return String.format(":%s", binding.getTerm().replaceAll("\\s", "_"));
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("Triple-Patterns:");
        for (TriplePattern triplePattern : queryPlan) {
            strb.append("\n\t").append(triplePattern);
        }
        strb.append("\n\n").append("Bindings & Mappings:");
        for (String placeholder : mappings.keySet()) {
            if (bindings.containsKey(placeholder)) {
                strb.append("\n\t").append(bindings.get(placeholder));
            } else {
                strb.append("\n\t").append(placeholder);
            }
            if (mappings.containsKey(placeholder) && mappings.get(placeholder).size() > 0) {
                strb.append("\n\t\t").append("Mappings: ").append(mappings.get(placeholder));
            } else {
                strb.append("\n\t\t").append("NO MAPPINGS");
            }
        }
        return strb.toString();
    }
}