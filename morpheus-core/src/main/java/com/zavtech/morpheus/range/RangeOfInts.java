/*
 * Copyright (C) 2014-2018 D3X Systems - All Rights Reserved
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
package com.zavtech.morpheus.range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.IntPredicate;

/**
 * A Range implementation for Integers
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
class RangeOfInts extends RangeBase<Integer> {

    private int start;
    private int end;
    private int step;
    private boolean ascend;
    private IntPredicate excludes;

    /**
     * Constructor
     * @param start     the start for range, inclusive
     * @param end       the end for range, exclusive
     * @param step      the step increment
     * @param excludes  optional predicate to exclude elements in this range
     */
    RangeOfInts(int start, int end, int step, IntPredicate excludes) {
        super(start, end);
        this.start = start;
        this.end = end;
        this.step = step;
        this.ascend = start <= end;
        this.excludes = excludes;
    }

    @Override()
    public final long estimateSize() {
        return (long)Math.ceil((double)Math.abs(end-start) / (double)step);
    }

    @Override
    public boolean isAscending() {
        return start < end;
    }

    @Override
    public List<Range<Integer>> split() {
        return split(1000000);
    }

    @Override
    public List<Range<Integer>> split(int splitThreshold) {
        final int[] segmentSteps = getSegmentSteps((int)estimateSize());
        if (segmentSteps[0] < splitThreshold) {
            return Collections.singletonList(this);
        } else {
            final int stepSize = step;
            final List<Range<Integer>> segments = new ArrayList<>();
            for (int i=0; i<segmentSteps.length; ++i) {
                final int segmentSize = stepSize * segmentSteps[i];
                if (i == 0) {
                    final int end = start() + segmentSize * (isAscending() ? 1 : -1);
                    final Range<Integer> range = Range.of(start(), end, step, excludes);
                    segments.add(range);
                } else {
                    final Range<Integer> previous = segments.get(i-1);
                    final int end = previous.end() + segmentSize * (isAscending() ? 1 : -1);
                    final Range<Integer> next = Range.of(previous.end(), end, step, excludes);
                    segments.add(next);
                }
            }
            return segments;
        }
    }

    @Override
    public final Iterator<Integer> iterator() {
        return iteratorOfInt();
    }

    /**
     * Checks that the value specified is in the bounds of this range
     * @param value     the value to check if in bounds
     * @return          true if in bounds
     */
    private boolean inBounds(int value) {
        return ascend ? value >= start && value < end : value <= start && value > end;
    }

    /**
     * Returns a primitive iterator for this range
     * @return  the primitive iterator
     */
    private PrimitiveIterator.OfInt iteratorOfInt() {
        return new PrimitiveIterator.OfInt() {
            private int value = start();
            @Override
            public boolean hasNext() {
                if (excludes != null) {
                    while (excludes.test(value) && inBounds(value)) {
                        value = ascend ? value + step : value - step;
                    }
                }
                return inBounds(value);
            }
            @Override
            public int nextInt() {
                final int next = value;
                value = ascend ? value + step : value - step;
                return next;
            }
        };
    }
}
