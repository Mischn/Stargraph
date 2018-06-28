package net.stargraph.server;

/*-
 * ==========================License-Start=============================
 * stargraph-server
 * --------------------------------------------------------------------
 * Copyright (C) 2017 Lambda^3
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ==========================License-End===============================
 */

import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.Document;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.rest.DocumentEntry;
import net.stargraph.rest.EntityEntry;
import net.stargraph.rest.FetchResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class FetchResourceImpl implements FetchResource {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Marker marker = MarkerFactory.getMarker("server");

    private Stargraph stargraph;
    private EntitySearcher entitySearcher;

    FetchResourceImpl(Stargraph stargraph) {
        this.stargraph = Objects.requireNonNull(stargraph);
        this.entitySearcher = stargraph.getEntitySearcher();
    }

    @Override
    public Response getInstance(String dbId, String id) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        InstanceEntity instanceEntity = entitySearcher.getInstanceEntity(dbId, id);
        EntityEntry entityEntry = EntityEntryCreator.createLabeledEntityEntry(instanceEntity, dbId, namespace);

        return Response.status(Response.Status.OK).entity(entityEntry).build();
    }

    @Override
    public Response getProperty(String dbId, String id) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        PropertyEntity propertyEntity = entitySearcher.getPropertyEntity(dbId, id);
        EntityEntry entityEntry = EntityEntryCreator.createPropertyEntityEntry(propertyEntity, dbId, namespace);

        return Response.status(Response.Status.OK).entity(entityEntry).build();
    }

    @Override
    public Response getDocument(String dbId, String id) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        Document document = entitySearcher.getDocument(dbId, id);
        DocumentEntry documentEntry = EntityEntryCreator.createDocumentEntry(document, dbId, namespace);

        return Response.status(Response.Status.OK).entity(documentEntry).build();
    }

    @Override
    public Response getDocuments(String dbId, String entityId, List<String> docTypes) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        List<String> dTypes;
        if (docTypes.size() == 1 && docTypes.get(0).equals("null")) {
            dTypes = null;
        } else {
            dTypes = new ArrayList<>(docTypes);
        }

        List<Document> documents = entitySearcher.getDocumentsForResourceEntity(dbId, entityId, dTypes);
        List<DocumentEntry> documentEntries = EntityEntryCreator.createDocumentEntries(documents, dbId, namespace);

        return Response.status(Response.Status.OK).entity(documentEntries).build();
    }
}
