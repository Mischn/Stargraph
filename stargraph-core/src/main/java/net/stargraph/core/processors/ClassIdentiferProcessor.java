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
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.*;

import java.io.Serializable;

/**
 * Can be placed in the workflow to identify classes.
 */
public final class ClassIdentiferProcessor extends BaseProcessor {
    public static String name = "class-identifier";

    private EntitySearcher entitySearcher;

    public ClassIdentiferProcessor(Stargraph stargraph, Config config) {
        super(config);
        this.entitySearcher = stargraph.getEntitySearcher();
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();

        if (entry instanceof InstanceEntity) {
            InstanceEntity entity = (InstanceEntity)entry;

            ModifiableSearchParams searchParams = ModifiableSearchParams.create(holder.getKBId().getId()).lookup(false).limit(1);
            boolean isClass = entitySearcher.getClassMembers(entity, searchParams).size() > 0;

            holder.set(new InstanceEntity(entity.getId(), entity.getValue(), isClass, entity.getOtherValues()));
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
