package net.stargraph.core.processors;

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

import com.typesafe.config.Config;
import net.stargraph.core.Stargraph;
import net.stargraph.core.ner.LinkedNamedEntity;
import net.stargraph.core.ner.NER;
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.*;
import net.stargraph.model.date.TimeParser;
import net.stargraph.model.date.TimeRange;
import org.lambda3.text.simplification.discourse.utils.sentences.SentencesUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Can be placed in the workflow to create passage-extractions.
 */
public final class PassageExtractionProcessor extends BaseProcessor {
    public static String name = "passage-extraction-processor";

    private Stargraph stargraph;
    private static final SimpleIE SIMPLE_IE = new SimpleIE();
    private static final TimeParser TIME_PARSER = new TimeParser();

    public PassageExtractionProcessor(Stargraph stargraph, Config config) {
        super(config);
        this.stargraph = Objects.requireNonNull(stargraph);
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();
        NER ner = stargraph.getKBCore(holder.getKBId().getId()).getNER();

        if (entry instanceof Document) {
            Document document = (Document)entry;

            List<PassageExtraction> passageExtractions = new ArrayList<>();

            List<Extraction> extractions = SIMPLE_IE.extract(document.getText());

            // convert extractions into passage-extractions
            for (Extraction extraction : extractions) {
                String relation = extraction.getRelation();
                String concArgStr = extraction.getArguments().stream().collect(Collectors.joining(" "));

                List<LinkedNamedEntity> lners = ner.searchAndLink(concArgStr);

                // only add linked entities
                List<LabeledEntity> entities = lners.parallelStream()
                        .filter(e -> e.isLinked())
                        .map(LinkedNamedEntity::getEntity).collect(Collectors.toList());

                List<TimeRange> temporals = TIME_PARSER.parse(concArgStr);

                passageExtractions.add(new PassageExtraction(relation, entities, temporals));
            }


            holder.set(new Document(
                    document.getId(),
                    document.getType(),
                    document.getEntity(),
                    document.getTitle(),
                    document.getSummary(),
                    document.getText(),
                    document.getEntities(),
                    document.getPassages(),
                    passageExtractions,
                    document.getExtractions()));
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
