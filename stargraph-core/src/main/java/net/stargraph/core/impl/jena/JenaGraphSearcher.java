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

import net.stargraph.core.ModelCreator;
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.BaseGraphModel;
import net.stargraph.core.graph.GraphSearcher;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.LabeledEntity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.ResourceEntity;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;

public final class JenaGraphSearcher implements GraphSearcher {
    private interface SparqlIteration {
        void process(Binding binding);
    }

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("jena");
    private Namespace namespace;
    private EntitySearcher entitySearcher;
    private BaseGraphModel graphModel;
    private String dbId;

    public JenaGraphSearcher(String dbId, Stargraph stargraph) {
        this.dbId = Objects.requireNonNull(dbId);
        this.entitySearcher = stargraph.getEntitySearcher();
        this.graphModel = stargraph.getKBCore(dbId).getGraphModel();
        this.namespace = stargraph.getKBCore(dbId).getNamespace();
    }

    @Override
    public Map<String, List<LabeledEntity>> select(String sparqlQuery) {
        return doSparqlQuery(sparqlQuery);
    }

    @Override
    public boolean ask(String sparqlQuery) {
        return false;
    }

    private static HashMap<String, Node> getVarMap(Binding binding) {
        HashMap<String, Node> res = new HashMap<>();

        Iterator<Var> varIt = binding.vars();
        while (varIt.hasNext()) {
            Var var = varIt.next();
            res.put(var.getVarName(), binding.get(var));
        }

        return res;
    }

    private LabeledEntity convertEntityNode(Node node, boolean lookup) {
        if (!node.isLiteral()) {
            String id = node.getURI();
            ResourceEntity resourceEntity = null;

            //TODO re-enable?
            //ResourceEntity resourceEntity = ns.isFromMainNS(id) ? entitySearcher.getResourceEntity(dbId, id) : ModelUtils.createResource(id);

            if (lookup) {
                resourceEntity = entitySearcher.getResourceEntity(dbId, id);
            }
            if (resourceEntity == null) {
                resourceEntity = ModelCreator.createResource(id, namespace);
            }
            return resourceEntity;
        } else {
            LiteralLabel lit = node.getLiteral();
            return ModelCreator.createValue(lit.getLexicalForm(), lit.getDatatype().getURI(), lit.language());
        }
    }

    private PropertyEntity convertPropertyNode(Node node, boolean lookup) {
        if (!node.isLiteral()) {
            String id = node.getURI();
            PropertyEntity propertyEntity = null;

            if (lookup) {
                propertyEntity = entitySearcher.getPropertyEntity(dbId, id);
            }
            if (propertyEntity == null) {
                propertyEntity = ModelCreator.createProperty(id, namespace);
            }
            return propertyEntity;
        } else {
            throw new IllegalArgumentException("Node should not be a literal.");
        }
    }


    private void doSparqlQuery(String sparqlQuery, SparqlIteration sparqlIteration) {
        logger.debug(marker, "Executing: {}", sparqlQuery);

        long startTime = System.currentTimeMillis();
        graphModel.doRead(new BaseGraphModel.ReadTransaction() {
            @Override
            public void readTransaction(Model model) {
                try (QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, model)) {
                    ResultSet results = qexec.execSelect();

                    while (results.hasNext()) {
                        sparqlIteration.process(results.nextBinding());
                    }
                }
            }
        });
        long millis = System.currentTimeMillis() - startTime;
        logger.info(marker, "SPARQL query '{}' took {}s", sparqlQuery, millis / 1000.0);
    }


    private Map<String, List<LabeledEntity>> doSparqlQuery(String sparqlQuery) {
        Map<String, List<LabeledEntity>> result = new LinkedHashMap<>();

        doSparqlQuery(sparqlQuery, new SparqlIteration() {
            @Override
            public void process(Binding binding) {
                HashMap<String, Node> varMap = getVarMap(binding);
                for (Map.Entry<String, Node> entry : varMap.entrySet()) {
                    String var = entry.getKey();
                    Node node = entry.getValue();

                    //TODO what if property?
                    LabeledEntity entity = convertEntityNode(node, true);
                    result.computeIfAbsent(var, (v) -> new ArrayList<>()).add(entity);
                }
            }
        });

        if (result.isEmpty()) {
            logger.warn(marker, "No matches for {}", sparqlQuery);
        }

        return result;
    }

}
