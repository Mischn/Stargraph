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
import net.stargraph.core.ner.LinkedNamedEntity;
import net.stargraph.core.ner.NER;
import net.stargraph.query.Language;
import net.stargraph.rest.EntityEntry;
import net.stargraph.rest.LinkedEntityEntry;
import net.stargraph.rest.NERResource;
import net.stargraph.rest.NERResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NERResourceImpl implements NERResource {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("server");
    private Stargraph core;

    public NERResourceImpl(Stargraph core) {
        this.core = Objects.requireNonNull(core);
    }

    @Override
    public Response searchAndLink(String dbId, String language, UserText userText) {
        Namespace namespace = core.getKBCore(dbId).getNamespace();
        Language lang = Language.valueOf(language.toUpperCase());
        NER ner = core.createNER(lang, dbId);

        List<LinkedNamedEntity> linkedNamedEntities = ner.searchAndLink(userText.text);

        // convert to LinkedEntityEntries (expand IDs by convention)
        List<LinkedEntityEntry> linkedEntityEntries = new ArrayList<>();
        for (LinkedNamedEntity linkedEntity : linkedNamedEntities) {
            EntityEntry entity = (linkedEntity.isLinked())? EntityEntryCreator.createEntityEntry(linkedEntity.getEntity(), dbId, namespace) : null;
            double score = (linkedEntity.isLinked())? linkedEntity.getScore() : -1;
            linkedEntityEntries.add(new LinkedEntityEntry(linkedEntity.getValue(), linkedEntity.getCat(), linkedEntity.getStart(), linkedEntity.getEnd(), entity, score));
        }

        return Response.ok().entity(new NERResponse(userText.text, linkedEntityEntries)).build();
    }
}
