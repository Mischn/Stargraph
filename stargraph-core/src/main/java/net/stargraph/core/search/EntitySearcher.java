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
import net.stargraph.core.model.ModelCreator;
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
    private static final int FUZZINESS = 1;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("elastic");

    private Stargraph stargraph;
    private ModelCreator modelCreator;

    public EntitySearcher(Stargraph stargraph) {
        this.stargraph = stargraph;
        this.modelCreator = new ModelCreator(this);
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
     * Returns a propertyPath for the given id.
     * @param dbId
     * @param id
     * @return
     */
    public PropertyPath getPropertyPath(String dbId, String id) {
        PropertyPath res = null;
        for (PropertyPath.PropertyParse propertyParse : PropertyPath.parseId(id)) {
            PropertyEntity propertyEntity = getPropertyEntity(dbId, propertyParse.propertyId);
            if (propertyEntity == null) {
                return null;
            }

            if (res == null) {
                res = new PropertyPath(propertyEntity, propertyParse.direction);
            } else {
                res = res.extend(propertyEntity, propertyParse.direction);
            }
        }
        return res;
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
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(Arrays.asList(memberId), null, searchParams);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // We have to remap the facts to the classes.
        return scores.stream()
                .map(s -> ((Fact)s.getEntry()).getObject())
                .filter(x -> x instanceof InstanceEntity)
                .map(x -> (InstanceEntity)x)
                .distinct()
                .limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit())
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
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(null, Arrays.asList(classId), searchParams);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // We have to remap the facts to the members.
        return scores.stream()
                .map(s -> ((Fact)s.getEntry()).getSubject())
                .filter(x -> x instanceof InstanceEntity)
                .map(x -> (InstanceEntity)x)
                .distinct()
                .limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit())
                .collect(Collectors.toList());
    }

    /**
     * Check if entity is a member of any of the classes.
     * @param entity
     * @param classes
     * @param searchParams
     * @return
     */
    public boolean isClassMember(InstanceEntity entity, List<InstanceEntity> classes, ModifiableSearchParams searchParams) {
        return isClassMember(entity.getId(), classes.stream().map(c -> c.getId()).collect(Collectors.toList()), searchParams);
    }

    /**
     * Check if entity is a member of any of the classes.
     * @param entityId
     * @param classIds
     * @param searchParams
     * @return
     */
    public boolean isClassMember(String entityId, List<String> classIds, ModifiableSearchParams searchParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.FACT);
        searchParams.searchSpaceLimit(1);
        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(Arrays.asList(entityId), classIds, searchParams);
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return scores.size() > 0;
    }

    /**
     * Check if entity is a class.
     * @param entity
     * @return
     */
    public boolean isClass(InstanceEntity entity, ModifiableSearchParams searchParams) {
        return isClass(entity.getId(), searchParams);
    }

    /**
     * Check if entity is a class.
     * @param entityId
     * @return
     */
    public boolean isClass(String entityId, ModifiableSearchParams searchParams) {
        searchParams.searchSpaceLimit(1);
        return getClassMembers(entityId, searchParams).size() > 0;
    }




    /**
     * Search instances by their value & otherValues (fuzzy match with searchTerm).
     * @param searchParams
     * @param rankParams
     * @return
     */
    public Scores instanceSearch(ModifiableSearchParams searchParams, ModifiableSearchString searchString, ModifiableRankParams rankParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.ENTITY);
        // ensure to use phrase (TODO this is not transparent to calling methods)
        if (!searchString.hasSearchPhrases()) {
            searchString.searchPhrase(new ModifiableSearchString.Phrase(searchString.getSearchTerms().stream().collect(Collectors.joining(" "))));
        }
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findInstanceInstances(searchParams, searchString, true, FUZZINESS, false);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // Re-Rank
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }
        scores = Rankers.apply(scores, rankParams, searchString.getRankableStr());

        return new Scores(scores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }

    /**
     * Search classes by their value & otherValues (fuzzy match with searchTerm).
     * @param searchParams
     * @param rankParams
     * @return
     */
    public Scores classSearch(ModifiableSearchParams searchParams, ModifiableSearchString searchString, ModifiableRankParams rankParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.ENTITY);
        // ensure to use phrase (TODO this is not transparent to calling methods)
        if (!searchString.hasSearchPhrases()) {
            searchString.searchPhrase(new ModifiableSearchString.Phrase(searchString.getSearchTerms().stream().collect(Collectors.joining(" "))));
        }
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findClassInstances(searchParams, searchString, true, FUZZINESS, false);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // Re-Rank
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }
        scores = Rankers.apply(scores, rankParams, searchString.getRankableStr());

        return new Scores(scores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }


    /**
     * Search properties by their hyponyms, hypernyms or synonyms (fuzzy match with searchTerm).
     * @param searchParams
     * @param rankParams
     * @return
     */
    public Scores propertySearch(ModifiableSearchParams searchParams, ModifiableSearchString searchString, ModifiableRankParams rankParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.PROPERTY);
        // ensure to use phrase (TODO this is not transparent to calling methods)
        if (!searchString.hasSearchPhrases()) {
            searchString.searchPhrase(new ModifiableSearchString.Phrase(searchString.getSearchTerms().stream().collect(Collectors.joining(" "))));
        }
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findPropertyInstances(searchParams, searchString,false, FUZZINESS, false);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        // Re-Rank
        if (rankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
        }
        scores = Rankers.apply(scores, rankParams, searchString.getRankableStr());

        return new Scores(scores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }

    public Scores documentSearch(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument, boolean mustPhrases) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.DOCUMENT);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findDocumentInstances(searchParams, searchString, docTypes, entityDocument, true, FUZZINESS, mustPhrases);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return new Scores(scores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
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
    public Scores pivotedSearch(String pivotId, ModifiableSearchParams searchParams, ModifiableRangeSearchParams rangeSearchParams, int range, String rankString, ModifiableRankParams rankParams, boolean returnBestMatchEntities) {
        Namespace namespace = stargraph.getKBCore(searchParams.getDbId()).getNamespace();
        return pivotedSearch(modelCreator.createInstance(pivotId, searchParams.getDbId(), namespace), searchParams, rangeSearchParams, range, rankString, rankParams, returnBestMatchEntities);
    }
    public Scores pivotedSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, ModifiableRangeSearchParams rangeSearchParams, int range, String rankString, ModifiableRankParams rankParams, boolean returnBestMatchEntities) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        List<Route> neighbours = neighbourSearch(pivot, searchParams.clone().resultLimit(-1), rangeSearchParams, range);

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
        Scores rankedScores = Rankers.apply(propScores, rankParams, rankString);

        // Re-Rank again using heuristics about property-paths
        rankedScores = rerankPropertyPaths(rankedScores);

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

        return new Scores(result.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }


    public static Scores rerankPropertyPaths(Scores scores) {
        final double RANGE_WEIGHT = 0.2;
        final double INVERSE_WEIGHT = 0.1;
        final double OLD_WEIGHT = 1. - (RANGE_WEIGHT + INVERSE_WEIGHT);

        // Re-Rank based on heuristics
        Scores res = new Scores();
        for (Score score : scores) {

            // the smaller range, the better
            double rangeScore = 1. / ((PropertyPath)score.getEntry()).getProperties().size();

            // the less inverse directions the better
            long nrIncoming = ((PropertyPath)score.getEntry()).getDirections().stream().filter(d -> d.equals(PropertyPath.Direction.INCOMING)).count();
            double inverseScore = 1. / (nrIncoming+1);

            double s = (OLD_WEIGHT * score.getValue()) + (RANGE_WEIGHT * rangeScore) + (INVERSE_WEIGHT * inverseScore);
            res.add(new Score(score.getEntry(), s));
        }

        res.sort(true);
        return res;
    }


     public Scores pivotedPropertySearch(InstanceEntity pivot, ModifiableSearchParams searchParams, int range, String rankString, ModifiableRankParams rankParams) {
        ModifiableRangeSearchParams rangeSearchParams = ModifiableRangeSearchParams.create()
                .incomingEdges(true)
                .outgoingEdges(true)
                .limitToRepresentatives(true)
                .propertyTypes(SearchQueryGenerator.PropertyTypes.NON_TYPE_ONLY)
                .pruningStrategies(Arrays.asList(new ModifiableRangeSearchParams.NoCyclesPruning(), new ModifiableRangeSearchParams.EdgeDirectionPruning()));

         return pivotedSearch(pivot, searchParams, rangeSearchParams, range, rankString, rankParams, false);
     }


//    public Scores pivotedClassSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, String rankString, ModifiableRankParams rankParams, int range, boolean returnClassMembers) {
//        KBCore core = stargraph.getKBCore(searchParams.getDbId());
//
//        List<Route> neighbours = neighbourSearch(pivot, searchParams.clone().resultLimit(-1), range, true, true, false, Arrays.asList(SearchQueryGenerator.PropertyTypes.TYPE, SearchQueryGenerator.PropertyTypes.NON_TYPE), null);
//
//        // filter for resource entities
//        neighbours = neighbours.stream().filter(n -> n.getLastWaypoint() instanceof InstanceEntity).collect(Collectors.toList());
//
//        List<String> neighbourIds = neighbours.stream()
//                .map(n -> n.getLastWaypoint().getId())
//                .distinct()
//                .collect(Collectors.toList());
//
//        searchParams.model(BuiltInModel.FACT);
//        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
//        SearchQueryHolder holder = searchQueryGenerator.findClassFacts(neighbourIds, null, searchParams);
//        Searcher searcher = core.getGraphSearcher();
//
//        // Fetch initial candidates from the search engine
//        Scores scores = searcher.search(holder);
//
//        // maps classes to class-members
//        Map<InstanceEntity, List<InstanceEntity>> classMembers = new HashMap<>();
//        scores.stream().forEach(s -> {
//            Fact fact = (Fact)s.getEntry();
//            if (fact.getSubject() instanceof InstanceEntity && fact.getObject() instanceof InstanceEntity) { ;
//                InstanceEntity member = (InstanceEntity) fact.getSubject();
//                InstanceEntity clazz = (InstanceEntity) fact.getObject();
//
//                List<InstanceEntity> members = classMembers.getOrDefault(clazz, new ArrayList<>());
//                if (!members.contains(member)) {
//                    members.add(member);
//                }
//                classMembers.put(clazz, members);
//            }
//        });
//        logger.info(marker, "Fetched {} classes", classMembers.size());
//
//        // We have to remap to classes
//        Scores classScores = new Scores(classMembers.keySet().stream().map(c -> new Score(c, 0.0)).collect(Collectors.toList()));
//
//        // Re-Rank
//        if (rankParams instanceof ModifiableIndraParams) {
//            core.configureDistributionalParams((ModifiableIndraParams) rankParams);
//        }
//        Scores rankedScores = Rankers.apply(classScores, rankParams, rankString);
//        Scores result = rankedScores;
//
//        if (returnClassMembers) {
//            if (rankedScores.size() <= 0) {
//                return new Scores();
//            }
//            Score bestScore = rankedScores.get(0);
//            InstanceEntity bestClass = (InstanceEntity) bestScore.getEntry();
//            logger.debug("Best match is {}, returning instances ..", bestClass);
//            result = new Scores(classMembers.get(bestClass).stream().map(m -> new Score(m, bestScore.getValue())).collect(Collectors.toList()));
//        }
//
//        return new Scores(result.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
//    }

    public Scores similarDocumentSearch(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.DOCUMENT);
        SearchQueryGenerator searchQueryGenerator = core.getSearchQueryGenerator(searchParams.getKbId().getModel());
        SearchQueryHolder holder = searchQueryGenerator.findSimilarDocuments(searchParams, searchString, docTypes, entityDocument);
        Searcher searcher = core.getSearcher(searchParams.getKbId().getModel());

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        return new Scores(scores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }

    public Scores likeThisInstanceSearch(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes) {
        Scores scores = similarDocumentSearch(searchParams, searchString, docTypes, true);

        // now map documents back to their entities
        Scores entityScores = new Scores();
        for (Score score : scores) {
            Document doc = (Document)score.getEntry();

            InstanceEntity ent = getInstanceEntity(searchParams.getDbId(), doc.getEntity());
            if (ent != null) {
                entityScores.add(new Score(ent, score.getValue()));
            }
        }

        return new Scores(entityScores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }

    public Scores similarInstanceSearch(InstanceEntity entity, ModifiableSearchParams searchParams, List<String> docTypes) {
        String dbId = searchParams.getDbId();

        // Search for entity-documents
        List<Document> entityDocs = getDocumentsForResourceEntity(dbId, entity.getId(), docTypes);
        if (entityDocs.size() == 0) {
            logger.warn(marker, "Did not find any documents for entity {}", entity.getId());
            return new Scores();
        }

        List<String> texts = entityDocs.stream().map(d -> d.getText()).collect(Collectors.toList());

        searchParams.model(BuiltInModel.DOCUMENT);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrases(texts.stream().map(t -> new ModifiableSearchString.Phrase(t)).collect(Collectors.toList()));
        Scores scores = similarDocumentSearch(searchParams, searchString, null, true);

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

        return new Scores(entitiyScores.stream().limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList()));
    }










    // HELPER FUNCTIONS

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public List<Route> neighbourSearch(String pivotId, ModifiableSearchParams searchParams, ModifiableRangeSearchParams rangeSearchParams, int range) {
        Namespace namespace = stargraph.getKBCore(searchParams.getDbId()).getNamespace();
        return neighbourSearch(modelCreator.createInstance(pivotId, searchParams.getDbId(), namespace), searchParams, rangeSearchParams, range);
    }
    public List<Route> neighbourSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, ModifiableRangeSearchParams rangeSearchParams, int range) {
        Namespace namespace = stargraph.getKBCore(searchParams.getDbId()).getNamespace();
        InstanceEntity myPivot = namespace.shrink(pivot);

        if (range == 0) {
            return Arrays.asList(new Route(myPivot));
        }

        Map<InstanceEntity, List<Route>> directNeighbours = new HashMap<>(); // for avoiding redundant calculations
        Map<Integer, List<Route>> neighbours = new HashMap<>();
        neighbourSearchRec(
                new Route(myPivot),
                searchParams.clone().resultLimit(-1),
                rangeSearchParams,
                range,
                range,
                directNeighbours,
                neighbours
        );

        return neighbours.values().stream().flatMap(Collection::stream).limit((searchParams.getResultLimit() < 0)? Long.MAX_VALUE : searchParams.getResultLimit()).collect(Collectors.toList());
    }

    // direct neighbours only
    private List<Route> directNeighbourSearch(InstanceEntity pivot, ModifiableSearchParams searchParams, ModifiableRangeSearchParams rangeSearchParams) {
        KBCore core = stargraph.getKBCore(searchParams.getDbId());

        searchParams.model(BuiltInModel.FACT);
        SearchQueryGenerator searchQueryGenerator = core.getGraphSearchQueryGenerator();
        SearchQueryHolder holder = searchQueryGenerator.findPivotFacts(pivot, searchParams, rangeSearchParams.isOutgoingEdges(), rangeSearchParams.isIncomingEdges(), rangeSearchParams.getPropertyTypes());
        Searcher searcher = core.getGraphSearcher();

        // Fetch initial candidates from the search engine
        Scores scores = searcher.search(holder);

        List<Route> result = new ArrayList();
        for (Score score : scores) {
            Fact fact = (Fact)score.getEntry();
            if (rangeSearchParams.isOutgoingEdges() && fact.getSubject().equals(pivot)) {
                result.add(new Route(pivot).extend(fact.getPredicate(), PropertyPath.Direction.OUTGOING, fact.getObject()));
            } else if (rangeSearchParams.isIncomingEdges() && fact.getObject().equals(pivot) && fact.getSubject() instanceof NodeEntity) {
                result.add(new Route(pivot).extend(fact.getPredicate(), PropertyPath.Direction.INCOMING, (NodeEntity) fact.getSubject()));
            }
        }

        // limit to representatives
        if (rangeSearchParams.isLimitToRepresentatives()) {
            Map<String, Route> representatives = new HashMap();
            for (Route directN : result) {
                // a simple approach of combining the property (use id, not value -> avoid value-lookup), direction and the target-classname
                String hashKey = directN.getPropertyPath().getLastProperty().getId() + directN.getPropertyPath().getLastDirection() + directN.getLastWaypoint().getClass().getSimpleName();
                representatives.put(hashKey, directN);
            }
            result = new ArrayList<>(representatives.values());
        }

        return result;
    }

    private void neighbourSearchRec(Route route, ModifiableSearchParams searchParams, ModifiableRangeSearchParams rangeSearchParams, int range, int leftRange, Map<InstanceEntity, List<Route>> directNeighbours, Map<Integer, List<Route>> routes) {
        if (leftRange == 0 || !(route.getLastWaypoint() instanceof InstanceEntity)) {
            return;
        }

//        System.out.println(route.getWaypoints());
//        System.out.println(route.getPropertyPath());

        InstanceEntity currPivot = (InstanceEntity) route.getLastWaypoint();
        List<Route> directNs;
        if (directNeighbours.containsKey(currPivot)) {
            directNs = directNeighbours.get(currPivot);
        } else {
            directNs = directNeighbourSearch(currPivot, searchParams, rangeSearchParams);
            directNeighbours.put(currPivot, directNs);
        }

        // create new routes
        List<Route> newRoutes = new ArrayList<>();
        for (Route directN : directNs) {
            PropertyEntity newProperty = directN.getPropertyPath().getLastProperty();
            PropertyPath.Direction newDirection = directN.getPropertyPath().getLastDirection();
            NodeEntity newWaypoint = directN.getLastWaypoint();
            Route newRoute = route.extend(newProperty, newDirection, newWaypoint);
            newRoutes.add(newRoute);
        }

        // execute pruning strategies
        Set<Route> rejectRoutes = new HashSet<>();
        Set<Route> notTraverseRoutes = new HashSet<>();
        ModifiableRangeSearchParams nextRangeSearchParams = rangeSearchParams.clone();

        for (ModifiableRangeSearchParams.PruningStrategy pruningStrategy : rangeSearchParams.getPruningStrategies()) {
            ModifiableRangeSearchParams.PruningStrategy.Result r = pruningStrategy.prune(newRoutes, nextRangeSearchParams);
            nextRangeSearchParams = r.getNextRangeSearchParams();

            rejectRoutes.addAll(r.getRejectRoutes());
            notTraverseRoutes.addAll(r.getNotTraverseRoutes());
        }

        // add to result
        for (Route r : newRoutes) {
            if (rejectRoutes.contains(r)) {
                logger.debug(marker, "Pruned (Reject) {}", r);
            } else {
                routes.computeIfAbsent(range - (leftRange - 1), (x) -> new ArrayList<>()).add(r);
            }
        }

        // recursion
        for (Route r : newRoutes) {
            if (notTraverseRoutes.contains(r)) {
                logger.debug(marker, "Pruned (Not-Traverse) {}", r);
            } else {
                neighbourSearchRec(r, searchParams, nextRangeSearchParams, range, leftRange - 1, directNeighbours, routes);
            }
        }
    }







}
