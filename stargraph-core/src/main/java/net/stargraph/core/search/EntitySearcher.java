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
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.model.*;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
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

    // returns ResourceEntity-instance
    public ResourceEntity getResourceEntity(String dbId, String id) {
        List<ResourceEntity> res = getResourceEntities(dbId, Collections.singletonList(id));
        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }
        return null;
    }

    // returns ResourceEntity-instances
    public List<ResourceEntity> getResourceEntities(String dbId, List<String> ids) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.ENTITY);
        KBCore core = stargraph.getKBCore(dbId);

        logger.info(marker, "Fetching ids={}", ids);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.entitiesWithIds(ids, searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.stream().map(s -> (ResourceEntity)s.getEntry()).collect(Collectors.toList());
    }



    // returns PropertyEntity-instance
    public PropertyEntity getPropertyEntity(String dbId, String id) {
        List<PropertyEntity> res = getPropertyEntities(dbId, Collections.singletonList(id));
        if (res != null && !res.isEmpty()) {
            return res.get(0);
        }
        return null;
    }

    // returns PropertyEntity-instances
    public List<PropertyEntity> getPropertyEntities(String dbId, List<String> ids) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).model(BuiltInModel.PROPERTY);
        KBCore core = stargraph.getKBCore(dbId);

        logger.info(marker, "Fetching ids={}", ids);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.propertiesWithIds(ids, searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.stream().map(s -> (PropertyEntity)s.getEntry()).collect(Collectors.toList());
    }






    // returns ResourceEntity-instances
    public Scores resourceSearch(ModifiableSearchParams searchParams, ModifiableRankParams rankParams) {
        searchParams.model(BuiltInModel.ENTITY);
        KBCore core = stargraph.getKBCore(searchParams.getKbId().getId());

        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findResourceInstances(searchParams, FUZZINESS);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);
        // Re-Rank
        return Rankers.apply(scores, rankParams, searchParams.getSearchTerm());
    }

    // returns ResourceEntity-instances
    public Scores classSearch(ModifiableSearchParams searchParams, ModifiableRankParams rankParams) {
        searchParams.model(BuiltInModel.FACT);
        KBCore core = stargraph.getKBCore(searchParams.getKbId().getId());

        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }

        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        List<Score> classes2Score = scores.stream()
                .map(s -> new Score(((Fact)s.getEntry()).getObject(), s.getValue())).collect(Collectors.toList());
        // Re-Rank
        return Rankers.apply(new Scores(classes2Score), rankParams, searchParams.getSearchTerm());
    }

    // returns PropertyEntity-instances
    public Scores propertySearch(ModifiableSearchParams searchParams, ModifiableRankParams rankParams) {
        searchParams.model(BuiltInModel.PROPERTY);
        KBCore core = stargraph.getKBCore(searchParams.getKbId().getId());

        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }

        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findPropertyInstances(searchParams);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);
        // Re-Rank
        return Rankers.apply(scores, rankParams, searchParams.getSearchTerm());
    }

    // returns LabeledEntity-instances (if returnBestMatchEntities), else: PropertyPath-instances
    public Scores pivotedSearch(ResourceEntity pivot,
                                ModifiableSearchParams searchParams, ModifiableRankParams rankParams, int range, boolean returnBestMatchEntities) {

        searchParams.model(BuiltInModel.FACT);
        KBCore core = stargraph.getKBCore(searchParams.getKbId().getId());
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }

        List<Route> neighbours = neighbourSearch(pivot, searchParams, range, true, true, new PruningMethod() {
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


    // direct neighbours only
    private List<Route> directNeighbourSearch(ResourceEntity pivot, ModifiableSearchParams searchParams, boolean incomingEdges, boolean outgoingEdges) {
        searchParams.model(BuiltInModel.FACT);
        KBCore core = stargraph.getKBCore(searchParams.getKbId().getId());

        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findPivotFacts(pivot, searchParams, outgoingEdges, incomingEdges);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

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

    private List<Route> neighbourSearch(ResourceEntity pivot, ModifiableSearchParams searchParams, int range, boolean incomingEdges, boolean outgoingEdges, PruningMethod pruningMethod) {
        if (range < 1) {
            throw new IllegalArgumentException("Range has to be >= 1");
        }

        Namespace namespace = stargraph.getKBCore(searchParams.getKbId().getId()).getNamespace();
        ResourceEntity myPivot = namespace.shrink(pivot);

        Map<ResourceEntity, List<Route>> directNeighbours = new HashMap<>(); // for avoiding redundant calculations
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

        for (Map.Entry<Integer, List<Route>> integerListEntry : neighbours.entrySet()) {
            System.out.println(integerListEntry.getKey());
            integerListEntry.getValue().forEach(v -> System.out.println("\t" + v));
        }

        return neighbours.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private void neighbourSearchRec(Route route, ModifiableSearchParams searchParams, int range, int leftRange, boolean incomingEdges, boolean outgoingEdges, PruningMethod pruningMethod, Map<ResourceEntity, List<Route>> directNeighbours, Map<Integer, List<Route>> routes) {
        if (leftRange == 0 || !(route.getLastWaypoint() instanceof ResourceEntity)) {
            return;
        }

        ResourceEntity currPivot = (ResourceEntity) route.getLastWaypoint();
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
