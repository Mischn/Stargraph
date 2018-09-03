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
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.core.tools.SimpleIE.impl.SASimplePassageIE;
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.Document;
import net.stargraph.model.PassageExtraction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Can be placed in the workflow to create passage-extractions.
 */
public final class PassageExtractionProcessor extends BaseProcessor {
    public static String name = "passage-extraction-processor";

    private Stargraph stargraph;
    Map<String, SimpleIE> passageIEs;

    public PassageExtractionProcessor(Stargraph stargraph, Config config) {
        super(config);
        this.stargraph = Objects.requireNonNull(stargraph);
        this.passageIEs = new HashMap<>();
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();

        SimpleIE passageIE = passageIEs.computeIfAbsent(holder.getKBId().getId(), (id) -> new SASimplePassageIE(stargraph, id));

        if (entry instanceof Document) {
            Document document = (Document)entry;

            List<PassageExtraction> passageExtractions = passageIE.extractFromText(document.getText());

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
                    passageExtractions,
                    document.getExtractions()));
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
