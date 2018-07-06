package net.stargraph.core.search;

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

import net.stargraph.core.KBCore;
import net.stargraph.core.ModelCreator;
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.model.*;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EntitySearcher {
    private interface PruningMethod {
        List<Route> traverse(List<Route> routes);
    }


    private static final int FUZZINESS = 1;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("elastic");

    private Stargraph stargraph;

    public EntitySearcher(Stargraph stargraph) {
        this.stargraph = stargraph;
    }

    /**
     * Returns an instance for the given id.
     * @param dbId
     * @param id
     * @return
     */
    public InstanceEntity getInstanceEntity(String dbId, String id) {
        List<InstanceEntity> res = getInstanceEntities(dbId, Collections.singletonList(id));
        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }
        return null;
    }

    /**
     * Returns instances for the given ids.
     * @param dbId
     * @param ids
     * @return
     */
    public List<InstanceEntity> getInstanceEntities(String dbId, List<String> ids) {
        KBCore core = stargraph.getKBCore(dbId);

        logger.info(marker, "Fetching ids={}", ids);
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.ENTITY);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.entitiesWithIds(ids, searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.stream().map(s -> (InstanceEntity)s.getEntry()).collect(Collectors.toList());
    }

    /**
     * Returns a property for the given id.
     * @param dbId
     * @param id
     * @return
     */
    public PropertyEntity getPropertyEntity(String dbId, String id) {
        List<PropertyEntity> res = getPropertyEntities(dbId, Collections.singletonList(id));
        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }
        return null;
    }

    /**
     * Returns properties for the given ids.
     * @param dbId
     * @param ids
     * @return
     */
    public List<PropertyEntity> getPropertyEntities(String dbId, List<String> ids) {
        KBCore core = stargraph.getKBCore(dbId);

        logger.info(marker, "Fetching ids={}", ids);
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.PROPERTY);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.propertiesWithIds(ids, searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.stream().map(s -> (PropertyEntity)s.getEntry()).collect(Collectors.toList());
    }

    /**
     * Returns a document for the given id.
     * @param dbId
     * @param id
     * @return
     */
    public Document getDocument(String dbId, String id) {
        List<Document> res = getDocuments(dbId, Collections.singletonList(id));
        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }
        return null;
    }

    /**
     * Returns documents for the given ids.
     * @param dbId
     * @param ids
     * @return
     */
    public List<Document> getDocuments(String dbId, List<String> ids) {
        KBCore core = stargraph.getKBCore(dbId);

        logger.info(marker, "Fetching ids={}", ids);
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.DOCUMENT);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.documentsWithIds(ids, searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.stream().map(s -> (Document)s.getEntry()).collect(Collectors.toList());
    }

    /**
     * Returns documents for the given entity-id.
     * @param dbId
     * @return
     */
    public List<Document> getDocumentsForResourceEntity(String dbId, String id, List<String> docTypes) {
        return getDocumentsForResourceEntities(dbId, Arrays.asList(id), docTypes);
    }

    /**
     * Returns documents for the given entity-ids.
     * @param dbId
     * @param ids
     * @return
     */
    public List<Document> getDocumentsForResourceEntities(String dbId, List<String> ids, List<String> docTypes) {
        KBCore core = stargraph.getKBCore(dbId);

        logger.info(marker, "Fetching documents for entity-ids={} with document-types={}", ids, docTypes);
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.DOCUMENT);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.documentsForEntityIds(ids, docTypes, searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.stream().map(s -> (Document)s.getEntry()).collect(Collectors.toList());
    }

    /**
     * Get classes for the given member.
     * @param memberEntity
     * @param searchParams
     * @return
     */
    public List<InstanceEntity> getClassesForMember(InstanceEntity memberEntity, ModifiableSearchParams searchParams) {
        return getClassesForMember(memberEntity.getId(), searchParams);
    }

    /**
     * Get classes for the given member.
     * @param memberId
     * @param searchParams
     * @return
     */
    public List<InstanceEntity> getClassesForMember(String memberId, ModifiableSearchParams searchParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.FACT);
        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(Arrays.asList(memberId), true, searchParams);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // We have to remap the facts to the classes.
        return scores.stream()
                .map(s -> ((Fact)s.getEntry()).getObject())
                .filter(x -> x instanceof InstanceEntity)
                .map(x -> (InstanceEntity)x)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get members of a given class.
     * @param classEntity
     * @param searchParams
     * @return
     */
    public List<InstanceEntity> getClassMembers(InstanceEntity classEntity, ModifiableSearchParams searchParams) {
        return getClassMembers(classEntity.getId(), searchParams);
    }

    /**
     * Get members of a given class.
     * @param classId
     * @param searchParams
     * @return
     */
    public List<InstanceEntity> getClassMembers(String classId, ModifiableSearchParams searchParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.FACT);
        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(Arrays.asList(classId), false, searchParams);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // We have to remap the facts to the members.
        return scores.stream()
                .map(s -> ((Fact)s.getEntry()).getSubject())
                .filter(x -> x instanceof InstanceEntity)
                .map(x -> (InstanceEntity)x)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Search instances by their value & otherValues (fuzzy match with searchTerm).
     * @param searchParams
     * @param rankParams
     * @return
     */
    public Scores instanceSearch(ModifiableSearchParams searchParams, ModifiableRankParams rankParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.ENTITY);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findInstanceInstances(searchParams, true, FUZZINESS);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        //TODO activate?
//        // Re-Rank
//        if (rankParams instanceof ModifiableIndraParams) {
//            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
//        }
//        return Rankers.apply(scores, rankParams, searchParams.getSearchTerm());

        return scores;
    }

    /**
     * Search classes by their value & otherValues (fuzzy match with searchTerm).
     * @param searchParams
     * @param rankParams
     * @return
     */
    public Scores classSearch(ModifiableSearchParams searchParams, ModifiableRankParams rankParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.ENTITY);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findClassInstances(searchParams, true, FUZZINESS);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        //TODO activate?
//        // Re-Rank
//        if (rankParams instanceof ModifiableIndraParams) {
//            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
//        }
//        return Rankers.apply(scores, rankParams, searchParams.getSearchTerm());

        return scores;
    }


    /**
     * Search properties by their hyponyms, hypernyms or synonyms (fuzzy match with searchTerm).
     * @param searchParams
     * @param rankParams
     * @return
     */
    public Scores propertySearch(ModifiableSearchParams searchParams, ModifiableRankParams rankParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.PROPERTY);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findPropertyInstances(searchParams, false, FUZZINESS);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // Re-Rank
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }
        return Rankers.apply(scores, rankParams, searchParams.getSearchTerm());
    }

    public Scores documentSearch(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.DOCUMENT);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findDocumentInstances(searchParams, docTypes, entityDocument, true, FUZZINESS);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores;
    }

    /**
     * Search property paths from the pivot to other labeled entities that are connected with the pivot up to a certain range.
     * If {@code returnBestMatchEntities}, these labeled entities are returned.
     * @param pivotId
     * @param searchParams
     * @param rankParams
     * @param range
     * @param returnBestMatchEntities
     * @return
     */
    public Scores pivotedSearch(String pivotId, ModifiableSearchParams searchParams, ModifiableRankParams rankParams, boolean incomingEdges, boolean outgoingEdges, int range, boolean returnBestMatchEntities) {
        return pivotedSearch(ModelCreator.createInstance(pivotId, null), searchParams, rankParams, incomingEdges, outgoingEdges, range, returnBestMatchEntities);
    }
    public Scores pivotedSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, ModifiableRankParams rankParams, boolean incomingEdges, boolean outgoingEdges, int range, boolean returnBestMatchEntities) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        List<Route> neighbours = neighbourSearch(pivot, searchParams, range, incomingEdges, outgoingEdges, new PruningMethod() {
            @Override
            public List<Route> traverse(List<Route> routes) {
                //TODO strategies for pruning
                return routes;
            }
        });

        // We have to remap the routes to the propertyPath, the real target of the ranker call.
        Scores propScores = new Scores(neighbours.stream()
                .map(n -> n.getPropertyPath())
                .distinct()
                .map(p -> new Score(p, 0))
                .collect(Collectors.toList()));

        // Re-Rank
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }
        Scores rankedScores = Rankers.apply(propScores, rankParams, searchParams.getSearchTerm());
        Scores result = rankedScores;

        if (returnBestMatchEntities) {
            if (rankedScores.size() <= 0) {
                return new Scores();
            }
            Score bestScore = rankedScores.get(0);
            PropertyPath bestPropertyPath = (PropertyPath) bestScore.getEntry();
            logger.debug("Best match is {}, returning instances ..", bestPropertyPath);

            result = new Scores();
            for (Route neighbour : neighbours) {
                if (neighbour.getPropertyPath().equals(bestPropertyPath)) {
                    result.add(new Score(neighbour.getLastWaypoint(), bestScore.getValue()));
                }
            }
        }

        return result;
    }


    public Scores pivotedClassSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, ModifiableRankParams rankParams, int range, boolean returnClassMembers) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        List<Route> neighbours = neighbourSearch(pivot, searchParams, range, true, true, new PruningMethod() {
            @Override
            public List<Route> traverse(List<Route> routes) {
                //TODO strategies for pruning
                return routes;
            }
        });

        // filter for resource entities
        neighbours = neighbours.stream().filter(n -> n.getLastWaypoint() instanceof InstanceEntity).collect(Collectors.toList());

        List<String> neighbourIds = neighbours.stream()
                .map(n -> n.getLastWaypoint().getId())
                .distinct()
                .collect(Collectors.toList());

        searchParams.model(BuiltInModel.FACT);
        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(neighbourIds, true, searchParams);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // maps classes to class-members
        Map<InstanceEntity, List<InstanceEntity>> classMembers = new HashMap<>();
        scores.stream().forEach(s -> {
            Fact fact = (Fact)s.getEntry();
            if (fact.getSubject() instanceof InstanceEntity && fact.getObject() instanceof InstanceEntity) { ;
                InstanceEntity member = (InstanceEntity) fact.getSubject();
                InstanceEntity clazz = (InstanceEntity) fact.getObject();

                List<InstanceEntity> members = classMembers.getOrDefault(clazz, new ArrayList<>());
                if (!members.contains(member)) {
                    members.add(member);
                }
                classMembers.put(clazz, members);
            }
        });
        logger.info(marker, "Fetched {} classes", classMembers.size());

        // We have to remap to classes
        Scores classScores = new Scores(classMembers.keySet().stream().map(c -> new Score(c, 0.0)).collect(Collectors.toList()));

        // Re-Rank
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }
        Scores rankedScores = Rankers.apply(classScores, rankParams, searchParams.getSearchTerm());
        Scores result = rankedScores;

        if (returnClassMembers) {
            if (rankedScores.size() <= 0) {
                return new Scores();
            }
            Score bestScore = rankedScores.get(0);
            InstanceEntity bestClass = (InstanceEntity) bestScore.getEntry();
            logger.debug("Best match is {}, returning instances ..", bestClass);
            result = new Scores(classMembers.get(bestClass).stream().map(m -> new Score(m, bestScore.getValue())).collect(Collectors.toList()));
        }

        return result;
    }

    public Scores similarDocumentSearch(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.DOCUMENT);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findSimilarDocuments(searchParams, docTypes, entityDocument);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores;
    }

    public Scores likeThisInstanceSearch(ModifiableSearchParams searchParams, List<String> docTypes) {
        Scores scores = similarDocumentSearch(searchParams, docTypes, true);

        // now map documents back to their entities
        Scores entityScores = new Scores();
        for (Score score : scores) {
            Document doc = (Document)score.getEntry();

            InstanceEntity ent = getInstanceEntity(searchParams.getDbId(), doc.getEntity());
            if (ent != null) {
                entityScores.add(new Score(ent, score.getValue()));
            }
        }

        return entityScores;
    }

    public Scores similarInstanceSearch(String dbId, InstanceEntity entity, List<String> docTypes, Integer limit) {

        // Search for entity-documents
        List<Document> entityDocs = getDocumentsForResourceEntity(dbId, entity.getId(), docTypes);
        if (entityDocs.size() == 0) {
            logger.warn(marker, "Did not find any documents for entity {}", entity.getId());
            return new Scores();
        }

        List<String> texts = entityDocs.stream().map(d -> d.getText()).collect(Collectors.toList());
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.DOCUMENT).terms(texts);
        if (limit != null) {
            searchParams.limit(limit);
        }

        Scores scores = similarDocumentSearch(searchParams, null, true);

        // now map documents back to their entities
        Scores entitiyScores = new Scores();
        for (Score score : scores) {
            Document doc = (Document)score.getEntry();

            // exclude documents from entityDocs
            if (!entityDocs.contains(doc)) {
                InstanceEntity ent = getInstanceEntity(dbId, doc.getEntity());
                if (ent != null) {
                    entitiyScores.add(new Score(ent, score.getValue()));
                }
            }
        }

        return entitiyScores;
    }










    // HELPER FUNCTIONS

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    // direct neighbours only
    private List<Route> directNeighbourSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean incomingEdges, boolean outgoingEdges) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.FACT);
        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
        SearchQueryHolder holder = searchQueryGenerator.findPivotFacts(pivot, searchParams, outgoingEdges, incomingEdges);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        List result = new ArrayList();
        for (Score score : scores) {
            Fact fact = (Fact)score.getEntry();
            if (outgoingEdges && fact.getSubject().equals(pivot)) {
                result.add(new Route(pivot).extend(fact.getPredicate(), fact.getObject()));
            } else if (incomingEdges && fact.getObject().equals(pivot) && fact.getSubject() instanceof LabeledEntity) {
                result.add(new Route(pivot).extend(fact.getPredicate(), (LabeledEntity) fact.getSubject()));
            }
        }

        return result;
    }

    private List<Route> neighbourSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, int range, boolean incomingEdges, boolean outgoingEdges, PruningMethod pruningMethod) {
        Namespace namespace = stargraph.getKBCore(searchParams.getDbId()).getNamespace();
        InstanceEntity myPivot = namespace.shrink(pivot);

        if (range == 0) {
            return Arrays.asList(new Route(myPivot));
        }

        Map<InstanceEntity, List<Route>> directNeighbours = new HashMap<>(); // for avoiding redundant calculations
        Map<Integer, List<Route>> neighbours = new HashMap<>();
        neighbourSearchRec(
                new Route(myPivot),
                searchParams,
                range,
                range,
                incomingEdges,
                outgoingEdges,
                pruningMethod,
                directNeighbours,
                neighbours
        );

        return neighbours.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private void neighbourSearchRec(Route route, ModifiableSearchParams searchParams, int range, int leftRange, boolean incomingEdges, boolean outgoingEdges, PruningMethod pruningMethod, Map<InstanceEntity, List<Route>> directNeighbours, Map<Integer, List<Route>> routes) {
        if (leftRange == 0 || !(route.getLastWaypoint() instanceof InstanceEntity)) {
            return;
        }

        InstanceEntity currPivot = (InstanceEntity) route.getLastWaypoint();
        List<Route> directNs;
        if (directNeighbours.containsKey(currPivot)) {
            directNs = directNeighbours.get(currPivot);
        } else {
            directNs = directNeighbourSearch(currPivot, searchParams, incomingEdges, outgoingEdges);
            directNeighbours.put(currPivot, directNs);
        }

        // create new routes
        List<Route> newRoutes = new ArrayList<>();
        for (Route directN : directNs) {
            PropertyEntity newProperty = directN.getPropertyPath().getLastProperty();
            LabeledEntity newWaypoint = directN.getLastWaypoint();
            Route newRoute = route.extend(newProperty, newWaypoint);

            // don't allow links back to previous waypoint
            if ((route.getWaypoints().size() >= 2) && newWaypoint.equals(route.getWaypoints().get(route.getWaypoints().size() - 2))) {
                continue;
            }

            // add to result
            newRoutes.add(newRoute);
            routes.computeIfAbsent(range-(leftRange-1), (x) -> new ArrayList<>()).add(newRoute);
        }

        // recursion
        pruningMethod.traverse(newRoutes).forEach(r -> {
            neighbourSearchRec(r, searchParams, range, leftRange-1, incomingEdges, outgoingEdges, pruningMethod, directNeighbours, routes);
        });
    }

}
