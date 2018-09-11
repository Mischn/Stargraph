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
import net.stargraph.core.model.PropertyEntityImpl;
import net.stargraph.model.KBId;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.wordnet.PosType;
import net.stargraph.model.wordnet.WNTuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PropertyDeserializer extends AbstractDeserializer<PropertyEntity> {

    PropertyDeserializer(KBId kbId) {
        super(kbId, PropertyEntity.class);
    }

    @Override
    public PropertyEntity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String id = node.get("id").asText();
        String value = node.get("value").asText();

        List<WNTuple> hypernyms = new ArrayList<>();
        if (node.has("hypernyms")) {
            for (final JsonNode wnt : node.get("hypernyms")) {
                hypernyms.add(new WNTuple(PosType.valueOf(wnt.get("posType").asText()), wnt.get("word").asText()));
            }
        }

        List<WNTuple> hyponyms = new ArrayList<>();
        if (node.has("hyponyms")) {
            for (final JsonNode wnt : node.get("hyponyms")) {
                hyponyms.add(new WNTuple(PosType.valueOf(wnt.get("posType").asText()), wnt.get("word").asText()));
            }
        }

        List<WNTuple> synonyms = new ArrayList<>();
        if (node.has("synonyms")) {
            for (final JsonNode wnt : node.get("synonyms")) {
                synonyms.add(new WNTuple(PosType.valueOf(wnt.get("posType").asText()), wnt.get("word").asText()));
            }
        }

        return new PropertyEntityImpl(id, value, hypernyms, hyponyms, synonyms);
    }


}
