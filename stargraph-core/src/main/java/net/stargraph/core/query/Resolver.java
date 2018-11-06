package net.stargraph.core.query;

import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.impl.jena.JenaGraphSearcher;
import net.stargraph.core.impl.jena.JenaQueryHolder;
import net.stargraph.core.impl.jena.JenaSPARQLQuery;
import net.stargraph.core.query.nli.*;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.BuiltInModel;
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
    protected Marker marker = MarkerFactory.getMarker("resolver");

    protected Stargraph stargraph;
    protected EntitySearcher entitySearcher;
    protected String dbId;
    protected Namespace namespace;
    protected Map<String, Map<DataModelBindingContext, Set<Score>>> possibleMappings; // maps placeholder & context to a list of scored entities
    protected Map<String, Map<DataModelBindingContext, Set<Score>>> mappings; // maps placeholder & context to a list of scored entities

    // individual resolvers
    protected final InstanceResolver instanceResolver;
    protected final ClassResolver classResolver;
    protected final PredicateResolver predicateResolver;
    protected final PivotedPredicateResolver pivotedPredicateResolver;
    protected final VariableResolver variableResolver;

    public Resolver(Stargraph stargraph, String dbId) {
        this.stargraph = stargraph;
        this.dbId = dbId;
        this.entitySearcher = stargraph.getEntitySearcher();
        this.namespace = stargraph.getKBCore(dbId).getNamespace();
        this.possibleMappings = new ConcurrentHashMap<>();
        this.mappings = new ConcurrentHashMap<>();

        // resolvers
        this.instanceResolver = new InstanceResolver();
        this.classResolver = new ClassResolver();
        this.predicateResolver = new PredicateResolver();
        this.pivotedPredicateResolver = new PivotedPredicateResolver();
        this.variableResolver = new VariableResolver();
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

    private boolean shouldBeResolved(DataModelType modelType) {
        return !Arrays.asList(DataModelType.VARIABLE, DataModelType.TYPE, DataModelType.EQUALS).contains(modelType);
    }

    private boolean isResolved(TriplePattern.BoundTriple triple) {
        if (shouldBeResolved(triple.getS().getModelType()) && !isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
            return false;
        }
        if (shouldBeResolved(triple.getP().getModelType()) && !isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE)) {
            return false;
        }
        if (shouldBeResolved(triple.getO().getModelType()) && !isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
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
        logger.debug(marker, "RESOLVE TRIPLES {}", triples);

        // a best-effort heuristic in which order to resolve the triples
        boolean loop = true;
        while(loop) {
            boolean loop1 = true;
            while (loop1) {
                boolean loop2 = true;
                while (loop2) {
                    boolean loop3 = true;
                    while (loop3) {
                        loop3 = resolveNextDependant(triples);
                    }
                    loop2 = resolveNextInstance(triples);
                }
                loop1 = resolveNextSO(triples);
            }
            loop = resolveAVariable(triples);
        }
        resolveAll(triples);
    }

    private boolean resolveNextDependant(List<TriplePattern.BoundTriple> triples) {
        logger.debug(marker, "# Resolving-Step: Try resolve a dependant");

        for (TriplePattern.BoundTriple triple : triples) {
            if (isResolved(triple)) {
                continue;
            }

            // Resolve predicate:
            // ???   [TO_BE_RESOLVED]   [resolved]
            // [resolved]   [TO_BE_RESOLVED]   ???
            if (!isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE) && shouldBeResolved(triple.getP().getModelType())) {
                if (isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) || isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {

                    boolean sPivots = isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    DataModelBinding pivotBinding = sPivots ? triple.getS() : triple.getO();

                    List<InstanceEntity> pivots = getMappings(pivotBinding.getPlaceHolder(), DataModelBindingContext.NON_PREDICATE).stream()
                            .filter(s -> s.getEntry() instanceof InstanceEntity)
                            .map(s -> (InstanceEntity) s.getEntry())
                            .collect(Collectors.toList());

                    pivotedPredicateResolver.setTriple(triple);
                    pivotedPredicateResolver.setBinding(triple.getP());
                    pivotedPredicateResolver.setContext(DataModelBindingContext.PREDICATE);
                    pivotedPredicateResolver.setSubjectPivots(sPivots);
                    pivotedPredicateResolver.setPivots(pivots);
                    pivotedPredicateResolver.resolve();

                    if (isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE)) {
                        return true;
                    }
                }
            }

            // Resolve target:
            // [TO_BE_RESOLVED]   ?VAR   [resolved]
            // [resolved]   ?VAR   [TO_BE_RESOLVED]
            //TODO implement DNA search



            // Resolve class
            // [resolved]   TYPE   [TO_BE_RESOLVED]



            // Resolve class member
            // [TO_BE_RESOLVED]   TYPE   [resolved]



            // Resolve equality
            // [resolved]   EQUALS   [TO_BE_RESOLVED]
            // [TO_BE_RESOLVED]   EQUALS   [resolved]
            if (triple.getP().getModelType().equals(DataModelType.EQUALS)) {
                if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getS().getModelType()) && isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {

                    // just copy mappings from o to s
                    Set<Score> mappings = getMappings(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    Set<Score> possibleMappings = getPossibleMappings(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
                    addMappings(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE, new ArrayList<>(mappings));
                    addPossibleMappings(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE, new ArrayList<>(mappings));
                }
                if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getO().getModelType()) && isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {

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

    private TriplePattern.BoundTriple getVarResolverTriple(List<TriplePattern.BoundTriple> triples, DataModelBinding varBinding) {
        for (TriplePattern.BoundTriple triple : triples) {

            // must have two resolved elements and one unresolved variable
            boolean sResolved = isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
            boolean pResolved = isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE);
            boolean oResolved = isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE);
            long nrResolved = Arrays.asList(sResolved, pResolved, oResolved).stream().filter(b -> b).count();

            if (nrResolved == 2 && triple.getS().getModelType().equals(DataModelType.VARIABLE) && !sResolved) {
                return triple;
            }
            if (nrResolved == 2 && triple.getP().getModelType().equals(DataModelType.VARIABLE) && !pResolved) {
                return triple;
            }
            if (nrResolved == 2 && triple.getO().getModelType().equals(DataModelType.VARIABLE) && !oResolved) {
                return triple;
            }
        }
        return null;
    }

    private boolean resolveAVariable(List<TriplePattern.BoundTriple> triples) {
        logger.debug(marker, "# Resolving-Step: Try resolve a VARIABLE");


        for (TriplePattern.BoundTriple triple : triples) {
            if (isResolved(triple)) {
                continue;
            }

            DataModelBinding binding = null;
            TriplePattern.BoundTriple resolverTriple = null;
            if (triple.getS().getModelType().equals(DataModelType.VARIABLE) && !isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                resolverTriple = getVarResolverTriple(triples, triple.getS());
                binding = triple.getS();
            }

            if (resolverTriple == null) {
                if (triple.getP().getModelType().equals(DataModelType.VARIABLE) && !isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.PREDICATE)) {
                    resolverTriple = getVarResolverTriple(triples, triple.getP());
                    binding = triple.getP();
                }
            }

            if (resolverTriple == null) {
                if (triple.getO().getModelType().equals(DataModelType.VARIABLE) && !isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.PREDICATE)) {
                    resolverTriple = getVarResolverTriple(triples, triple.getO());
                    binding = triple.getO();
                }
            }

            if (resolverTriple != null) {
                variableResolver.setTriple(resolverTriple);
                variableResolver.setBinding(binding);
                variableResolver.resolve();

                if (isResolved(binding.getPlaceHolder(), variableResolver.getContext())) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean resolveNextInstance(List<TriplePattern.BoundTriple> triples) {
        logger.debug(marker, "# Resolving-Step: Try resolve an INSTANCE");

        for (TriplePattern.BoundTriple triple : triples) {
            if (isResolved(triple)) {
                continue;
            }

            if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getS().getModelType()) && triple.getS().getModelType().equals(DataModelType.INSTANCE)) {
                instanceResolver.setTriple(triple);
                instanceResolver.setBinding(triple.getS());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();

                if (isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }

            if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getO().getModelType()) && triple.getO().getModelType().equals(DataModelType.INSTANCE)) {
                instanceResolver.setTriple(triple);
                instanceResolver.setBinding(triple.getO());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();

                if (isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean resolveNextSO(List<TriplePattern.BoundTriple> triples) {
        logger.debug(marker, "# Resolving-Step: Try resolve a subject or object");

        // subjects
        for (TriplePattern.BoundTriple triple : triples) {
            if (isResolved(triple)) {
                continue;
            }

            if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getS().getModelType())) {
                instanceResolver.setTriple(triple);
                instanceResolver.setBinding(triple.getS());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();

                if (isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }
        }

        // objects
        for (TriplePattern.BoundTriple triple : triples) {
            if (isResolved(triple)) {
                continue;
            }

            if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getO().getModelType())) {
                if (triple.getP().getModelType().equals(DataModelType.TYPE)) {
                    classResolver.setTriple(triple);
                    classResolver.setBinding(triple.getO());
                    classResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    classResolver.resolve();
                } else {
                    instanceResolver.setTriple(triple);
                    instanceResolver.setBinding(triple.getO());
                    instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    instanceResolver.resolve();
                }

                if (isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void resolveAll(List<TriplePattern.BoundTriple> triples) {
        logger.debug(marker, "# Resolving-Step: Resolve all");

        for (TriplePattern.BoundTriple triple : triples) {
            if (isResolved(triple)) {
                continue;
            }

            if (!isResolved(triple.getS().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getS().getModelType())) {
                instanceResolver.setTriple(triple);
                instanceResolver.setBinding(triple.getS());
                instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                instanceResolver.resolve();
            }
            if (!isResolved(triple.getP().getPlaceHolder(), DataModelBindingContext.PREDICATE) && shouldBeResolved(triple.getP().getModelType())) {
                predicateResolver.setTriple(triple);
                predicateResolver.setBinding(triple.getP());
                predicateResolver.setContext(DataModelBindingContext.PREDICATE);
                predicateResolver.resolve();
            }
            if (!isResolved(triple.getO().getPlaceHolder(), DataModelBindingContext.NON_PREDICATE) && shouldBeResolved(triple.getO().getModelType())) {
                if (triple.getP().getModelType().equals(DataModelType.TYPE)) {
                    classResolver.setTriple(triple);
                    classResolver.setBinding(triple.getO());
                    classResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    classResolver.resolve();
                } else {
                    instanceResolver.setTriple(triple);
                    instanceResolver.setBinding(triple.getO());
                    instanceResolver.setContext(DataModelBindingContext.NON_PREDICATE);
                    instanceResolver.resolve();
                }
            }
        }
    }















    // SINGLE RESOLVER IMPLEMENTATIONS /////////////////////////////////////////////////////////////////////////////////

    private abstract class SingleResolver {
        protected Logger logger = LoggerFactory.getLogger(getClass());
        protected Marker marker = MarkerFactory.getMarker("single-resolver");

        protected TriplePattern.BoundTriple triple; // the triple in which binding occurs (optional)
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

        public void setTriple(TriplePattern.BoundTriple triple) {
            this.triple = triple;
        }

        public void setBinding(DataModelBinding binding) {
            this.binding = binding;
        }

        public void setContext(DataModelBindingContext context) {
            this.context = context;
        }

        public DataModelBindingContext getContext() {
            return context;
        }

        public void setPossibleLimit(long possibleLimit) {
            this.possibleLimit = possibleLimit;
        }

        public void setUsedLimit(long usedLimit) {
            this.usedLimit = usedLimit;
        }

        public Set<Score> resolve() {
            if (triple != null) {
                logger.debug(marker, "Try to resolve {} from triple {}", binding, triple);
            } else {
                logger.debug(marker, "Try to resolve {}", binding);
            }

            if (hasMappings(binding.getPlaceHolder(), context)) {
                logger.debug(marker, "{} was already resolved to {}", binding, getMappings(binding.getPlaceHolder(), context));
                return getMappings(binding.getPlaceHolder(), context);
            }

            Set<Score> scoreSet = new LinkedHashSet<>();

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scoreSet.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve {}", binding);
                scoreSet.addAll(findResults());
                logger.debug(marker, "Results:\n{}", scoreSet.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }

            Scores scores = new Scores(scoreSet);
            if (scores.size() > 0) {
                logger.debug(marker, "Possible mappings for {}: {}", binding, limitScores(scores, possibleLimit));
                addPossibleMappings(binding.getPlaceHolder(), context, limitScores(scores, possibleLimit));

                logger.debug(marker, "Used mappings for {}: {}", binding, limitScores(scores, usedLimit));
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

        private boolean subjectPivots;
        private List<InstanceEntity> pivots;

        public void setSubjectPivots(boolean subjectPivots) {
            this.subjectPivots = subjectPivots;
        }

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
                scores.addAll(entitySearcher.pivotedPropertySearch(pivot, searchParams, subjectPivots, RANGE, rankString, rankParams));
            }

            return scores;
        }
    }

    private class VariableResolver extends SingleResolver {
        private JenaGraphSearcher jenaGraphSearcher;

        public VariableResolver() {
            this.jenaGraphSearcher = (JenaGraphSearcher)stargraph.getKBCore(dbId).getGraphSearcher();
        }

        @Override
        protected Scores findResults() {
            boolean asPredicate = triple.getP().getPlaceHolder().equals(binding.getPlaceHolder());

            Map<String, DataModelBinding> bindings = new HashMap<>();
            bindings.put(triple.getS().getPlaceHolder(), triple.getS());
            bindings.put(triple.getP().getPlaceHolder(), triple.getP());
            bindings.put(triple.getO().getPlaceHolder(), triple.getO());

            QueryPlan queryPlan = new QueryPlan();
            queryPlan.add(triple.getTriplePattern());
            SPARQLQueryBuilder sparqlQueryBuilder = new SPARQLQueryBuilder(stargraph, dbId, QueryType.SELECT, queryPlan, bindings, getMappings());
            String sparqlQueryStr = sparqlQueryBuilder.build();

            BuiltInModel model;
            if (asPredicate) {
                sparqlQueryStr = sparqlQueryStr.replace(binding.getPlaceHolder(), "?p");
                model = BuiltInModel.PROPERTY;
                context = DataModelBindingContext.PREDICATE;
            } else {
                sparqlQueryStr = sparqlQueryStr.replace(binding.getPlaceHolder(), "?e");
                model = BuiltInModel.ENTITY;
                context = DataModelBindingContext.NON_PREDICATE;
            }

            return jenaGraphSearcher.search(new JenaQueryHolder(new JenaSPARQLQuery(sparqlQueryStr), ModifiableSearchParams.create(dbId).model(model)));
        }
    }
}
