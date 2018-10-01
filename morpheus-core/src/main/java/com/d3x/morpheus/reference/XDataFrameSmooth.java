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
package com.d3x.morpheus.reference;

import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.frame.DataFrameException;
import com.d3x.morpheus.frame.DataFrameSmooth;

/**
 * The default implementation of the DataFrameSmooth interface
 *
 * @param <R>   the row key type
 * @param <C>   the column key type
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
class XDataFrameSmooth<R,C> implements DataFrameSmooth<R,C> {

    private boolean inPlace;
    private DataFrame<R,C> frame;

    /**
     * Constructor
     * @param frame the frame reference
     */
    XDataFrameSmooth(boolean inPlace, DataFrame<R, C> frame) {
        this.inPlace = inPlace;
        this.frame = frame;
    }

    @Override
    public DataFrame<R,C> sma(double windowSize) {
        return frame;
    }


    @Override
    public DataFrame<R,C> ema(double halfLife) {
        try {
            if (halfLife < 0) {
                throw new IllegalArgumentException("Half-life for smoothing must be >= 0, " + halfLife + " is illegal");
            } else if (halfLife == 0d) {
                return frame;
            } else if (!inPlace) {
                return frame.copy().smooth(true).ema(halfLife);
            } else {
                final int rowCount = frame.rows().count();
                if (rowCount > 0) {
                    final double alpha = 1d - Math.exp(Math.log(0.5d) / halfLife);
                    this.frame.rows().sequential().forEach(row -> {
                        final int rowOrdinal = row.ordinal();
                        if (rowOrdinal > 0) {
                            row.forEachValue(v -> {
                                final double rawValue = v.getDouble();
                                final double emaPrior = v.col().getDoubleAt(rowOrdinal - 1);
                                final double emaValue = rawValue * alpha + (1d - alpha) * emaPrior;
                                v.setDouble(emaValue);
                            });
                        }
                    });
                }
                return frame;
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to apply EWMA smoothing to DataFrame", ex);
        }
    }
}
