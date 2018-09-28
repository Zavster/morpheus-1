/**
 * Copyright (C) 2014-2017 Xavier Witdouck
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
package com.zavtech.morpheus.viz.chart;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum that defines various shapes that can be used to represent points on a chart
 *
 * @author  Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public enum ChartShape {

    CIRCLE,
    SQUARE,
    DIAMOND,
    TRIANGLE_UP,
    TRIANGLE_DOWN,
    TRIANGLE_RIGHT,
    TRIANGLE_LEFT;


    /**
     * An interface to an entity that provides shapes for different keys
     */
    public interface Provider {

        /**
         * Returns a shape for the key specified
         * @param key   the arbitrary key
         * @return      the shape
         */
        ChartShape getShape(Object key);
    }


    /**
     * The default shape provider
     */
    public static class DefaultProvider implements Provider {

        private volatile int index = -1;
        private ChartShape[] shapes = ChartShape.values();
        private Map<Object,ChartShape> shapeMap = new HashMap<>();

        @Override
        public ChartShape getShape(Object key) {
            ChartShape shape = shapeMap.get(key);
            if (shape == null) {
                shape = next();
                shapeMap.put(key, shape);
            }
            return shape;
        }

        /**
         * Returns the next shape in line
         * @return  the next shape
         */
        private ChartShape next() {
            this.index++;
            if (index < shapes.length) {
                return shapes[index];
            } else {
                this.index = 0;
                return shapes[index];
            }
        }
    }

}
