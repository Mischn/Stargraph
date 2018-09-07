package net.stargraph.core.query;

import net.stargraph.core.Namespace;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.query.nli.DataModelType;
import net.stargraph.core.query.nli.TriplePattern;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Resolver {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected Marker marker = MarkerFactory.getMarker("query");

    private static final long MAPPING_CLASS_LIMIT = 3;
    private static final long MAPPING_PREDICATE_LIMIT = 6;

    protected EntitySearcher entitySearcher;
    protected String dbId;
    protected Namespace namespace;
    protected Map<DataModelBinding, List<Score>> mappings;

    public Resolver(EntitySearcher entitySearcher, Namespace namespace, String dbId) {
        this.entitySearcher = entitySearcher;
        this.dbId = dbId;
        this.namespace = namespace;
        this.mappings = new ConcurrentHashMap<>();
    }

    public void reset() {
        this.mappings.clear();
    }

    // MAPPINGS

    private void addMappings(DataModelBinding binding, List<Score> scores) {
        // Expanding the Namespace for all entities
        List<Score> expanded = new Scores(scores.stream().map(s -> new Score(namespace.expand(s.getEntry()), s.getValue())).collect(Collectors.toList()));
        mappings.computeIfAbsent(binding, (b) -> new Scores()).addAll(expanded);
    }

    public boolean hasMappings(DataModelBinding dataModelBinding) {
        return mappings.containsKey(dataModelBinding) && mappings.get(dataModelBinding).size() > 0;
    }

    public List<Score> getMappings(DataModelBinding binding) {
        if (hasMappings(binding)) {
            return mappings.get(binding);
        }
        return Collections.emptyList();
    }

    public Map<DataModelBinding, List<Score>> getMappings() {
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

        if (triple.getP().getModelType() != DataModelType.TYPE) {
            // Probably is: 'I (C|P) V' or 'V (C|P) I'
            logger.debug(marker, "Assume 'I (C|P) V' or 'V (C|P) I' pattern");

            boolean subjPivot = true;
            InstanceEntity pivot = resolvePivot(triple.getS());
            if (pivot == null) {
                subjPivot = false;
                pivot = resolvePivot(triple.getO());
            }

            resolvePredicate(pivot, !subjPivot, subjPivot, triple.getP());
        }
        else {
            // Probably is: 'V T C' or 'C T V'
            logger.debug(marker, "Assume 'V T C' or 'C T V' pattern");

            DataModelBinding binding = triple.getS().getModelType() == DataModelType.VARIABLE ? triple.getO() : triple.getS();
            resolveClass(binding);
        }
    }



    public void resolveClass(DataModelBinding binding) {
        if (hasMappings(binding)) {
            logger.debug(marker, "Class was already resolved to {}", getMappings(binding));
            return;
        }

        if (binding.getModelType() == DataModelType.CLASS) {
            final int PRE_LIMIT = 50;
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve class, searching for term '{}'", binding.getTerm());
                scores = searchClass(binding);
                logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));

                // re-rank classes
                logger.debug(marker, "Rescore classes");
                scores = rescoreClasses(new Scores(scores.stream().limit(PRE_LIMIT).collect(Collectors.toList()))); // Limit, because otherwise, the SPARQL-Query gets too long -> StackOverflow
                logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }

            if (scores.size() > 0) {
                logger.debug(marker, "Map {} to binding {}", limitScores(scores, MAPPING_CLASS_LIMIT), binding);
                addMappings(binding, limitScores(scores, MAPPING_CLASS_LIMIT));
            } else {
                logger.error(marker, "Could not resolve class for {}", binding);
            }
        }
    }

    public Scores searchClass(DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(binding.getTerm()));
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.classSearch(searchParams, searchString, rankParams);
    }




    public void resolvePredicate(InstanceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding) {
        if (hasMappings(binding)) {
            logger.debug(marker, "Predicate was already resolved to {}", getMappings(binding));
            return;
        }

        if (binding.getModelType() == DataModelType.CLASS || binding.getModelType() == DataModelType.PROPERTY) {
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve predicate for pivot {}, searching for term '{}'", pivot, binding.getTerm());
                scores = searchPredicate(pivot, incomingEdges, outgoingEdges, binding);
                logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }

            if (scores.size() > 0) {
                logger.debug(marker, "Map {} to binding {}", limitScores(scores, MAPPING_PREDICATE_LIMIT), binding);
                addMappings(binding, limitScores(scores, MAPPING_PREDICATE_LIMIT));
            } else {
                logger.error(marker, "Could not resolve predicate for {}", binding);
            }
        }
    }

    public Scores searchPredicate(InstanceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        String rankString = binding.getTerm();
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.pivotedPropertySearch(pivot, searchParams, rankString, rankParams, 1, false);
    }




    public InstanceEntity resolvePivot(DataModelBinding binding) {
        if (hasMappings(binding)) {
            logger.debug(marker, "Pivot was already resolved to {}", getMappings(binding));
            return (InstanceEntity)getMappings(binding).get(0).getEntry();
        }

        if (binding.getModelType() == DataModelType.INSTANCE) {
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve pivot, searching for term '{}'", binding.getTerm());
                scores = searchPivot(binding);
                logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }

            if (scores.size() > 0) {
                logger.debug(marker, "Map {} to binding {}", scores.get(0), binding);
                InstanceEntity instance = (InstanceEntity) scores.get(0).getEntry();
                addMappings(binding, Collections.singletonList(scores.get(0)));

                return instance;
            } else {
                logger.error(marker, "Could not resolve pivot for {}", binding);
            }
        }
        return null;
    }

    public Scores searchPivot(DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(binding.getTerm()));
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.instanceSearch(searchParams, searchString, rankParams);
    }





    public void resolveInstance(DataModelBinding binding, long limit) {
        if (hasMappings(binding)) {
            logger.debug(marker, "Instance was already resolved to {}", getMappings(binding));
            return;
        }

        if (binding.getModelType() == DataModelType.INSTANCE) {
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve instance, searching for term '{}'", binding.getTerm());
                scores = searchInstance(binding.getTerm());
                logger.debug(marker, "Results:\n{}", scores.stream().map(s -> s.toString()).collect(Collectors.joining("\n")));
            }

            if (scores.size() > 0) {
                logger.debug(marker, "Map {} to binding {}", limitScores(scores, limit), binding);
                addMappings(binding, limitScores(scores, limit));
            } else {
                logger.error(marker, "Could not resolve instance for {}", binding);
            }
        }
    }

    public Scores searchInstance(String instanceTerm) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(instanceTerm));
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.instanceSearch(searchParams, searchString, rankParams);
    }
}
