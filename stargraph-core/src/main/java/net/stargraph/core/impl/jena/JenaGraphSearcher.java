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

import net.stargraph.StarGraphException;
import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.BaseGraphModel;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.*;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.NotSupportedException;
import java.util.*;

public final class JenaGraphSearcher extends JenaBaseSearcher {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("jena");

    public JenaGraphSearcher(String dbId, Stargraph stargraph, BaseGraphModel model) {
        super(stargraph, dbId, model);
    }

    public JenaGraphSearcher(String dbId, Stargraph stargraph) {
        super(stargraph, dbId);
    }

    @Override
    public Map<String, List<NodeEntity>> select(String sparqlQuery) {
        Map<String, List<NodeEntity>> result = new LinkedHashMap<>();

        sparqlQuery(sparqlQuery, new SparqlIteration() {
            @Override
            public void process(Binding binding) {
                HashMap<String, Node> varMap = getVarMap(binding);
                for (Map.Entry<String, Node> entry : varMap.entrySet()) {
                    String var = entry.getKey();
                    Node node = entry.getValue();

                    //TODO what if property?
                    NodeEntity entity = asEntity(node);
                    result.computeIfAbsent(var, (v) -> new ArrayList<>()).add(entity);
                }
            }
        });

        if (result.isEmpty()) {
            logger.warn(marker, "No matches for {}", sparqlQuery);
        }

        return result;
    }

    @Override
    public boolean ask(String sparqlQuery) {
        return false;
    }

    @Override
    public Scores search(SearchQueryHolder holder) {
        JenaQueryHolder jenaQueryHolder = (JenaQueryHolder)holder;
        String modelId = jenaQueryHolder.getSearchParams().getKbId().getModel();
        String sparqlQuery = jenaQueryHolder.getQuery();

        Scores scores = new Scores();
        sparqlQuery(sparqlQuery, new SparqlIteration() {
            @Override
            public void process(Binding binding) {
                HashMap<String, Node> varMap = getVarMap(binding);

                if (modelId.equals(BuiltInModel.FACT.modelId)) {
                    // assume that '?s', '?p<n>', ?x<n>', '?o' variables are available in the query
                    if (varMap.containsKey("s") && varMap.containsKey("p1") && varMap.containsKey("o")) {
                        Route route = new Route(asEntity(varMap.get("s")));
                        int c = 1;
                        while (varMap.containsKey("p" + c)) {
                            PropertyEntity predicate = asProperty(varMap.get("p" + c));
                            NodeEntity waypoint = (varMap.containsKey("x" + c))? asEntity(varMap.get("x" + c)) : asEntity(varMap.get("o"));
                            route = route.extend(predicate, PropertyPath.Direction.OUTGOING, waypoint);
                            c += 1;
                        }
                        scores.add(new Score(route, 0.0));
                    } else
                    // assume that '?s', '?p', '?o' variables are available in the query
                    if (varMap.containsKey("s") && varMap.containsKey("p") && varMap.containsKey("o")) {
                        InstanceEntity subject = (InstanceEntity) asEntity(varMap.get("s"));
                        PropertyEntity predicate = asProperty(varMap.get("p"));
                        NodeEntity object = asEntity(varMap.get("o"));

                        Fact fact = new Fact(holder.getSearchParams().getKbId(), subject, predicate, object);
                        scores.add(new Score(fact, 0.0));
                    } else {
                        throw new StarGraphException("?s ?p ?o / ?s ?p<n> ?x<n> ?o  variables need to be available in the query");
                    }

                } else if (modelId.equals(BuiltInModel.ENTITY.modelId)) {
                    // assume that '?e' variable is available in the query
                    if (!varMap.containsKey("e")) {
                        throw new StarGraphException("?e variable need to be available in the query");
                    }

                    InstanceEntity entity = (InstanceEntity) asEntity(varMap.get("e"));
                    scores.add(new Score(entity, 0.0));
                } else if (modelId.equals(BuiltInModel.PROPERTY.modelId)) {
                    // assume that '?p' variable is available in the query
                    if (!varMap.containsKey("p")) {
                        throw new StarGraphException("?p variable need to be available in the query");
                    }

                    PropertyEntity predicate = asProperty(varMap.get("p"));
                    scores.add(new Score(predicate, 0.0));
                } else {
                    throw new NotSupportedException("Model '" + modelId + "' is not supported.");
                }
            }
        });

        return scores;
    }

    @Override
    public long countDocuments() {
        return graphModel.getSize();
    }
}
