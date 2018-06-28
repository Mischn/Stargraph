package net.stargraph.core.search;

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

import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.ModifiableSearchParams;

import java.util.List;

public interface SearchQueryGenerator {

    // return resource entities that match any of the given ids
    SearchQueryHolder entitiesWithIds(List<String> idList, ModifiableSearchParams searchParams);

    // return property entities that match any of the given ids
    SearchQueryHolder propertiesWithIds(List<String> idList, ModifiableSearchParams searchParams);

    // return documents that match any of the given ids
    SearchQueryHolder documentsWithIds(List<String> idList, ModifiableSearchParams searchParams);

    // return entity-documents that match any of the given entity-ids and their document-types
    // if docTypes is null, return for all docTypes
    SearchQueryHolder documentsForEntityIds(List<String> idList, List<String> docTypes, ModifiableSearchParams searchParams);

    // return facts that represent an is-a relationship between arbitrary subjects and any object of the given ids
    SearchQueryHolder findClassFacts(List<String> idList, boolean inSubject, ModifiableSearchParams searchParams);

    // return resource entities whose value or otherValues match the searchTerm
    SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits);

    // return class resource entities whose value or otherValues match the searchTerm
    SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits);

    // return properties whose hyponyms, hypernyms or synonyms match the searchTerm (why not including the value?)
    SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits);

    // return facts that represent an arbitrary relationship with the pivot being either a subject or an object
    SearchQueryHolder findPivotFacts(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject);

    // return documents that are similar to the searchTerm, matching docTypes
    SearchQueryHolder findSimilarDocuments(List<String> docTypes, boolean entityDocument, List<String> texts, ModifiableSearchParams searchParams);
}
