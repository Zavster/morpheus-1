/*
 * Copyright (C) 2014-2021 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.morpheus.pipeline;

import java.util.Collection;
import java.util.List;

import com.d3x.morpheus.vector.DataVector;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Collects a sequence of pipelines into a single pipeline.
 *
 * @author Scott Shaffer
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompositePipeline implements DataPipeline {
    /**
     * The pipelines to apply, in the order of application.
     */
    @Getter @NonNull
    private final List<DataPipeline> pipelines;

    /**
     * Creates a composite pipeline from a series of individual pipelines.
     *
     * @param pipelines the pipelines to compose the composite.
     *
     * @return the composite of the specified pipelines.
     */
    public static CompositePipeline of(DataPipeline... pipelines) {
        return new CompositePipeline(List.of(pipelines));
    }

    /**
     * Creates a composite pipeline from a series of individual pipelines.
     *
     * @param pipelines the pipelines to compose the composite.
     *
     * @return the composite of the specified pipelines.
     */
    public static CompositePipeline of(Collection<DataPipeline> pipelines) {
        return new CompositePipeline(List.copyOf(pipelines));
    }

    @Override
    public <K> DataVector<K> apply(DataVector<K> vector) {
        for (DataPipeline pipeline : pipelines)
            pipeline.apply(vector);

        return vector;
    }

    @Override
    public boolean isLocal() {
        return pipelines.stream().allMatch(DataPipeline::isLocal);
    }

    @Override
    public boolean isSizePreserving() {
        return pipelines.stream().allMatch(DataPipeline::isSizePreserving);
    }
}
