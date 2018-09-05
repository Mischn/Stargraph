package net.stargraph.core.query.entity;

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

import net.stargraph.StarGraphException;
import net.stargraph.core.query.InteractionModeSelector;
import net.stargraph.query.InteractionMode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class EntityQueryBuilder {

    public EntityQuery parse(String queryString, InteractionMode mode){

        EntityQuery entityQuery;

        switch (mode) {
            case ENTITY_SIMILARITY:
                entityQuery = parseSimilarityQuery(queryString);
                break;
            case DEFINITION:
                entityQuery = parseDefinitionQuery(queryString);
                break;
            default:
                throw new StarGraphException("Input type not yet supported");
        }

        return entityQuery;
    }

    private EntityQuery parseSimilarityQuery(String queryString){
        String coreEntity = "";

        Pattern p = Pattern.compile(InteractionModeSelector.ENTITY_SIMILARITY_PATTERN);
        Matcher m = p.matcher(queryString);
        if (m.matches()) {
            coreEntity = queryString.substring(m.start(1)).replaceAll("\\?", "").trim();
        }

        return new EntityQuery(coreEntity);
    }

    private EntityQuery parseDefinitionQuery(String queryString){
        String coreEntity = "";

        Pattern p = Pattern.compile(InteractionModeSelector.DEFINITION_PATTERN);
        Matcher m = p.matcher(queryString);
        if (m.matches()) {
            coreEntity = queryString.substring(m.start(1)).replaceAll("\\?", "").trim();
        }

        return new EntityQuery(coreEntity);
    }

}
