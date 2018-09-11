package net.stargraph.core.model;

import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.wordnet.WNTuple;

import java.util.Collection;
import java.util.Objects;

public class PropertyEntityImpl extends PropertyEntity {
    private EntitySearcher entitySearcher;
    private String dbId;

    private String value; // may be null
    private Collection<WNTuple> hypernyms; // may be null
    private Collection<WNTuple> hyponyms; // may be null
    private Collection<WNTuple> synonyms; // may be null

    public PropertyEntityImpl(String id) {
        super(id);
    }

    public PropertyEntityImpl(EntitySearcher entitySearcher, String dbId, String id) {
        super(id);
        this.entitySearcher = Objects.requireNonNull(entitySearcher);
        this.dbId = Objects.requireNonNull(dbId);
    }

    public PropertyEntityImpl(String id, String value, Collection<WNTuple> hypernyms, Collection<WNTuple> hyponyms, Collection<WNTuple> synonyms) {
        super(id);
        this.value = Objects.requireNonNull(value);
        this.hypernyms = Objects.requireNonNull(hypernyms);
        this.hyponyms = Objects.requireNonNull(hyponyms);
        this.synonyms = Objects.requireNonNull(synonyms);
    }

    private void lookup() {
        if (entitySearcher == null || dbId == null) {
            logger.error(marker, "Lookup was requested, but entitySearcher or dbId were null");
            return;
        }

        logger.debug(marker, "Lookup {} '{}'", getClass().getSimpleName(), id);
        PropertyEntity entity = entitySearcher.getPropertyEntity(dbId, id);
        if (entity != null) {
            this.value = (this.value == null)? entity.getValue(): this.value;
            this.hypernyms = (this.hypernyms == null)? entity.getHypernyms(): this.hypernyms;
            this.hyponyms = (this.hyponyms == null)? entity.getHyponyms(): this.hyponyms;
            this.synonyms = (this.synonyms == null)? entity.getSynonyms(): this.synonyms;
        }
    }

    public PropertyEntityImpl clone(String id) {
        PropertyEntityImpl cloned = new PropertyEntityImpl(id);
        cloned.entitySearcher = entitySearcher;
        cloned.dbId = dbId;
        cloned.value = value;
        cloned.hypernyms = hypernyms;
        cloned.hyponyms = hyponyms;
        cloned.synonyms = synonyms;
        return cloned;
    }

    @Override
    public Collection<WNTuple> getHypernyms() {
        if (hypernyms == null) {
            lookup();
        }
        return hypernyms;
    }

    @Override
    public Collection<WNTuple> getHyponyms() {
        if (hyponyms == null) {
            lookup();
        }
        return hyponyms;
    }

    @Override
    public Collection<WNTuple> getSynonyms() {
        if (synonyms == null) {
            lookup();
        }
        return synonyms;
    }

    @Override
    public String getValue() {
        if (value == null) {
            lookup();
        }
        return value;
    }

    @Override
    public String getRankableValue() {
        return getValue(); //TODO include hypernyms/hyponyms/synonyms as well?
    }

//    @Override
//    public String toString() {
//        return "PropertyEntityImpl{" +
//                "id='" + id + '\'' +
//                ", value='" + ((value != null)? value: "???") + '\'' +
//                ", hypernyms=" + ((hypernyms != null)? hypernyms: "???") +
//                ", hyponyms=" + ((hyponyms != null)? hyponyms: "???") +
//                ", synonyms=" + ((synonyms != null)? synonyms: "???") +
//                '}';
//    }
}
