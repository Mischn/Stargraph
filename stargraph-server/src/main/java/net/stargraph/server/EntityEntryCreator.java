package net.stargraph.server;

import net.stargraph.core.Namespace;
import net.stargraph.model.Document;
import net.stargraph.model.LabeledEntity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.ValueEntity;
import net.stargraph.rank.Score;
import net.stargraph.rest.DocumentEntry;
import net.stargraph.rest.EntityEntry;

import java.util.List;
import java.util.stream.Collectors;

public class EntityEntryCreator {

    // ATTENTION: EXPAND ALL URIS BY CONVENTION

    // EntityEntries
    public static List<EntityEntry> createPropertyEntityEntries(List<PropertyEntity> propertyEntities, String dbId, Namespace namespace) {
        return propertyEntities.stream().map(e -> createPropertyEntityEntry(e, dbId, namespace)).collect(Collectors.toList());
    }
    public static EntityEntry createPropertyEntityEntry(PropertyEntity propertyEntity, String dbId, Namespace namespace) {
        return createScoredEntityEntry(new Score(propertyEntity, 1), dbId, namespace);
    }
    public static List<EntityEntry> createLabeledEntityEntries(List<LabeledEntity> labeledEntities, String dbId, Namespace namespace) {
        return labeledEntities.stream().map(e -> createLabeledEntityEntry(e, dbId, namespace)).collect(Collectors.toList());
    }
    public static EntityEntry createLabeledEntityEntry(LabeledEntity labeledEntity, String dbId, Namespace namespace) {
        return createScoredEntityEntry(new Score(labeledEntity, 1), dbId, namespace);
    }
    public static List<EntityEntry> createScoredEntityEntries(List<Score> entityScores, String dbId, Namespace namespace) {
        return entityScores.stream().map(e -> createScoredEntityEntry(e, dbId, namespace)).collect(Collectors.toList());
    }
    public static EntityEntry createScoredEntityEntry(Score entityScore, String dbId, Namespace namespace) {
        return new EntityEntry(
                dbId,
                (entityScore.getEntry() instanceof ValueEntity)? EntityEntry.EntityType.LITERAL: EntityEntry.EntityType.RESOURCE,
                namespace.expandURI(entityScore.getRankableView().getId()),
                entityScore.getRankableView().getValue(),
                entityScore.getValue()
        );
    }


    // DocumentEntries
    public static List<DocumentEntry> createDocumentEntries(List<Document> documents, String dbId, Namespace namespace) {
        return documents.stream().map(e -> createDocumentEntry(e, dbId, namespace)).collect(Collectors.toList());
    }
    public static DocumentEntry createDocumentEntry(Document document, String dbId, Namespace namespace) {
        return createScoredDocumentEntry(new Score(document, 1), dbId, namespace);
    }
    public static List<DocumentEntry> createScoredDocumentEntries(List<Score> documentScores, String dbId, Namespace namespace) {
        return documentScores.stream().map(e -> createScoredDocumentEntry(e, dbId, namespace)).collect(Collectors.toList());
    }
    public static DocumentEntry createScoredDocumentEntry(Score documentScore, String dbId, Namespace namespace) {
        Document document = (Document) documentScore.getEntry();
        return new DocumentEntry(
                dbId,
                document.getType(),
                document.getEntity(),
                namespace.expandURI(document.getId()),
                document.getTitle(),
                document.getSummary(),
                document.getText(),
                createLabeledEntityEntries(document.getEntities(), dbId, namespace),
                documentScore.getValue()
        );
    }
}
