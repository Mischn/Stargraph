package net.stargraph.core.data;

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

import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.BaseGraphModel;
import net.stargraph.core.impl.jena.JenaBaseSearcher;
import net.stargraph.core.impl.jena.JenaGraphSearcher;
import net.stargraph.data.Indexable;
import net.stargraph.model.Document;
import net.stargraph.model.KBId;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.engine.binding.Binding;

import java.util.*;

/**
 * Produces entity-documents from literals (selected by SPARQL-Query)
 */
final class DocumentLiteralIterator implements Iterator<Indexable> {
    private final JenaGraphSearcher graphSearcher;
    private final KBId kbId;
    private final String sparqlQuery;
    private final String docType;

    private Iterator<Indexable> innerIt;

    public DocumentLiteralIterator(Stargraph stargraph, KBId kbId, String sparqlQuery, String docType, BaseGraphModel model) {
        this.kbId = Objects.requireNonNull(kbId);
        this.graphSearcher = (JenaGraphSearcher)stargraph.getKBCore(kbId.getId()).getGraphSearcher(model);
        this.sparqlQuery = Objects.requireNonNull(sparqlQuery);
        this.docType = Objects.requireNonNull(docType);
    }

    public DocumentLiteralIterator(Stargraph stargraph, KBId kbId, String sparqlQuery, String docType) {
        this.kbId = Objects.requireNonNull(kbId);
        this.graphSearcher = (JenaGraphSearcher)stargraph.getKBCore(kbId.getId()).getGraphSearcher();
        this.sparqlQuery = Objects.requireNonNull(sparqlQuery);
        this.docType = Objects.requireNonNull(docType);
    }

    private void init() {
        List<Indexable> docs = new ArrayList();
        graphSearcher.sparqlQuery(sparqlQuery, new JenaBaseSearcher.SparqlIteration() {
            @Override
            public void process(Binding binding) {
                // assume to contain ?e (for entitiy) and ?t for (text)
                Map<String, Node> varMap = JenaGraphSearcher.getVarMap(binding);
                if (!varMap.get("t").isLiteral()) {
                    return;
                }

                Document document = new Document(
                        varMap.get("e").getURI() + "#" + docType,
                        docType,
                        varMap.get("e").getURI(),
                        docType,
                        null,
                        varMap.get("t").getLiteralLexicalForm()
                );

                docs.add(new Indexable(document, kbId));
            }
        });
        innerIt = docs.iterator();
    }

    @Override
    public boolean hasNext() {
        if (innerIt == null) {
            init();
        }
        return innerIt.hasNext();
    }

    @Override
    public Indexable next() {
        if (innerIt == null) {
            init();
        }
        return innerIt.next();
    }
}
