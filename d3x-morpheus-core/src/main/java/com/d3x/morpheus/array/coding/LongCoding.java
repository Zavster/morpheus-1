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
package com.d3x.morpheus.array.coding;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.d3x.core.util.Option;

/**
 * An interface that exposes a coding between object values and corresponding long code
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public interface LongCoding<T> extends Coding<T> {

    /**
     * Returns the code for the value specified
     * @param value the value, which can be null
     * @return      the code for value
     */
    long getCode(T value);

    /**
     * Returns the value for the code specified
     * @param code  the code for requested value
     * @return      the value match, which can be null
     */
    T getValue(long code);


    /**
     * Returns a newly created coding for java.util.Date objects
     * @return  the newly created LongCoding
     */
    static LongCoding<Date> ofDate() {
        return new LongCoding.OfDate();
    }

    /**
     * Returns a newly created coding for LocalDate objects
     * @return  the newly created LongCoding
     */
    static LongCoding<LocalDate> ofLocalDate() {
        return new LongCoding.OfLocalDate();
    }

    /**
     * Returns a newly created coding for Instant objects
     * @return  the newly created LongCoding
     */
    static LongCoding<Instant> ofInstant() {
        return new LongCoding.OfInstant();
    }

    /**
     * Returns a newly created coding for LocalTime objects
     * @return  the newly created LongCoding
     */
    static LongCoding<LocalTime> ofLocalTime() {
        return new LongCoding.OfLocalTime();
    }

    /**
     * Returns a newly created coding for LocalDateTime objects
     * @return  the newly created LongCoding
     */
    static LongCoding<LocalDateTime> ofLocalDateTime() {
        return new LongCoding.OfLocalDateTime();
    }


    /**
     * Returns a newly created coding for ZonedDateTime objects
     * @return  the newly created LongCoding
     */
    static LongCoding<ZonedDateTime> ofZonedDateTime() {
        return new LongCoding.OfZonedDateTime();
    }


    /**
     * Manages LongCoding support
     */
    class Support {

        private static Map<Class<?>,LongCoding<?>> codingMap = new HashMap<>();

        /*
         * Static initializer
         */
        static {
            Support.register(Long.class, new OfLong());
            Support.register(Date.class, new OfDate());
            Support.register(Instant.class, new OfInstant());
            Support.register(LocalDate.class, new OfLocalDate());
            Support.register(LocalDateTime.class, new OfLocalDateTime());
            Support.register(LocalTime.class, new OfLocalTime());
        }


        /**
         * Returns true if long coding is supported for type
         * @param type  the data type
         * @return      true if supported
         */
        public static boolean includes(Class<?> type) {
            return codingMap.containsKey(type);
        }


        /**
         * Registers a coding definition for the type
         * @param type      the coding type
         * @param coding    the coding instance
         * @param <T>       the type
         */
        public static <T> void register(Class<T> type, LongCoding<T> coding) {
            codingMap.put(type, coding);
        }


        /**
         * Returns the long coding for type if available
         * @param type  the data type
         * @param <T>   the coding type
         * @return      the coding option
         */
        @SuppressWarnings("unchecked")
        public static <T> Option<LongCoding<T>> getCoding(Class<T> type) {
            return Option.of((LongCoding<T>)codingMap.get(type));
        }
    }


    /**
     * An identity coding for Long
     */
    class OfLong extends BaseCoding<Long> implements LongCoding<Long> {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        OfLong() {
            super(Long.class);
        }

        @Override
        public final long getCode(Long value) {
            return value == null ? Long.MIN_VALUE : value;
        }

        @Override
        public final Long getValue(long code) {
            return code == Long.MIN_VALUE ? null : code;
        }
    }


    /**
     * A LongCoding implementation for the Date class.
     */
    class OfDate extends BaseCoding<Date> implements LongCoding<Date> {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        OfDate() {
            super(Date.class);
        }

        @Override
        public final long getCode(Date value) {
            return value == null ? Long.MIN_VALUE : value.getTime();
        }

        @Override
        public final Date getValue(long code) {
            return code == Long.MIN_VALUE ? null : new Date(code);
        }
    }

    /**
     * A LongCoding implementation for the LocalDate class.
     */
    class OfLocalDate extends BaseCoding<LocalDate> implements LongCoding<LocalDate> {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        OfLocalDate() {
            super(LocalDate.class);
        }

        @Override
        public final long getCode(LocalDate value) {
            return value == null ? Long.MIN_VALUE : value.toEpochDay();
        }

        @Override
        public final LocalDate getValue(long code) {
            return code == Long.MIN_VALUE ? null : LocalDate.ofEpochDay(code);
        }
    }


    /**
     * A LongCoding implementation for the Instant class.
     */
    class OfInstant extends BaseCoding<Instant> implements LongCoding<Instant> {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        OfInstant() {
            super(Instant.class);
        }

        @Override
        public final long getCode(Instant value) {
            return value == null ? Long.MIN_VALUE : value.toEpochMilli();
        }

        @Override
        public final Instant getValue(long code) {
            return code == Long.MIN_VALUE ? null : Instant.ofEpochMilli(code);
        }
    }


    /**
     * A LongCoding implementation for the LocalTime class.
     */
    class OfLocalTime extends BaseCoding<LocalTime> implements LongCoding<LocalTime> {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         */
        OfLocalTime() {
            super(LocalTime.class);
        }

        @Override
        public final long getCode(LocalTime value) {
            return value == null ? Long.MIN_VALUE : value.toNanoOfDay();
        }

        @Override
        public final LocalTime getValue(long code) {
            return code == Long.MIN_VALUE ? null : LocalTime.ofNanoOfDay(code);
        }
    }

    /**
     * A LongCoding implementation for the LocalDateTime class.
     */
    class OfLocalDateTime extends BaseCoding<LocalDateTime> implements LongCoding<LocalDateTime> {

        private static final long serialVersionUID = 1L;

        private static final ZoneId UTC = ZoneId.of("UTC");

        /**
         * Constructor
         */
        OfLocalDateTime() {
            super(LocalDateTime.class);
        }

        @Override
        public final long getCode(LocalDateTime value) {
            return value == null ? Long.MIN_VALUE : value.toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        @Override
        public final LocalDateTime getValue(long code) {
            return code == Long.MIN_VALUE ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(code), UTC);
        }
    }


    /**
     * A LongCoding implementation for the ZonedDateTime class.
     */
    class OfZonedDateTime extends BaseCoding<ZonedDateTime> implements LongCoding<ZonedDateTime> {

        private static final long serialVersionUID = 1L;

        private static final ZoneId UTC = ZoneId.of("UTC");

        /**
         * Constructor
         */
        OfZonedDateTime() {
            super(ZonedDateTime.class);
        }

        @Override
        public final long getCode(ZonedDateTime value) {
            return value == null ? Long.MIN_VALUE : value.toInstant().toEpochMilli();
        }

        @Override
        public final ZonedDateTime getValue(long code) {
            return code == Long.MIN_VALUE ? null : ZonedDateTime.ofInstant(Instant.ofEpochMilli(code), UTC);
        }
    }


}
