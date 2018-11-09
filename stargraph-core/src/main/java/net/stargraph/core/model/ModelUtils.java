package net.stargraph.core.model;

import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.Entity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.PropertyPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelUtils {
    private static Logger logger = LoggerFactory.getLogger(ModelUtils.class);
    private static Marker marker = MarkerFactory.getMarker("utils");

    private static Set<PropertyEntity> getProperties(List<PropertyPath> propertyPaths) {
        Set<PropertyEntity> res = new HashSet<>();
        propertyPaths.stream().forEach(pp -> res.addAll(pp.getProperties()));
        return res;
    }
    
    // for performance (prefer to lookup bulks instead of single ids)
    public static void bulkLookup(EntitySearcher entitySearcher, List<Entity> entities) {

        // Instances
        if (entities.size() > 0
                && entities.stream().allMatch(s -> (s instanceof InstanceEntityImpl) && ((InstanceEntityImpl)s).getDbId().equals(((InstanceEntityImpl)entities.get(0)).getDbId()))) {
            logger.debug(marker, "Bulk-Lookup of instances");
            String dbId = ((InstanceEntityImpl)entities.get(0)).getDbId();
            InstanceEntityImpl.bulkLookup(entitySearcher, dbId, entities.stream().map(s -> ((InstanceEntityImpl)s)).collect(Collectors.toList()));
        }

        // Properties
        if (entities.size() > 0
                && entities.stream().allMatch(s -> (s instanceof PropertyEntityImpl) && ((PropertyEntityImpl)s).getDbId().equals(((PropertyEntityImpl)entities.get(0)).getDbId()))) {
            logger.debug(marker, "Bulk-Lookup of properties");
            String dbId = ((PropertyEntityImpl)entities.get(0)).getDbId();
            PropertyEntityImpl.bulkLookup(entitySearcher, dbId, entities.stream().map(s -> ((PropertyEntityImpl)s)).collect(Collectors.toList()));
        }

        // Property-Paths
        if (entities.size() > 0
                && entities.stream().allMatch(s -> (s instanceof PropertyPath))
                && (getProperties(entities.stream().map(s -> (PropertyPath)s).collect(Collectors.toList())).stream().allMatch(p -> p instanceof PropertyEntityImpl))) {
            logger.debug(marker, "Bulk-Lookup of property paths");

            // recursive call
            List<Entity> properties = getProperties(entities.stream().map(s -> (PropertyPath)s).collect(Collectors.toList())).stream().map(p -> (PropertyEntityImpl)p).collect(Collectors.toList());
            ModelUtils.bulkLookup(entitySearcher, properties);
        }
    };
}
