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

import net.stargraph.core.Stargraph;
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.core.tools.SimpleIE.impl.SASimplePassageIE;
import net.stargraph.model.PassageExtraction;
import net.stargraph.query.InteractionMode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class FilterQueryBuilder {
    private SimpleIE<PassageExtraction> simplePassageIE;

    public FilterQueryBuilder(Stargraph stargraph, String dbId) {
        this.simplePassageIE = new SASimplePassageIE(stargraph, dbId);
    }

    // TODO put settings in reference.conf and make specific for different languages
    public static String getObjectRelation(String expr) {
        if (expr.toLowerCase().matches("from|of|between|during")) {
            return "created";
        } else if (expr.toLowerCase().equals("with")) {
            return "depict";
        }
        return null;
    }

    public FilterQuery parse(String queryString, InteractionMode mode){
        Pattern pattern = Pattern.compile(InteractionModeSelector.FILTER_PATTERN);
        Matcher matcher = pattern.matcher(queryString);
        if (matcher.matches()) {
            String text = queryString.substring(matcher.start("obj"));
            String obj = matcher.group("obj");
            String expr = matcher.group("expr");

            List<PassageExtraction> extractionFilters = simplePassageIE.extract(text);
            PassageExtraction objectFilter = simplePassageIE.extractModifiers(text, obj);
            if (objectFilter != null) {
                String objectRelation = getObjectRelation(expr);
                if (objectRelation != null) {
                    extractionFilters.add(new PassageExtraction(objectRelation, objectFilter.getTerms(), objectFilter.getTemporals()));
                }
            }

            return new FilterQuery(extractionFilters);
        }
        return null;
    }
}
