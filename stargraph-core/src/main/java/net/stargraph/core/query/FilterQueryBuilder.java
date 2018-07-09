package net.stargraph.core.query;

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

import net.stargraph.core.ner.NER;
import net.stargraph.core.processors.PassageExtractionProcessor;
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.model.PassageExtraction;
import net.stargraph.query.InteractionMode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public final class FilterQueryBuilder {
    private static final SimpleIE SIMPLE_IE = new SimpleIE();

    private final NER ner;

    public FilterQueryBuilder(NER ner) {
        this.ner = ner;
    }

    // TODO put settings in reference.conf and make specific for different languages
    public static String getObjectRelation(String expr) {
        if (expr.toLowerCase().equals("from") || expr.toLowerCase().equals("of")) {
            return "created";
        } else if (expr.toLowerCase().equals("with")) {
            return "have";
        }
        return null;
    }

    public FilterQuery parse(String queryString, InteractionMode mode){
        Pattern pattern = Pattern.compile("^.* all (?<text>(?<obj>objects) (?<expr>\\w+) (.*))$");
        Matcher matcher = pattern.matcher(queryString);
        if (matcher.matches()) {
            String text = matcher.group("text");
            String obj = matcher.group("obj");
            String expr = matcher.group("expr");

            List<PassageExtraction> extractionFilters = SIMPLE_IE.extract(text).stream().map(e -> PassageExtractionProcessor.convert(e, ner)).collect(Collectors.toList());

            PassageExtraction objectFilter = PassageExtractionProcessor.convert(SIMPLE_IE.extractModifiers(text, obj), ner);
            String objectRelation = getObjectRelation(expr);
            if (objectRelation != null) {
                extractionFilters.add(new PassageExtraction(objectRelation, objectFilter.getTerms(), objectFilter.getTemporals()));
            }

            return new FilterQuery(extractionFilters);
        }
        return null;
    }
}
