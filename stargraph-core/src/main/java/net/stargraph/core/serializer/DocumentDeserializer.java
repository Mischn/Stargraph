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
import net.stargraph.model.*;
import net.stargraph.model.date.TimeRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DocumentDeserializer extends AbstractDeserializer<Document> {

    DocumentDeserializer(KBId kbId) {
        super(kbId, PropertyEntity.class);
    }

    private static List<LabeledEntity> deserializeLabeledEntities(JsonNode entitiesNode) {
        List<LabeledEntity> res = new ArrayList<>();

        if (entitiesNode.has("entities")) {
            for (final JsonNode ent : entitiesNode.get("entities")) {
                String entId = ent.get("id").asText();
                String entValue = ent.get("value").asText();

                if (ent.has("language")) {
                    res.add(new ValueEntity(entId, entValue, ent.get("dataType").asText(null), ent.get("language").asText(null)));
                } else {
                    res.add(new InstanceEntity(entId, entValue));
                }
            }
        }

        return res;
    }

    private static List<TimeRange> deserializeTemporals(JsonNode temporalsNode) {
        List<TimeRange> res = new ArrayList<>();

        if (temporalsNode.has("temporals")) {
            for (final JsonNode temp : temporalsNode.get("temporals")) {
                long from = temp.get("from").asLong();
                long to = temp.get("to").asLong();
                TimeRange temporal = TimeRange.fromTo(from, to);
                res.add(temporal);
            }
        }

        return res;
    }

    @Override
    public Document deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String id = node.get("id").asText();
        String type = node.get("type").asText();
        String entity = (node.has("entity"))? node.get("entity").asText() : null;
        String title = node.get("title").asText();
        String summary = (node.has("summary"))? node.get("summary").asText() : null;
        String text = node.get("text").asText();

        List<LabeledEntity> entities = deserializeLabeledEntities(node);

        List<Passage> passages = new ArrayList();
        if (node.has("passages")) {
            for (final JsonNode pass : node.get("passages")) {
                String passText = pass.get("text").asText();
                List<LabeledEntity> passEntities = deserializeLabeledEntities(pass);

                passages.add(new Passage(passText, passEntities));
            }
        }

        List<PassageExtraction> passageExtractions = new ArrayList();
        if (node.has("passageExtractions")) {
            for (final JsonNode pass : node.get("passageExtractions")) {
                String passId = pass.get("id").asText();
                String passRelation = pass.get("relation").asText();
                List<String> passNamedEntities = new ArrayList<>();
                for (final JsonNode namedEntity : pass.get("namedEntities")) {
                    passNamedEntities.add(namedEntity.asText());
                }
                List<TimeRange> passTemporals = deserializeTemporals(pass);

                passageExtractions.add(new PassageExtraction(passId, passRelation, passNamedEntities, passTemporals));
            }
        }

        List<Extraction> extractions =  new ArrayList<>();
        if (node.has("extractions")) {
            for (final JsonNode ex : node.get("extractions")) {
                String exId = ex.get("id").asText();
                String exRelation = ex.get("relation").asText();
                List<String> exArgs = new ArrayList<>();
                if (ex.has("arguments")) {
                    for (final JsonNode arg : ex.get("arguments")) {
                        exArgs.add(arg.asText());
                    }
                }
                extractions.add(new Extraction(exId, exRelation, exArgs));
            }
        }

        return new Document(id, type, entity, title, summary, text, entities, passages, passageExtractions, extractions);
    }


}
