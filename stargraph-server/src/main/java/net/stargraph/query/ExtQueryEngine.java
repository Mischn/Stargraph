package net.stargraph.query;

import net.stargraph.core.Stargraph;
import net.stargraph.core.query.QueryEngine;
import net.stargraph.core.query.QueryResponse;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.PropertyPath;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Allows to define better-mappings for NLI queries
 */
public class ExtQueryEngine extends QueryEngine {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("query");

    private Map<String, List<String>> betterMappings;

    public ExtQueryEngine(String dbId, Stargraph stargraph) {
        super(dbId, stargraph);
        this.betterMappings = new HashMap<>();
    }

    public void setBetterMappings(Map<String, List<String>> betterMappings) {
        logger.info(marker, "Defined better mappings {}", betterMappings);
        this.betterMappings = betterMappings;
    }

    public void clearBetterMappings() {
        betterMappings.clear();
    }

    @Override
    public QueryResponse query(String query) {
        return super.query(query);
    }

    // assumes the id to consist of ' '-joined property-ids
    private PropertyPath getPropertyPath(String id) {
        String[] uris = id.split("\\s+");
        List<PropertyEntity> properties = new ArrayList<>();
        for (String uri : uris) {
            PropertyEntity property = entitySearcher.getPropertyEntity(dbId, uri);
            if (property == null) {
                return null;
            }
            properties.add(property);
        }

        return new PropertyPath(properties);
    }

    @Override
    protected Scores searchClass(DataModelBinding binding) {
        if (betterMappings.containsKey(binding.getTerm())) {

            // lookup classes
            Scores scores = new Scores(betterMappings.get(binding.getTerm()).stream()
                    .map(id -> entitySearcher.getInstanceEntity(dbId, id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", binding.getTerm());
                return scores;
            }
        }

        return super.searchClass(binding);
    }

    @Override
    protected Scores searchPredicate(InstanceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding) {
        if (betterMappings.containsKey(binding.getTerm())) {

            // lookup property-paths
            Scores scores = new Scores(betterMappings.get(binding.getTerm()).stream()
                    .map(id -> getPropertyPath(id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", binding.getTerm());
                return scores;
            }
        }

        return super.searchPredicate(pivot, incomingEdges, outgoingEdges, binding);
    }

    @Override
    protected Scores searchPivot(DataModelBinding binding) {
        if (betterMappings.containsKey(binding.getTerm())) {

            // lookup pivots
            Scores scores = new Scores(betterMappings.get(binding.getTerm()).stream()
                    .map(id -> entitySearcher.getInstanceEntity(dbId, id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", binding.getTerm());
                return scores;
            }
        }

        return super.searchPivot(binding);
    }

    @Override
    protected Scores searchInstance(String instanceTerm) {
        if (betterMappings.containsKey(instanceTerm)) {

            // lookup instances
            Scores scores = new Scores(betterMappings.get(instanceTerm).stream()
                    .map(id -> entitySearcher.getInstanceEntity(dbId, id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", instanceTerm);
                return scores;
            }
        }

        return super.searchInstance(instanceTerm);
    }
}
