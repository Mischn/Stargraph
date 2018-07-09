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
import java.util.List;

class DocumentDeserializer extends AbstractDeserializer<Document> {

    DocumentDeserializer(KBId kbId) {
        super(kbId, PropertyEntity.class);
    }

    private static LabeledEntity deserializeLabeledEntity(JsonNode node) {
        String id = node.get("id").asText();
        String value = node.get("value").asText();

        if (node.has("language")) {
            return new ValueEntity(id, value, node.get("dataType").asText(null), node.get("language").asText(null));
        } else {
            return new InstanceEntity(id, value);
        }
    }

    private static Passage deserializePassage(JsonNode node) {
        String text = node.get("text").asText();

        List<LabeledEntity> entities = new ArrayList<>();
        if (node.has("entities")) {
            for (final JsonNode ent : node.get("entities")) {
                entities.add(deserializeLabeledEntity(ent));
            }
        }

        return new Passage(text, entities);
    }

    private static PassageExtraction deserializePassageExtraction(JsonNode node) {
        String id = node.get("id").asText();
        String relation = node.get("relation").asText();

        List<String> terms = new ArrayList<>();
        if (node.has("terms")) {
            for (final JsonNode term : node.get("terms")) {
                terms.add(term.asText());
            }
        }

        List<TimeRange> temporals = new ArrayList<>();
        if (node.has("temporals")) {
            for (final JsonNode tmp : node.get("temporals")) {
                temporals.add(deserializeTemporal(tmp));
            }
        }

        return new PassageExtraction(id, relation, terms, temporals);
    }

    private static Extraction deserializeExtraction(JsonNode node) {
        String id = node.get("id").asText();
        String relation = node.get("relation").asText();

        List<String> args = new ArrayList<>();
        if (node.has("arguments")) {
            for (final JsonNode arg : node.get("arguments")) {
                args.add(arg.asText());
            }
        }
        return new Extraction(id, relation, args);
    }

    private static TimeRange deserializeTemporal(JsonNode node) {
            long from = node.get("from").asLong();
            long to = node.get("to").asLong();
            return TimeRange.fromTo(from, to);
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

        // named entities
        List<LabeledEntity> entities = new ArrayList<>();
        if (node.has("entities")) {
            for (final JsonNode nent : node.get("entities")) {
                entities.add(deserializeLabeledEntity(nent));
            }
        }

        // passages
        List<Passage> passages = new ArrayList();
        if (node.has("passages")) {
            for (final JsonNode pass : node.get("passages")) {
                passages.add(deserializePassage(pass));
            }
        }

        // passageExtractions
        List<PassageExtraction> passageExtractions = new ArrayList();
        if (node.has("passageExtractions")) {
            for (final JsonNode passEx : node.get("passageExtractions")) {
                passageExtractions.add(deserializePassageExtraction(passEx));
            }
        }

        // extractions
        List<Extraction> extractions =  new ArrayList<>();
        if (node.has("extractions")) {
            for (final JsonNode ex : node.get("extractions")) {
                extractions.add(deserializeExtraction(ex));
            }
        }

        return new Document(id, type, entity, title, summary, text, entities, passages, passageExtractions, extractions);
    }


}
