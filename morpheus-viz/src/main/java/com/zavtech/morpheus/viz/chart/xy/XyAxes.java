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
package com.zavtech.morpheus.viz.chart.xy;

/**
 * An interface that provides access to the domain and range axes in an XyPlot.
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public interface XyAxes {

    /**
     * Returns a reference to the domain axis
     * @return the domain axis reference
     */
    XyAxis domain();

    /**
     * Returns a reference to the range axis for the index specified
     * @param index the index for range axis, 0 for primary, 1 for secondary
     * @return the range axis reference
     */
    XyAxis range(int index);
}
