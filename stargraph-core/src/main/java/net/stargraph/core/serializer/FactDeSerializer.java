package net.stargraph.core.serializer;

/*-
 * ==========================License-Start=============================
 * stargraph-core
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import net.stargraph.core.model.InstanceEntityImpl;
import net.stargraph.core.model.PropertyEntityImpl;
import net.stargraph.core.model.ValueEntityImpl;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.*;

import java.io.IOException;

class FactDeSerializer extends AbstractDeserializer<Fact> {
    private EntitySearcher entitySearcher;

    FactDeSerializer(EntitySearcher entitySearcher, KBId kbId) {
        super(kbId, Fact.class);
        this.entitySearcher = entitySearcher;
    }

    @Override
    public Fact deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        JsonNode s = node.get("s");
        InstanceEntity subject = new InstanceEntityImpl(entitySearcher, getKbId().getId(), s.get("id").asText());

        JsonNode p = node.get("p");
        PropertyEntity property = new PropertyEntityImpl(entitySearcher, getKbId().getId(), p.get("id").asText());

        JsonNode o = node.get("o");
        NodeEntity object;

        if (o.has("dataType") || o.has("language")) {
            object = new ValueEntityImpl(o.get("id").asText(), o.get("value").asText(), o.get("dataType").asText(null), o.get("language").asText(null));
        } else {
            object = new InstanceEntityImpl(entitySearcher, getKbId().getId(), o.get("id").asText());
        }

        return new Fact(getKbId(), subject, property, object);
    }


}
