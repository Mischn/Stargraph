package net.stargraph.core.query;

import net.stargraph.core.Namespace;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.PropertyPath;
import net.stargraph.rank.Score;
import net.stargraph.rank.Scores;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExtResolver extends Resolver {
    private Map<String, List<String>> customMappings;

    public ExtResolver(EntitySearcher entitySearcher, Namespace namespace, String dbId) {
        super(entitySearcher, namespace, dbId);
        this.customMappings = new HashMap<>();
    }

    public void setCustomMappings(Map<String, List<String>> customMappings) {
        logger.info(marker, "Defined custom mappings {}", customMappings);
        this.customMappings = customMappings;
    }

    public void clearCustomMappings() {
        this.customMappings.clear();
    }

    private PropertyPath getPropertyPath(String id) {
        PropertyPath propertyPath = null;

        for (PropertyPath.PropertyParse propertyParse : PropertyPath.parseId(id)) {
            PropertyEntity property = entitySearcher.getPropertyEntity(dbId, propertyParse.propertyId);
            if (property == null) {
                return null;
            }

            if (propertyPath == null) {
                propertyPath = new PropertyPath(property, propertyParse.direction);
            } else {
                propertyPath = propertyPath.extend(property, propertyParse.direction);
            }
        }

        return propertyPath;
    }

    @Override
    public Scores searchClass(DataModelBinding binding) {
        if (customMappings.containsKey(binding.getTerm())) {

            // lookup classes
            Scores scores = new Scores(customMappings.get(binding.getTerm()).stream()
                    .map(id -> entitySearcher.getInstanceEntity(dbId, id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", binding.getTerm());
                return scores;
            } else {
                logger.warn(marker, "Better mappings for '{}' were available ({}), but could not find any instances", binding.getTerm(), customMappings.get(binding.getTerm()));
            }
        }

        return super.searchClass(binding);
    }

    @Override
    public Scores searchPredicate(InstanceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding) {
        if (customMappings.containsKey(binding.getTerm())) {

            // lookup property-paths
            Scores scores = new Scores(customMappings.get(binding.getTerm()).stream()
                    .map(id -> getPropertyPath(id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", binding.getTerm());
                return scores;
            } else {
                logger.warn(marker, "Better mappings for '{}' were available ({}), but could not find any instances", binding.getTerm(), customMappings.get(binding.getTerm()));
            }
        }

        return super.searchPredicate(pivot, incomingEdges, outgoingEdges, binding);
    }

    @Override
    public Scores searchPivot(DataModelBinding binding) {
        if (customMappings.containsKey(binding.getTerm())) {

            // lookup pivots
            Scores scores = new Scores(customMappings.get(binding.getTerm()).stream()
                    .map(id -> entitySearcher.getInstanceEntity(dbId, id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", binding.getTerm());
                return scores;
            } else {
                logger.warn(marker, "Better mappings for '{}' were available ({}), but could not find any instances", binding.getTerm(), customMappings.get(binding.getTerm()));
            }
        }

        return super.searchPivot(binding);
    }

    @Override
    public Scores searchInstance(String instanceTerm) {
        if (customMappings.containsKey(instanceTerm)) {

            // lookup instances
            Scores scores = new Scores(customMappings.get(instanceTerm).stream()
                    .map(id -> entitySearcher.getInstanceEntity(dbId, id))
                    .filter(e -> e != null)
                    .map(e -> new Score(e, 1))
                    .collect(Collectors.toList()));
            if (scores.size() > 0) {
                logger.info(marker, "Used better mappings for '{}'", instanceTerm);
                return scores;
            } else {
                logger.warn(marker, "Better mappings for '{}' were available ({}), but could not find any instances", instanceTerm, customMappings.get(instanceTerm));
            }
        }

        return super.searchInstance(instanceTerm);
    }
}
