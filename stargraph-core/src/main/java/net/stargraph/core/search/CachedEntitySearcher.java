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

import net.stargraph.core.Cache;
import net.stargraph.core.Stargraph;
import net.stargraph.model.Document;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.PropertyEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class CachedEntitySearcher extends EntitySearcher {
    private static long CACHE_SIZE = 10000;

    private static abstract class LookupCache<T> extends Cache<String, Optional<T>> {

        private final String dbId;

        public LookupCache(String dbId) {
            super(CACHE_SIZE);
            this.dbId = dbId;
        }

        protected abstract T _generateValue(String id); // should return null when not found

        @Override
        public Optional<T> generateValue(String id) throws Exception {
            Optional<T> entity = Optional.ofNullable(_generateValue(id));
            return entity;
        }
    }

    private Map<String, LookupCache<InstanceEntity>> instanceEntityCaches;
    private Map<String, LookupCache<PropertyEntity>> propertyEntityCaches;
    private Map<String, LookupCache<Document>> documentCaches;

    public CachedEntitySearcher(Stargraph stargraph) {
        super(stargraph);

        this.instanceEntityCaches = new ConcurrentHashMap<>();
        this.propertyEntityCaches = new ConcurrentHashMap<>();
        this.documentCaches = new ConcurrentHashMap<>();
    }

    public InstanceEntity lookupInstanceEntity(String dbId, String id) {
        return super.getInstanceEntity(dbId, id);
    }

    public PropertyEntity lookupPropertyEntity(String dbId, String id) {
        return super.getPropertyEntity(dbId, id);
    }

    public Document lookupDocument(String dbId, String id) {
        return super.getDocument(dbId, id);
    }

    @Override
    public InstanceEntity getInstanceEntity(String dbId, String id) {
        LookupCache<InstanceEntity> cache = instanceEntityCaches.computeIfAbsent(dbId, (i) -> new LookupCache<InstanceEntity>(dbId) {
            @Override
            protected InstanceEntity _generateValue(String id) {
                return lookupInstanceEntity(dbId, id);
            }
        });

        try {
            return cache.get(id).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public PropertyEntity getPropertyEntity(String dbId, String id) {
        LookupCache<PropertyEntity> cache = propertyEntityCaches.computeIfAbsent(dbId, (i) -> new LookupCache<PropertyEntity>(dbId) {
            @Override
            protected PropertyEntity _generateValue(String id) {
                return lookupPropertyEntity(dbId, id);
            }
        });

        try {
            return cache.get(id).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public Document getDocument(String dbId, String id) {
        LookupCache<Document> cache = documentCaches.computeIfAbsent(dbId, (i) -> new LookupCache<Document>(dbId) {
            @Override
            protected Document _generateValue(String id) {
                return lookupDocument(dbId, id);
            }
        });

        try {
            return cache.get(id).orElse(null);
        } catch (ExecutionException e) {
            return null;
        }
    }
}
