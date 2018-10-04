package net.stargraph.rest;

public class FactEntry {
    private EntityEntry subject;
    private EntityEntry predicate;
    private EntityEntry object;

    public FactEntry(EntityEntry subject, EntityEntry predicate, EntityEntry object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public EntityEntry getSubject() {
        return subject;
    }

    public EntityEntry getPredicate() {
        return predicate;
    }

    public EntityEntry getObject() {
        return object;
    }
}
