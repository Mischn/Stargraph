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
import net.stargraph.core.Stargraph;
import net.stargraph.core.impl.jena.JenaSearchQueryGenerator;
import net.stargraph.core.processors.FactClassifierProcessor;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.query.nli.QueryPlanPatterns;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.PropertyPath;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SPARQLQueryBuilder {
    private List<String> classURIs;
    private QueryType queryType;
    private QueryPlanPatterns triplePatterns;
    private List<DataModelBinding> bindings;
    private Map<DataModelBinding, List<Score>> mappings;
    private Namespace namespace;
    private int tmpVarCounter;

    public SPARQLQueryBuilder(Stargraph stargraph, String dbId, QueryType queryType, QueryPlanPatterns triplePatterns, List<DataModelBinding> bindings) {
        this.classURIs = stargraph.getClassRelations(dbId);
        this.queryType = Objects.requireNonNull(queryType);
        this.triplePatterns = Objects.requireNonNull(triplePatterns);
        this.bindings = Objects.requireNonNull(bindings);
        this.mappings = new ConcurrentHashMap<>();
    }

    @Override
    public String toString() {
        return build();
    }

    public QueryPlanPatterns getTriplePatterns() {
        return triplePatterns;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public DataModelBinding getBinding(String placeHolder) {
        return bindings.stream()
                .filter(b -> b.getPlaceHolder().equals(placeHolder))
                .findFirst()
                .orElseThrow(() -> new StarGraphException("Unbounded '" + placeHolder + "'"));
    }

    List<DataModelBinding> getBindings() {
        return bindings;
    }

    void setNS(Namespace ns) {
        this.namespace = Objects.requireNonNull(ns);
    }

    boolean isResolved(DataModelBinding binding) {
        return mappings.containsKey(binding);
    }

    List<Score> getMappings(DataModelBinding binding) {
        if (mappings.containsKey(binding)) {
            return mappings.get(binding);
        }
        return Collections.emptyList();
    }

    Map<DataModelBinding, List<Score>> getMappings() {
        return mappings;
    }

    void addMapping(DataModelBinding binding, List<Score> scores) {
        final Scores newScores = new Scores(scores.size());
        // Expanding the Namespace for all entities
        scores.forEach(s -> newScores.add(new Score(namespace.expand(s.getEntry()), s.getValue())));
        mappings.computeIfAbsent(binding, (b) -> new Scores()).addAll(newScores);
    }

    String build() {
        switch (queryType) {
            case SELECT:
                return String.format("SELECT * WHERE {\n%s\n}", buildStatements());
            case ASK:
                return String.format("ASK {\n%s\n}", buildStatements());
            case AGGREGATE:
                throw new StarGraphException("TBD");
        }

        throw new StarGraphException("Unexpected: " + queryType);
    }

    private String buildStatements() {
        resetTmpVarCounter();
        StringJoiner tripleJoiner = new StringJoiner("\n");

        triplePatterns.forEach(triplePattern -> {
            String[] components = triplePattern.getPattern().split("\\s");

            List<Pattern> sURIs = placeHolder2Pattern(components[0], false);
            List<Pattern> pURIs = placeHolder2Pattern(components[1], true);
            List<Pattern> oURIs = placeHolder2Pattern(components[2], false);

            List<Pattern> prod = cartesianProduct(cartesianProduct(sURIs, pURIs), oURIs);

            StringJoiner stmtJoiner = new StringJoiner("} UNION \n{", "{", "}");
            prod.forEach(p -> {
                String filterPattern = p.getFilterPatterns().stream().collect(Collectors.joining(" "));
                stmtJoiner.add(p.getPattern().trim() + " . " + filterPattern);
            });

            tripleJoiner.add(stmtJoiner.toString());
        });

        return tripleJoiner.toString();
    }

    private List<Pattern> cartesianProduct(List<Pattern> x, List<Pattern> y) {
        List<Pattern> xy = new ArrayList<>();
        x.forEach(s1 -> y.forEach(s2 -> {
            List<String> joinedFilter = new ArrayList<>();
            joinedFilter.addAll(s1.getFilterPatterns());
            joinedFilter.addAll(s2.getFilterPatterns());
            xy.add(new Pattern(s1.getPattern().trim() + " " + s2.getPattern().trim(), joinedFilter));
        }));
        return xy;
    }

/*
    private List<String> cartesianProduct(List<String> x, List<String> y) {
        List<String> xy = new ArrayList<>();
        x.forEach(s1 -> y.forEach(s2 -> xy.add(s1.trim() + " " + s2.trim())));
        return xy;
    }
*/

    private List<Pattern> placeHolder2Pattern(String placeHolder, boolean predicate) {
        final int varRange = 1; //TODO experiment with this value
        final int typeRange = 1; //TODO experiment with this value

        // Variable
        if (isVar(placeHolder)) {
            if (predicate) {
                // create someting like ['?VAR_1', '?TMP_1 ?TMP_2 . ?TMP_2 ?TMP_3'] for varRange=2
                List<Pattern> patterns = new ArrayList<>();
                for (int i = 1; i < varRange+1; i++) {
                    List<String> ps = new ArrayList<>();
                    if (i == 1) {
                        ps.add(placeHolder);
                    } else {
                        for (int j = 0; j < i; j++) {
                            ps.add(getNewTmpVar());
                        }
                    }
                    patterns.add(new Pattern(joinedPathPredicate(ps)));
                }
                return patterns;
            } else {
                return Collections.singletonList(new Pattern(placeHolder));
            }
        }

        // Type
        if (isType(placeHolder)) {
            if (predicate) {
                // create someting like ['?TMP_1', '?TMP_2 ?TMP_3 . ?TMP_3 ?TMP_4'] for typeRange=2
                // with filters for ?TMP_1, ?TMP_2, ?TMP_3 specifying type-of-class-relations
                List<Pattern> patterns = new ArrayList<>();
                for (int i = 1; i < typeRange+1; i++) {
                    List<String> ps = new ArrayList<>();
                    List<String> filterPatterns = new ArrayList<>();
                    for (int j = 0; j < i; j++) {
                        String classVar = getNewTmpVar();
                        ps.add(classVar);
                        filterPatterns.add(JenaSearchQueryGenerator.createFilter(classVar, classURIs));
                    }

                    patterns.add(new Pattern(joinedPathPredicate(ps), filterPatterns));
                }
                return patterns;
            } else {
                return Collections.singletonList(new Pattern("a"));
            }
        }

        // Bindings
        DataModelBinding binding = getBinding(placeHolder);
        List<Score> mappings = getMappings(binding);
        if (mappings.isEmpty()) {
            return Collections.singletonList(new Pattern(getURI(binding)));
        }
        List<Pattern> patterns = new ArrayList<>();
        for (Score mapping : mappings) {

            // create someting like '<..> ?TMP_1 . ?TMP_1 <..>' for path with 2 properties
            if (mapping.getEntry() instanceof PropertyPath) {
                PropertyPath path = (PropertyPath)mapping.getEntry();
                List<String> ps = new ArrayList<>();
                for (PropertyEntity property : path.getProperties()) {
                    if (FactClassifierProcessor.isClassRelation(property)) {
                        ps.add("a");
                    } else {
                        ps.add(String.format("<%s>", unmap(mapping.getRankableView().getId())));
                    }
                }
                patterns.add(new Pattern(joinedPathPredicate(ps)));
            } else {
                patterns.add(new Pattern(String.format("<%s>", unmap(mapping.getRankableView().getId()))));
            }
        }

        return patterns;
    }

    private void resetTmpVarCounter() {
        tmpVarCounter = 0;
    }

    private String getNewTmpVar() {
        tmpVarCounter += 1;
        return "?TMP_" + tmpVarCounter;
    }

    // creates 'x ?TMP_1 . TMP_1 y TMP_2 . TMP_2 z' for ['x', 'y', 'z']
    private String joinedPathPredicate(List<String> predicateStrs) {
        if (predicateStrs.size() == 1) {
            return predicateStrs.get(0);
        } else {
            StringBuilder strb = new StringBuilder();
            strb.append(predicateStrs.get(0));
            for (int i = 1; i < predicateStrs.size(); i++) {
                String tmpVar = getNewTmpVar();
                strb.append(String.format(" %s . %s %s", tmpVar, tmpVar, predicateStrs.get(i)));
            }
            return strb.toString();
        }
    }


    private boolean isVar(String s) {
        return s.startsWith("?VAR");
    }

    private boolean isType(String s) {
        return s.startsWith("TYPE");
    }

    private String getURI(DataModelBinding binding) {
        return String.format(":%s", binding.getTerm().replaceAll("\\s", "_"));
    }

    private String unmap(String uri) {
        return namespace != null ? namespace.expandURI(uri) : uri;
    }

    private class Pattern {
        private String pattern;
        private List<String> filterPatterns;

        public Pattern(String pattern) {
            this(pattern, new ArrayList<>());
        }

        public Pattern(String pattern, List<String> filterPatterns) {
            this.pattern = pattern;
            this.filterPatterns = filterPatterns;
        }

        public String getPattern() {
            return pattern;
        }

        public List<String> getFilterPatterns() {
            return filterPatterns;
        }
    }
}