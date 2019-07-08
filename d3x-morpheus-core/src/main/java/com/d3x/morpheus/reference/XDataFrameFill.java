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

import com.d3x.morpheus.array.ArrayType;
import com.d3x.morpheus.frame.DataFrameCursor;
import com.d3x.morpheus.frame.DataFrameException;
import com.d3x.morpheus.frame.DataFrameFill;

/**
 * The reference implementation of the DataFrameFill interface
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
class XDataFrameFill<R,C> implements DataFrameFill {


    private XDataFrame<R,C> frame;

    /**
     * Constructor
     * @param frame the frame reference
     */
    XDataFrameFill(XDataFrame<R,C> frame) {
        this.frame = frame;
    }


    @Override()
    public final int up(int intervals) {
        try {
            int totalCount = 0;
            var rowCount = frame.rowCount();
            var colCount = frame.colCount();
            final DataFrameCursor<R,C> cursor = frame.cursor();
            for (int j=0; j<colCount; ++j) {
                int fillCount = 0;
                cursor.colAt(j);
                for (int i=rowCount-2; i>=0; --i) {
                    final Object current = cursor.rowAt(i).getValue();
                    if (current == null || (current instanceof Number && Double.isNaN(((Number)current).doubleValue()))) {
                        if (fillCount < intervals) {
                            final Object next = cursor.rowAt(i + 1).getValue();
                            cursor.rowAt(i).setValue(next);
                            fillCount++;
                            totalCount++;
                        }
                    } else {
                        fillCount = 0;
                    }
                }
            }
            return totalCount;
        } catch (Throwable t) {
            throw new DataFrameException("Failed to fill-up data: " + t.getMessage(), t);
        }
    }


    @Override()
    public final int down(int intervals) {
        try {
            int totalCount = 0;
            var rowCount = frame.rowCount();
            var colCount = frame.colCount();
            final DataFrameCursor<R,C> cursor = frame.cursor();
            for (int j=0; j<colCount; ++j) {
                int fillCount = 0;
                cursor.colAt(j);
                for (int i=1; i<rowCount; ++i) {
                    final Object current = cursor.rowAt(i).getValue();
                    if (current == null || (current instanceof Number && Double.isNaN(((Number)current).doubleValue()))) {
                        if (fillCount < intervals) {
                            final Object previous = cursor.rowAt(i-1).getValue();
                            cursor.rowAt(i).setValue(previous);
                            fillCount++;
                            totalCount++;
                        }
                    } else {
                        fillCount = 0;
                    }
                }
            }
            return totalCount;
        } catch (Throwable t) {
            throw new DataFrameException("Failed to fill-down data: " + t.getMessage(), t);
        }
    }


    @Override()
    public final int left(int intervals) {
        try {
            int totalCount = 0;
            var rowCount = frame.rowCount();
            var colCount = frame.colCount();
            final DataFrameCursor<R,C> cursor = frame.cursor();
            for (int rowIndex=0; rowIndex<rowCount; ++rowIndex) {
                int fillCount = 0;
                cursor.rowAt(rowIndex);
                for (int colIndex=colCount-2; colIndex >= 0; --colIndex) {
                    final Object current = cursor.colAt(colIndex).getValue();
                    if (current == null || (current instanceof Number && Double.isNaN(((Number)current).doubleValue()))) {
                        if (fillCount < intervals) {
                            cursor.colAt(colIndex+1).getValue();
                            final Object previous = cursor.colAt(colIndex+1).getValue();
                            cursor.colAt(colIndex).setValue(previous);
                            fillCount++;
                            totalCount++;
                        }
                    } else {
                        fillCount = 0;
                    }
                }

            }
            return totalCount;
        } catch (Throwable t) {
            throw new DataFrameException("Failed to fill-left data: " + t.getMessage(), t);
        }
    }


    @Override()
    public final int right(int intervals) {
        try {
            int totalCount = 0;
            var rowCount = frame.rowCount();
            var colCount = frame.colCount();
            final Object[] nullValues = getNullValues();
            final DataFrameCursor<R,C> cursor = frame.cursor();
            for (int rowIndex=0; rowIndex<rowCount; ++rowIndex) {
                int fillCount = 0;
                cursor.rowAt(rowIndex);
                for (int colIndex=1; colIndex<colCount; ++colIndex) {
                    final Object nullValue = nullValues[colIndex];
                    final Object current = cursor.colAt(colIndex).getValue();
                    if (equals(current, nullValue)) {
                        if (fillCount < intervals) {
                            final Object previous = cursor.colAt(colIndex-1).getValue();
                            cursor.colAt(colIndex).setValue(previous);
                            fillCount++;
                            totalCount++;
                        }
                    } else {
                        fillCount = 0;
                    }
                }
            }
            return totalCount;
        } catch (Throwable t) {
            throw new DataFrameException("Failed to fill-right data: " + t.getMessage(), t);
        }
    }

    /**
     * Returns true if the two objects are equal
     * @param left      the left object
     * @param right     the right object
     * @return          true if they are equal
     */
    private boolean equals(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        } else if (left instanceof Number && right instanceof Number) {
            var v1 = ((Number)left).doubleValue();
            var v2 = ((Number)right).doubleValue();
            return Double.isNaN(v1) && Double.isNaN(v2) || v1 == v2;
        } else if (left != null && right != null) {
            return left.equals(right);
        } else {
            return false;
        }
    }

    /**
     * Returns the null value array based on frame header
     * @return      the null value array
     */
    private Object[] getNullValues() {
        final Object[] nullValues = new Object[frame.colCount()];
        for (int i = 0; i<frame.colCount(); ++i) {
            final C colKey = frame.cols().key(i);
            final Class<?> type = frame.cols().type(colKey);
            final Object nullValue = ArrayType.defaultValue(type);
            nullValues[i] = nullValue;
        }
        return nullValues;
    }
}
