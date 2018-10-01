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
package com.d3x.morpheus.range;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A Range implementation for LocalDates
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
class RangeOfLocalDates extends RangeBase<LocalDate> {

    private Period step;
    private boolean ascend;
    private Predicate<LocalDate> excludes;

    /**
     * Constructor
     * @param start     the start for range, inclusive
     * @param end       the end for range, exclusive
     * @param step      the step increment
     * @param excludes  optional predicate to exclude elements in this range
     */
    RangeOfLocalDates(LocalDate start, LocalDate end, Period step, Predicate<LocalDate> excludes) {
        super(start, end);
        this.step = step;
        this.ascend = start.compareTo(end) <= 0;
        this.excludes = excludes;
    }

    @Override
    public long estimateSize() {
        if (ascend) {
            final long stepSize = ChronoUnit.DAYS.between(start(), start().plus(step));
            final long totalSize = ChronoUnit.DAYS.between(start(), end());
            return (long)Math.ceil((double)totalSize / (double)stepSize);
        } else {
            final long stepSize = ChronoUnit.DAYS.between(start(), start().plus(step));
            final long totalSize = ChronoUnit.DAYS.between(end(), start());
            return (long)Math.ceil((double)totalSize / (double)stepSize);
        }
    }

    @Override
    public boolean isAscending() {
        return start().isBefore(end());
    }

    @Override
    public List<Range<LocalDate>> split() {
        return split(100000);
    }

    @Override
    public List<Range<LocalDate>> split(int splitThreshold) {
        final int[] segmentSteps = getSegmentSteps((int)estimateSize());
        if (segmentSteps[0] < splitThreshold) {
            return Collections.singletonList(this);
        } else {
            final Period stepSize = step;
            final List<Range<LocalDate>> segments = new ArrayList<>();
            for (int i=0; i<segmentSteps.length; ++i) {
                final Period segmentSize = stepSize.multipliedBy(segmentSteps[i]);
                if (i == 0) {
                    final LocalDate end = isAscending() ? start().plus(segmentSize) : start().minus(segmentSize);
                    final Range<LocalDate> range = Range.of(start(), end, step, excludes);
                    segments.add(range);
                } else {
                    final Range<LocalDate> previous = segments.get(i-1);
                    final LocalDate end = isAscending() ? previous.end().plus(segmentSize) : previous.end().minus(segmentSize);
                    final Range<LocalDate> next = Range.of(previous.end(), end, step, excludes);
                    segments.add(next);
                }
            }
            return segments;
        }
    }

    @Override
    public final Iterator<LocalDate> iterator() {
        return new Iterator<LocalDate>() {
            private LocalDate value = start();
            @Override
            public boolean hasNext() {
                if (excludes != null) {
                    while (excludes.test(value) && inBounds(value)) {
                        value = ascend ? value.plus(step) : value.minus(step);
                    }
                }
                return inBounds(value);
            }
            @Override
            public LocalDate next() {
                final LocalDate next = value;
                value = ascend ? value.plus(step) : value.minus(step);
                return next;
            }
        };
    }

    /**
     * Checks that the value specified is in the bounds of this range
     * @param value     the value to check if in bounds
     * @return          true if in bounds
     */
    private boolean inBounds(LocalDate value) {
        return ascend ? value.compareTo(start()) >=0 && value.isBefore(end()) : value.compareTo(start()) <=0 && value.isAfter(end());
    }
}
