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
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.Document;
import net.stargraph.model.Extraction;
import org.lambda3.graphene.core.Graphene;
import org.lambda3.graphene.core.relation_extraction.model.RelationExtractionContent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Can be placed in the workflow to create extractions for documents.
 */
public final class OpenIEProcessor extends BaseProcessor {
    public static String name = "oie-extractor";

    private Graphene graphene;

    public OpenIEProcessor(Config config) {
        super(config);
        graphene = new Graphene(getConfig());
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();

        if (entry instanceof Document) {
            Document document = (Document) entry;
            List<Extraction> extractions = new ArrayList();

            RelationExtractionContent ec = graphene.doRelationExtraction(document.getText(), false, false);
            ec.getExtractions().stream().forEach(e -> {
                List<String> args = new ArrayList<>();
                args.add(e.getArg1());
                args.add(e.getArg2());
                e.getSimpleContexts().forEach(sc -> args.add(sc.getText()));

                extractions.add(new Extraction(e.getId(), e.getRelation(), args));
            });

            holder.set(new Document(
                    document.getId(),
                    document.getType(),
                    document.getEntity(),
                    document.getTitle(),
                    document.getSummary(),
                    document.getText(),
                    document.getEntities(),
                    document.getTerms(),
                    document.getPassages(),
                    document.getPassageExtractions(),
                    extractions
            ));
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
