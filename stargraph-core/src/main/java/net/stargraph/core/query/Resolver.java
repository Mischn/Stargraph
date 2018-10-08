package net.stargraph.core.query;

import net.stargraph.core.Namespace;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.query.nli.DataModelBindingContext;
import net.stargraph.core.query.nli.DataModelType;
import net.stargraph.core.query.nli.TriplePattern;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.PropertyPath;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Resolver {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected Marker marker = MarkerFactory.getMarker("query");

    private static final int PREDICATE_RANGE = 2;
    private static final long POSSIBLE_PIVOT_LIMIT = 10;
    private static final long POSSIBLE_CLASS_LIMIT = 10;
    private static final long POSSIBLE_PREDICATE_LIMIT = 10;
    private static final long USED_PIVOT_LIMIT = 1;
    private static final long USED_CLASS_LIMIT = 1;
    private static final long USED_PREDICATE_LIMIT = 1;

    protected EntitySearcher entitySearcher;
    protected String dbId;
    protected Namespace namespace;
    protected Map<String, Map<DataModelBindingContext, Set<Score>>> possibleMappings; // maps placeholder & context to a list of scored entities
    protected Map<String, Map<DataModelBindingContext, Set<Score>>> mappings; // maps placeholder & context to a list of scored entities

    public Resolver(EntitySearcher entitySearcher, Namespace namespace, String dbId) {
        this.entitySearcher = entitySearcher;
        this.dbId = dbId;
        this.namespace = namespace;
        this.possibleMappings = new ConcurrentHashMap<>();
        this.mappings = new ConcurrentHashMap<>();
    }

    public void reset() {
        this.possibleMappings.clear();
        this.mappings.clear();
    }

    // MAPPINGS

    public void setCustomMappings(Map<String, Map<DataModelBindingContext, List<String>>> customMappings) {
        logger.info(marker, "Set custom mappings: {}", customMappings);

        this.mappings.clear();

        for (String placeholder : customMappings.keySet()) {
            for (DataModelBindingContext context : customMappings.get(placeholder).keySet()) {
                List<Score> scores = new ArrayList<>();
                for (String id : customMappings.get(placeholder).get(context)) {
                    if (context.equals(DataModelBindingContext.PREDICATE)) {
                        PropertyPath propertyPath = entitySearcher.getPropertyPath(dbId, id);
                        if (propertyPath == null) {
                            logger.error(marker, "Unable to lookup custom mapping with id: {}", id);
                        } else {
                            scores.add(new Score(propertyPath, 1.));
                        }
                    } else {
                        InstanceEntity instanceEntity = entitySearcher.getInstanceEntity(dbId, id);
                        if (instanceEntity == null) {
                            logger.error(marker, "Unable to lookup custom mapping with id: {}", id);
                        } else {
                            scores.add(new Score(instanceEntity, 1.));
                        }
                    }
                }
                addMappings(placeholder, context, scores);
            }
        }
    }

    private void addPossibleMappings(String placeHolder, DataModelBindingContext context, List<Score> scores) {
        // Expanding the Namespace for all entities
        List<Score> expanded = new Scores(scores.stream().map(s -> new Score(namespace.expand(s.getEntry()), s.getValue())).collect(Collectors.toList()));
        possibleMappings.computeIfAbsent(placeHolder, (b) -> new HashMap<>()).computeIfAbsent(context, (c) -> new LinkedHashSet<>()).addAll(expanded);
    }

    private void addMappings(String placeHolder, DataModelBindingContext context, List<Score> scores) {
        // Expanding the Namespace for all entities
        List<Score> expanded = new Scores(scores.stream().map(s -> new Score(namespace.expand(s.getEntry()), s.getValue())).collect(Collectors.toList()));
        mappings.computeIfAbsent(placeHolder, (b) -> new HashMap<>()).computeIfAbsent(context, (c) -> new LinkedHashSet<>()).addAll(expanded);
    }

    public boolean hasPossibleMappings(String placeHolder, DataModelBindingContext context) {
        return possibleMappings.containsKey(placeHolder) && possibleMappings.get(placeHolder).containsKey(context) && possibleMappings.get(placeHolder).get(context).size() > 0;
    }

    public boolean hasMappings(String placeHolder, DataModelBindingContext context) {
        return mappings.containsKey(placeHolder) && mappings.get(placeHolder).containsKey(context) && mappings.get(placeHolder).get(context).size() > 0;
    }

    public Set<Score> getPossibleMappings(String placeHolder, DataModelBindingContext context) {
        if (hasPossibleMappings(placeHolder, context)) {
            return possibleMappings.get(placeHolder).get(context);
        }
        return Collections.emptySet();
    }

    public Set<Score> getMappings(String placeHolder, DataModelBindingContext context) {
        if (hasMappings(placeHolder, context)) {
            return mappings.get(placeHolder).get(context);
        }
        return Collections.emptySet();
    }

    public Map<String, Map<DataModelBindingContext, Set<Score>>> getPossibleMappings() {
        return possibleMappings;
    }

    public Map<String, Map<DataModelBindingContext, Set<Score>>> getMappings() {
        return mappings;
    }

    // OTHER

    private boolean isURI(String str) {
        return (str.toLowerCase().startsWith("http://") || str.toLowerCase().startsWith("https://"));
    }

    private InstanceEntity resolveUri(String str) {
        InstanceEntity instance = null;
        if (isURI(str.trim())) {
            instance = entitySearcher.getInstanceEntity(dbId, str.trim());
        }
        return instance;
    }

    private Scores limitScores(Scores scores, long limit) {
        if (limit < 0) {
            return scores;
        } else {
            return new Scores(scores.stream().limit(limit).collect(Collectors.toList()));
        }
    }

    public Scores rescoreClasses(Scores scores) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);

        List<String> noRootIds = new ArrayList<>();
        for (Score score : scores) {
            String id = score.getRankableView().getId();
            List<String> otherIds = scores.stream().map(s -> s.getRankableView().getId()).filter(i -> !i.equals(id)).collect(Collectors.toList());
            if (entitySearcher.isClassMember(id, otherIds, searchParams)) {
                noRootIds.add(id);
            }
        }

        // boost roots
        Scores rescored = new Scores();
        for (Score score : scores) {
            double s = (!noRootIds.contains(score.getRankableView().getId()))? 1 : 0;
            double newScore = (0.5 * score.getValue()) + (0.5 * s);
            rescored.add(new Score(score.getEntry(), newScore));
        }

        // order
        rescored.sort(true);

        return rescored;
    }


    public void resolveTriple(TriplePattern.BoundTriple triple) {
        logger.debug(marker, "Resolve triple {}", triple);

        if (triple.getP().getModelType() == DataModelType.TYPE) {
            if (triple.getO().getModelType() == DataModelType.INSTANCE || triple.getO().getModelType().equals(DataModelType.CLASS)) {
                resolveClass(triple.getO());
            }
        } else if (triple.getP().getModelType() == DataModelType.PROPERTY || triple.getP().getModelType() == DataModelType.CLASS) {
            List<InstanceEntity> pivots = null;

            if (triple.getS().getModelType() == DataModelType.INSTANCE || triple.getS().getModelType().equals(DataModelType.CLASS)) {
                pivots = resolvePivot(triple.getS()).stream().map(s -> (InstanceEntity)s.getEntry()).collect(Collectors.toList());
            } else if (triple.getO().getModelType() == DataModelType.INSTANCE || triple.getO().getModelType().equals(DataModelType.CLASS)) {
                pivots = resolvePivot(triple.getO()).stream().map(s -> (InstanceEntity)s.getEntry()).collect(Collectors.toList());
            }

            if (pivots != null && pivots.size() > 0) {
                resolvePredicate(pivots, triple.getP());
            }
        } else {
            resolveDefault(triple);
        }
    }

    private void resolveDefault(TriplePattern.BoundTriple triple) {
        if (triple.getS().getModelType() == DataModelType.INSTANCE || triple.getS().getModelType().equals(DataModelType.CLASS)) {
            resolvePivot(triple.getS());
        }
        if (triple.getO().getModelType() == DataModelType.INSTANCE || triple.getO().getModelType().equals(DataModelType.CLASS)) {
            resolvePivot(triple.getO());
        }
    }



    public Set<Score> resolveClass(DataModelBinding binding) {
        DataModelBindingContext context = DataModelBindingContext.NON_PREDICATE;

        if (hasMappings(binding.getPlaceHolder(), context)) {
            logger.debug(marker, "Class was already resolved to {}", getMappings(binding.getPlaceHolder(), context));
            return getMappings(binding.getPlaceHolder(), context);
        }

        final int PRE_LIMIT = 50;
        Scores scores = new Scores();

        // check for concrete URI
        InstanceEntity resolvedUri = resolveUri(binding.getTerm());
        if (resolvedUri != null) {
            scores.add(new Score(resolvedUri, 1));
        } else {
            logger.debug(marker, "Resolve class, searching for term '{}'", binding.getTerm());
            scores.addAll(searchClass(binding, context));
            logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));

            // re-rank classes
            logger.debug(marker, "Rescore classes");
            scores = rescoreClasses(limitScores(scores, PRE_LIMIT)); // Limit, because otherwise, the SPARQL-Query gets too long -> StackOverflow
            logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
        }

        if (scores.size() > 0) {
            logger.debug(marker, "Possible mappings for binding {}: {}", binding, limitScores(scores, POSSIBLE_CLASS_LIMIT));
            addPossibleMappings(binding.getPlaceHolder(), context, limitScores(scores, POSSIBLE_CLASS_LIMIT));

            logger.debug(marker, "Used mappings for binding {}: {}", binding, limitScores(scores, USED_CLASS_LIMIT));
            addMappings(binding.getPlaceHolder(), context, limitScores(scores, USED_CLASS_LIMIT));

            return getMappings(binding.getPlaceHolder(), context);
        } else {
            logger.error(marker, "Could not resolve class for {}", binding);
            return Collections.emptySet();
        }
    }

    public Scores searchClass(DataModelBinding binding, DataModelBindingContext context) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(binding.getTerm()));
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.classSearch(searchParams, searchString, rankParams);
    }




    public Set<Score> resolvePredicate(List<InstanceEntity> pivots, DataModelBinding binding) {
        DataModelBindingContext context = DataModelBindingContext.PREDICATE;

        if (hasMappings(binding.getPlaceHolder(), context)) {
            logger.debug(marker, "Predicate was already resolved to {}", getMappings(binding.getPlaceHolder(), context));
            return getMappings(binding.getPlaceHolder(), context);
        }

        Scores scores = new Scores();

        // check for concrete URI
        InstanceEntity resolvedUri = resolveUri(binding.getTerm());
        if (resolvedUri != null) {
            scores.add(new Score(resolvedUri, 1));
        } else {
            for (InstanceEntity pivot : pivots) {
                logger.debug(marker, "Resolve predicate for pivot {}, searching for term '{}'", pivot, binding.getTerm());
                scores.addAll(searchPredicate(pivot, binding, context));
                logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }
        }

        if (scores.size() > 0) {
            logger.debug(marker, "Possible mappings for binding {}: {}", binding, limitScores(scores, POSSIBLE_PREDICATE_LIMIT));
            addPossibleMappings(binding.getPlaceHolder(), context, limitScores(scores, POSSIBLE_PREDICATE_LIMIT));

            logger.debug(marker, "Used mappings for binding {}: {}", binding, limitScores(scores, USED_PREDICATE_LIMIT));
            addMappings(binding.getPlaceHolder(), context, limitScores(scores, USED_PREDICATE_LIMIT));

            return getMappings(binding.getPlaceHolder(), context);
        } else {
            logger.error(marker, "Could not resolve predicate for {}", binding);
            return Collections.emptySet();
        }
    }

    public Scores searchPredicate(InstanceEntity pivot, DataModelBinding binding, DataModelBindingContext context) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        String rankString = binding.getTerm();
        ModifiableRankParams rankParams = ParamsBuilder.word2vec().threshold(Threshold.min(0.3d)); //TODO magic number
        return entitySearcher.pivotedPropertySearch(pivot, searchParams, PREDICATE_RANGE, rankString, rankParams);
    }


    public Set<Score> resolvePivot(DataModelBinding binding) {
        DataModelBindingContext context = DataModelBindingContext.NON_PREDICATE;
        return resolveInstance(binding, context, POSSIBLE_PIVOT_LIMIT, USED_PIVOT_LIMIT);
    }


    public Set<Score> resolveInstance(DataModelBinding binding, DataModelBindingContext context, long possibleLimit, long usedLimit) {
        if (hasMappings(binding.getPlaceHolder(), context)) {
            logger.debug(marker, "Instance was already resolved to {}", getMappings(binding.getPlaceHolder(), context));
            return getMappings(binding.getPlaceHolder(), context);
        }

        Scores scores = new Scores();

        // check for concrete URI
        InstanceEntity resolvedUri = resolveUri(binding.getTerm());
        if (resolvedUri != null) {
            scores.add(new Score(resolvedUri, 1));
        } else {
            logger.debug(marker, "Resolve instance, searching for term '{}'", binding.getTerm());
            scores.addAll(searchInstance(binding.getTerm(), context));
            logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
        }

        if (scores.size() > 0) {
            logger.debug(marker, "Possible mappings for binding {}: {}", binding, limitScores(scores, possibleLimit));
            addPossibleMappings(binding.getPlaceHolder(), context, limitScores(scores, possibleLimit));

            logger.debug(marker, "Used mappings for binding {}: {}", binding, limitScores(scores, usedLimit));
            addMappings(binding.getPlaceHolder(), context, limitScores(scores, usedLimit));

            return getMappings(binding.getPlaceHolder(), context);
        } else {
            logger.error(marker, "Could not resolve instance for {}", binding);
            return Collections.emptySet();
        }
    }

    public Scores searchInstance(String instanceTerm, DataModelBindingContext context) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(instanceTerm));
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.instanceSearch(searchParams, searchString, rankParams);
    }
}
