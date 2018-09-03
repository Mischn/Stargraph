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
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.query.nli.QueryPlanPatterns;
import net.stargraph.model.PropertyPath;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SPARQLQueryBuilder {
    private SparqlCreator sparqlCreator;
    private List<String> classURIs;
    private QueryType queryType;
    private QueryPlanPatterns triplePatterns;
    private List<DataModelBinding> bindings;
    private Map<DataModelBinding, List<Score>> mappings;
    private Namespace namespace;

    public SPARQLQueryBuilder(Stargraph stargraph, String dbId, QueryType queryType, QueryPlanPatterns triplePatterns, List<DataModelBinding> bindings) {
        this.sparqlCreator = new SparqlCreator();
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

    public List<DataModelBinding> getBindings() {
        return bindings;
    }

    public void setNS(Namespace ns) {
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

        sparqlCreator.resetNewVarCounter();


        StringJoiner tripleJoiner = new StringJoiner(" UNION\n", "{", "}");

        triplePatterns.forEach(triplePattern -> {
            String[] components = triplePattern.getPattern().split("\\s");

            List<String> sMappings = new ArrayList<>();
            List<String> oMappings = new ArrayList<>();

            // Subject
            String subjectPlaceholder = components[0];
            if (isVar(subjectPlaceholder)) {
                sMappings = Arrays.asList(subjectPlaceholder);
            } else if (isType(subjectPlaceholder)) {
                throw new AssertionError("Subject should not be a type");
            } else {
                DataModelBinding binding = getBinding(subjectPlaceholder);
                List<Score> mappings = getMappings(binding);
                if (mappings.isEmpty()) {
                    sMappings = Arrays.asList(getURI(binding));
                } else {
                    for (Score mapping : mappings) {
                        sMappings.add(String.format("<%s>", unmap(mapping.getRankableView().getId())));
                    }
                }
            }

            // Object
            String objectPlaceholder = components[2];
            if (isVar(objectPlaceholder)) {
                oMappings = Arrays.asList(objectPlaceholder);
            } else if (isType(objectPlaceholder)) {
                throw new AssertionError("Object should not be a type");
            } else {
                DataModelBinding binding = getBinding(objectPlaceholder);
                List<Score> mappings = getMappings(binding);
                if (mappings.isEmpty()) {
                    oMappings = Arrays.asList(getURI(binding));
                } else {
                    for (Score mapping : mappings) {
                        oMappings.add(String.format("<%s>", unmap(mapping.getRankableView().getId())));
                    }
                }
            }

            // Property
            String propertyPlaceholder = components[1];

            if (isVar(propertyPlaceholder)) {
                List<String> strs = new ArrayList<>();
                for (int i = 0; i < varRange; i++) {
                    sparqlCreator.resetNewVarCounter();

                    SparqlCreator.PathPattern pathPattern = sparqlCreator.createPathPattern("?s", i+1, "?o", "?p", "?v");

                    Map<String, List<String>> varMappings = new HashMap<>();
                    varMappings.put("?s", sMappings);
                    varMappings.put("?o", oMappings);

                    strs.add(sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pathPattern.getPattern(), varMappings), false));
                }
                tripleJoiner.add(sparqlCreator.unionJoin(strs, true));
            } else if (isType(propertyPlaceholder)) {
                List<String> strs = new ArrayList<>();
                for (int i = 0; i < typeRange; i++) {
                    sparqlCreator.resetNewVarCounter();

                    SparqlCreator.PathPattern pathPattern = sparqlCreator.createPathPattern("?s", i+1, "?o", "?p", "?t");

                    Map<String, List<String>> varMappings = new HashMap<>();
                    varMappings.put("?s", sMappings);
                    varMappings.put("?o", oMappings);
                    for (String v : pathPattern.getPropertyVars()) {
                        varMappings.put(v, classURIs.stream().map(u -> String.format("<%s>", u)).collect(Collectors.toList()));
                    }

                    strs.add(sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pathPattern.getPattern(), varMappings), false));
                }
                tripleJoiner.add(sparqlCreator.unionJoin(strs, true));
            } else {
                DataModelBinding binding = getBinding(propertyPlaceholder);
                List<Score> mappings = getMappings(binding);
                if (mappings.isEmpty()) {
                    sparqlCreator.resetNewVarCounter();

                    String pattern = "?s ?p ?o .";

                    Map<String, List<String>> varMappings = new HashMap<>();
                    varMappings.put("?s", sMappings);
                    varMappings.put("?o", oMappings);
                    varMappings.put("?p", Arrays.asList(getURI(binding)));

                    tripleJoiner.add(sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varMappings), false));
                } else {
                    List<String> strs = new ArrayList<>();
                    for (Score mapping : mappings) {
                        sparqlCreator.resetNewVarCounter();

                        if (mapping.getEntry() instanceof PropertyPath) {
                            PropertyPath propertyPath = (PropertyPath) mapping.getEntry();

                            SparqlCreator.PathPattern pathPattern = sparqlCreator.createPathPattern("?s", propertyPath.getProperties().size(), "?o", "?p", "?pp");

                            Map<String, List<String>> varMappings = new HashMap<>();
                            varMappings.put("?s", sMappings);
                            varMappings.put("?o", oMappings);
                            for (int i = 0; i < pathPattern.getPropertyVars().size(); i++) {
                                String v = pathPattern.getPropertyVars().get(i);
                                varMappings.put(v, Arrays.asList(String.format("<%s>", unmap(propertyPath.getProperties().get(i).getId()))));
                            }

                            strs.add(sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pathPattern.getPattern(), varMappings), false));
                        } else {
                            String pattern = "?s ?p ?o .";

                            Map<String, List<String>> varMappings = new HashMap<>();
                            varMappings.put("?s", sMappings);
                            varMappings.put("?o", oMappings);
                            varMappings.put("?p", Arrays.asList(String.format("<%s>", unmap(mapping.getRankableView().getId()))));

                            strs.add(sparqlCreator.unionJoin(sparqlCreator.resolvePatternToStr(pattern, varMappings), false));
                        }
                    }
                    tripleJoiner.add(sparqlCreator.unionJoin(strs, true));
                }
            }
        });

        return tripleJoiner.toString();
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

}