package net.stargraph.core.model;

import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.InstanceEntity;

import java.util.*;
import java.util.stream.Collectors;

public class InstanceEntityImpl extends InstanceEntity {
    private EntitySearcher entitySearcher;
    private String dbId;
    private boolean lookedUp = false;

    private String value; // may be null
    private Boolean isClass; // may be null
    private Collection<String> otherValues; // may be null

    public InstanceEntityImpl(String id) {
        super(id);
    }

    public InstanceEntityImpl(EntitySearcher entitySearcher, String dbId, String id) {
        super(id);
        this.entitySearcher = Objects.requireNonNull(entitySearcher);
        this.dbId = Objects.requireNonNull(dbId);
    }

    public InstanceEntityImpl(String id, String value, boolean isClass, Collection<String> otherValues) {
        super(id);
        this.value = Objects.requireNonNull(value);
        this.isClass = Objects.requireNonNull(isClass);
        this.otherValues = Objects.requireNonNull(otherValues);
    }

    private void set(InstanceEntity entity) {
        this.value = (this.value == null)? entity.getValue(): this.value;
        this.isClass = (this.isClass == null)? entity.isClass(): this.isClass;
        this.otherValues = (this.otherValues == null)? entity.getOtherValues(): this.otherValues;
    }

    public static void bulkLookup(EntitySearcher entitySearcher, String dbId, List<InstanceEntityImpl> instanceEntities) {
        if (entitySearcher == null || dbId == null) {
            return;
        }

        List<InstanceEntityImpl> lookupEntities = instanceEntities.stream().filter(e -> !e.lookedUp).collect(Collectors.toList());
        List<InstanceEntity> lookedupEntities = entitySearcher.getInstanceEntities(dbId, lookupEntities.stream().map(e -> e.getId()).collect(Collectors.toList()));
        for (InstanceEntity entity : lookedupEntities) {
            lookupEntities.stream().filter(e -> e.getId().equals(entity.getId())).forEach(e -> e.set(entity));
        }
    }

    private void lookup() {
        if (entitySearcher == null || dbId == null) {
            logger.error(marker, "Lookup was requested, but entitySearcher or dbId were null");
            return;
        }

        logger.debug(marker, "Lookup {} '{}'", getClass().getSimpleName(), id);
        InstanceEntity entity = entitySearcher.getInstanceEntity(dbId, id);
        if (entity != null) {
            set(entity);
            lookedUp = true;
        } else {
            logger.error(marker, "Could not lookup Instance with id: {}", id);
        }
    }

    public InstanceEntityImpl clone(String id) {
        InstanceEntityImpl cloned = new InstanceEntityImpl(id);
        cloned.entitySearcher = entitySearcher;
        cloned.dbId = dbId;
        cloned.value = value;
        cloned.isClass = isClass;
        cloned.otherValues = otherValues;
        return cloned;
    }

    public String getDbId() {
        return dbId;
    }

    @Override
    public boolean isClass() {
        if (isClass == null) {
            lookup();
        }
        return isClass;
    }

    @Override
    public boolean isComplex() {
        return getValue().split("\\s+").length > 1;
    }

    @Override
    public Collection<String> getOtherValues() {
        if (otherValues == null) {
            lookup();
        }
        return otherValues;
    }

    @Override
    public String getValue() {
        if (value == null) {
            lookup();
        }
        return value;
    }

    @Override
    public List<List<String>> getRankableValues() {
        List<List<String>> res = new ArrayList<>();
        if (getValue() != null) {
            res.add(Arrays.asList(getValue()));
        }
        if (getOtherValues() != null) {
            res.addAll(getOtherValues().stream().map(v -> Arrays.asList(v)).collect(Collectors.toList()));
        }
        return res;
    }

//    @Override
//    public String toString() {
//        return "InstanceEntityImpl{" +
//                "id='" + id + '\'' +
//                ", value=" + ((value != null)? value: "???") +
//                ", isClass=" + ((isClass != null)? isClass: "???") +
//                ", otherValues=" + ((otherValues != null)? otherValues: "???") +
//                '}';
//    }
}
