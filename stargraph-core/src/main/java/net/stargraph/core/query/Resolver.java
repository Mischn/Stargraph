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

    protected EntitySearcher entitySearcher;
    protected String dbId;
    protected Namespace namespace;
    protected Map<String, Map<DataModelBindingContext, Set<Score>>> possibleMappings; // maps placeholder & context to a list of scored entities
    protected Map<String, Map<DataModelBindingContext, Set<Score>>> mappings; // maps placeholder & context to a list of scored entities

    // individual resolvers
    protected final InstanceResolver instanceResolver = new InstanceResolver();
    protected final ClassResolver classResolver = new ClassResolver();
    protected final PredicateResolver predicateResolver = new PredicateResolver();
    protected final PivotedPredicateResolver pivotedPredicateResolver = new PivotedPredicateResolver();

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

    // MAPPINGS ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setCustomMappings(Map<String, Map<DataModelBindingContext, List<String>>> customMappings) {
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

    private boolean isResolved(String placeHolder, DataModelBindingContext context) {
        return hasMappings(placeHolder, context);
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


    // OTHERS //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean resolvable(DataModelType modelType) {
        return !Arrays.asList(DataModelType.VARIABLE, DataModelType.TYPE, DataModelType.EQUALS).contains(modelType);
    }

    private boolean isResolved(TriplePattern.BoundTriple triple) {
        if (resolvable(triple.getS().getModelType()) && !isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
            return false;
        }
        if (resolvable(triple.getP().getModelType()) && !isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE)) {
            return false;
        }
        if (resolvable(triple.getO().getModelType()) && !isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
            return false;
        }
        return true;
    }


    public Set<Score> resolveInstance(DataModelBinding binding, DataModelBindingContext context, long possibleLimit, long usedLimit) {
        InstanceResolver instanceResolver = new InstanceResolver();
        instanceResolver.setBinding(binding);
        instanceResolver.setContext(context);
        instanceResolver.setPossibleLimit(possibleLimit);
        instanceResolver.setUsedLimit(usedLimit);
        return instanceResolver.resolve();
    }

    public void resolveTriples(List<TriplePattern.BoundTriple> triples) {
        logger.debug(marker, "Resolve triples {}", triples);
        List<TriplePattern.BoundTriple> unresolvedTriples = new ArrayList<>(triples);

        // a best-effort heuristic in which order to resolve the triples
        boolean loop = true;
        while (loop) {
            boolean loop2 = true;
            while (loop2) {
                while (resolveNextDependant(unresolvedTriples)) {
                }
                loop2 = resolveNextInstance(unresolvedTriples);
            }
            loop = resolveNextSO(unresolvedTriples);
        }
        resolveAll(unresolvedTriples);
    }

    private boolean resolveNextDependant(List<TriplePattern.BoundTriple> unresolvedTriples) {
        logger.debug(marker, "Resolving-Step: Resolve next dependant");

        for (int i = 0; i < unresolvedTriples.size(); i++) {
            TriplePattern.BoundTriple triple = unresolvedTriples.get(i);

            // Resolve predicate:
            // ???   [TO_BE_RESOLVED]   [resolved]
            // [resolved]   [TO_BE_RESOLVED]   ???
            if (!isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE) && resolvable(triple.getP().getModelType())) {
                if (isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) || isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {

                    DataModelBinding pivotBinding = isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)? triple.getS() : triple.getO();

                    List<InstanceEntity> pivots = getMappings(pivotBinding.getPlaceHolder(), DataModelBindingContext.NON_PREDICATE).stream()
                            .filter(s -> s.getEntry() instanceof InstanceEntity)
                            .map(s -> (InstanceEntity)s.getEntry())
                            .collect(Collectors.toList());

                        pivotedPredicateResolver.setBinding(triple.getP());
                        pivotedPredicateResolver.setContext(DataModelBindingContext.PREDICATE);
                        pivotedPredicateResolver.setPivots(pivots);
                        pivotedPredicateResolver.resolve();

                        if (isResolved(triple)) {
                            unresolvedTriples.remove(i);
                        }

                        if (isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE)) {
                            return true;
                        }
                }
            }

            //TODO resolve other dependant structures

            // Resolve class member
            // [TO_BE_RESOLVED]   TYPE   [resolved]


            // Resolve class
            // [resolved]   TYPE   [TO_BE_RESOLVED]


            // Resolve equality
            // [resolved]   EQUALS   [TO_BE_RESOLVED]
            // [TO_BE_RESOLVED]   EQUALS   [resolved]
            if (triple.getP().getModelType().equals(DataModelType.EQUALS)) {
                if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getS().getModelType()) && isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {

                    // just copy mappings from o to s
                    Set<Score> mappings = getMappings(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    Set<Score> possibleMappings = getPossibleMappings(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    addMappings(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE, new ArrayList<>(mappings));
                    addPossibleMappings(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE, new ArrayList<>(mappings));
                }
                if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getO().getModelType()) && isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {

                    // just copy mappings from s to o
                    Set<Score> mappings = getMappings(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    Set<Score> possibleMappings = getPossibleMappings(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    addMappings(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE, new ArrayList<>(mappings));
                    addPossibleMappings(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE, new ArrayList<>(mappings));
                }
            }
        }

        return false;
    }

    private boolean resolveNextInstance(List<TriplePattern.BoundTriple> unresolvedTriples) {
        logger.debug(marker, "Resolving-Step: Resolve next instance");

        for (int i = 0; i < unresolvedTriples.size(); i++) {
            TriplePattern.BoundTriple triple = unresolvedTriples.get(i);

            if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getS().getModelType()) && triple.getS().getModelType().equals(DataModelType.INSTANCE)) {
                logger.debug(marker, "Resolve triple: {}", triple);

                instanceResolver.setBinding(triple.getS());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();

                if (isResolved(triple)) {
                    unresolvedTriples.remove(i);
                }

                if (isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }

            if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getO().getModelType()) && triple.getO().getModelType().equals(DataModelType.INSTANCE)) {
                logger.debug(marker, "Resolve triple: {}", triple);

                instanceResolver.setBinding(triple.getO());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();

                if (isResolved(triple)) {
                    unresolvedTriples.remove(i);
                }

                if (isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean resolveNextSO(List<TriplePattern.BoundTriple> unresolvedTriples) {
        logger.debug(marker, "Resolving-Step: Resolve next subject or object");

        // subjects
        for (int i = 0; i < unresolvedTriples.size(); i++) {
            TriplePattern.BoundTriple triple = unresolvedTriples.get(i);

            if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getS().getModelType())) {
                logger.debug(marker, "Resolve triple: {}", triple);

                instanceResolver.setBinding(triple.getS());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();

                if (isResolved(triple)) {
                    unresolvedTriples.remove(i);
                }

                if (isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }
        }

        // objects
        for (int i = 0; i < unresolvedTriples.size(); i++) {
            TriplePattern.BoundTriple triple = unresolvedTriples.get(i);

            if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getO().getModelType())) {
                logger.debug(marker, "Resolve triple: {}", triple);

                if (triple.getP().getModelType().equals(DataModelType.TYPE)) {
                    classResolver.setBinding(triple.getO());
                    classResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    classResolver.resolve();
                } else {
                    instanceResolver.setBinding(triple.getO());
                    instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    instanceResolver.resolve();
                }

                if (isResolved(triple)) {
                    unresolvedTriples.remove(i);
                }

                if (isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void resolveAll(List<TriplePattern.BoundTriple> unresolvedTriples) {
        logger.debug(marker, "Resolving-Step: Resolve all");

        for (int i = unresolvedTriples.size() - 1; i >= 0; i--) {
            TriplePattern.BoundTriple triple = unresolvedTriples.get(i);

            if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getS().getModelType())) {
                instanceResolver.setBinding(triple.getS());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();
            }
            if (!isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE) && resolvable(triple.getP().getModelType())) {
                predicateResolver.setBinding(triple.getP());
                predicateResolver.setContext(DataModelBindingContext.PREDICATE);
                predicateResolver.resolve();
            }
            if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && resolvable(triple.getO().getModelType())) {
                if (triple.getP().getModelType().equals(DataModelType.TYPE)) {
                    classResolver.setBinding(triple.getO());
                    classResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    classResolver.resolve();
                } else {
                    instanceResolver.setBinding(triple.getO());
                    instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    instanceResolver.resolve();
                }
            }

            if (isResolved(triple)) {
                unresolvedTriples.remove(i);
            }
        }
    }















    // SINGLE RESOLVER IMPLEMENTATIONS /////////////////////////////////////////////////////////////////////////////////

    private abstract class SingleResolver {
        protected DataModelBinding binding;
        protected DataModelBindingContext context;
        protected long possibleLimit = 10;
        protected long usedLimit = 1;

        protected abstract Scores findResults();

        protected boolean isURI(String str) {
            return (str.toLowerCase().startsWith("http://") || str.toLowerCase().startsWith("https://"));
        }

        protected InstanceEntity resolveUri(String str) {
            InstanceEntity instance = null;
            if (isURI(str.trim())) {
                instance = entitySearcher.getInstanceEntity(dbId, str.trim());
            }
            return instance;
        }

        protected Scores limitScores(Scores scores, long limit) {
            if (limit < 0) {
                return scores;
            } else {
                return new Scores(scores.stream().limit(limit).collect(Collectors.toList()));
            }
        }

        public void setBinding(DataModelBinding binding) {
            this.binding = binding;
        }

        public void setContext(DataModelBindingContext context) {
            this.context = context;
        }

        public void setPossibleLimit(long possibleLimit) {
            this.possibleLimit = possibleLimit;
        }

        public void setUsedLimit(long usedLimit) {
            this.usedLimit = usedLimit;
        }

        public Set<Score> resolve() {

            if (hasMappings(binding.getPlaceHolder(), context)) {
                logger.debug(marker, "{} in context '{}' was already resolved to {}", binding, context, getMappings(binding.getPlaceHolder(), context));
                return getMappings(binding.getPlaceHolder(), context);
            }

            Set<Score> scoreSet = new LinkedHashSet<>();

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scoreSet.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve {} in context '{}'", binding, context);
                scoreSet.addAll(findResults());
                logger.debug(marker, "Results:\n{}", scoreSet.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }

            Scores scores = new Scores(scoreSet);
            if (scores.size() > 0) {
                logger.debug(marker, "Possible mappings for {} in context '{}': {}", binding, context, limitScores(scores, possibleLimit));
                addPossibleMappings(binding.getPlaceHolder(), context, limitScores(scores, possibleLimit));

                logger.debug(marker, "Used mappings for {} in context '{}': {}", binding, context, limitScores(scores, usedLimit));
                addMappings(binding.getPlaceHolder(), context, limitScores(scores, usedLimit));

                return getMappings(binding.getPlaceHolder(), context);
            } else {
                logger.error(marker, "Could not resolve {}", binding);
                return Collections.emptySet();
            }
        }
    }

    private class InstanceResolver extends SingleResolver {

        @Override
        protected Scores findResults() {
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
            ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(binding.getTerm()));
            ModifiableRankParams rankParams = ParamsBuilder.levenshtein();

            return entitySearcher.instanceSearch(searchParams, searchString, rankParams);
        }
    }

    private class ClassResolver extends SingleResolver {
        private final int LIMIT = 50;

        private Scores rescoreClasses(Scores scores) {
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
                double s = (!noRootIds.contains(score.getRankableView().getId())) ? 1 : 0;
                double newScore = (0.5 * score.getValue()) + (0.5 * s);
                rescored.add(new Score(score.getEntry(), newScore));
            }

            // order
            rescored.sort(true);

            return rescored;
        }

        @Override
        protected Scores findResults() {
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
            ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(binding.getTerm()));
            ModifiableRankParams rankParams = ParamsBuilder.word2vec();

            Scores scores = entitySearcher.classSearch(searchParams, searchString, rankParams);

            // re-score classes
            logger.debug(marker, "Rescore classes");
            scores = rescoreClasses(limitScores(scores, LIMIT)); // Limit because otherwise, the SPARQL-Query gets too long -> StackOverflow
            return scores;
        }
    }

    private class PredicateResolver extends SingleResolver {

        @Override
        protected Scores findResults() {
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
            ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(binding.getTerm()));
            ModifiableRankParams rankParams = ParamsBuilder.word2vec();

            return entitySearcher.propertySearch(searchParams, searchString, rankParams);
        }
    }

    private class PivotedPredicateResolver extends SingleResolver {
        private static final int RANGE = 2;

        private List<InstanceEntity> pivots;

        public void setPivots(List<InstanceEntity> pivots) {
            this.pivots = pivots;
        }

        @Override
        protected Scores findResults() {
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
            String rankString = binding.getTerm();
            ModifiableRankParams rankParams = ParamsBuilder.word2vec().threshold(Threshold.min(0.3d)); //TODO magic number

            Scores scores = new Scores();

            // for all pivots
            for (InstanceEntity pivot : pivots) {
                scores.addAll(entitySearcher.pivotedPropertySearch(pivot, searchParams, RANGE, rankString, rankParams));
            }

            return scores;
        }
    }
}
