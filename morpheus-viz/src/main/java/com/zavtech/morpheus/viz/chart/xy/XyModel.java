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

import com.zavtech.morpheus.frame.DataFrame;

/**
 * Interface to the overall data model for an XYPlot, which allows multiple XYDatasets to be plotted
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public interface XyModel<X extends Comparable,S extends Comparable> {

    /**
     * Clears all data from the plot
     */
    void removeAll();

    /**
     * Removes the data at the specified index
     * @param index the index of the dataset to remove
     */
    void remove(int index);

    /**
     * Sets the range axis to use for the dataset specified
     * @param dataset   the dataset index
     * @param axis      tne range axis index, 0 for primary, 1 for secondary etc...
     */
    void setRangeAxis(int dataset, int axis);

    /**
     * Returns the domain type for this model
     * @return      the domain type for model
     */
    Class<X> domainType();

    /**
     * Returns the chart model holding chart data at the specified index
     * @param index the index for requested data
     * @return      the chart model for index, null if no data for index
     */
    XyDataset<X,S> at(int index);

    /**
     * Adds a data frame using the row keys to define the domain
     * @param frame the data frame containing chart data where columns represent series
     * @return      the index assigned to the newly added dataset
     */
    int add(DataFrame<X,S> frame);

    /**
     * Adds a data frame using a specific column in the frame to define the domain axis
     * @param frame     the data frame containing chart data where column represent series, except for column with domainKey
     * @param domainKey the column key that should be used to define the domain
     * @return          the index assigned to the newly added dataset
     */
    int add(DataFrame<?,S> frame, S domainKey);

    /**
     * Applies an updated DataFrame to the model at the specified index
     * @param index the index of the data model to update with the specified frame
     * @param frame the data frame containing chart data where columns represent series
     * @return  the index assigned to the newly added dataset
     */
    XyDataset<X,S> update(int index, DataFrame<X,S> frame);

    /**
     * Applies an updated DataFrame to the model at the specified index
     * @param index the index of the data model to update with the specified frame
     * @param frame     the data frame containing chart data where column represent series, except for column with domainKey
     * @param domainKey the column key that should be used to define the domain
     * @return  the index assigned to the newly added dataset
     */
    XyDataset<X,S> update(int index, DataFrame<?,S> frame, S domainKey);
}
