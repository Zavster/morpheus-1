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
package com.d3x.morpheus.reference.algebra;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import com.d3x.morpheus.array.Array;
import com.d3x.morpheus.array.ArrayType;
import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.frame.DataFrameAlgebra;
import com.d3x.morpheus.frame.DataFrameColumn;
import com.d3x.morpheus.frame.DataFrameException;
import com.d3x.morpheus.util.Asserts;

/**
 * A convenience base class for building third-party library specific implementations for DataFrame Linear Algebra functionality.
 *
 * @param <R>   the row key type
 * @param <C>   the column key type
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
public abstract class XDataFrameAlgebra<R,C> implements DataFrameAlgebra<R,C> {

    private DataFrame<R,C> frame;

    /**
     * Constructor
     * @param frame     the frame reference
     */
    XDataFrameAlgebra(DataFrame<R,C> frame) {
        this.frame = frame;
    }


    /**
     * Returns a newly created DataFrame Linear Algebra engine to operate on the specified frame
     * @param frame     the frame to operate on
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created Linear Algebra engine
     */
    public static <R,C> DataFrameAlgebra<R,C> create(DataFrame<R,C> frame) {
        final DataFrameAlgebra.Lib lib = DataFrameAlgebra.LIBRARY.get();
        if (lib == null) {
            return new XDataFrameAlgebraApache<>(frame);
        } else {
            switch (lib) {
                case APACHE:    return new XDataFrameAlgebraApache<>(frame);
                case JAMA:      return new XDataFrameAlgebraJama<>(frame);
                default:        throw new IllegalStateException("Unsupported Linear Algebra library: " + lib);
            }
        }
    }


    /**
     * Returns a reference to the frame
     * @return  the frame reference
     */
    protected final DataFrame<R,C> frame() {
        return frame;
    }


    @Override
    public final DataFrame<R,C> plus(Number scalar) throws DataFrameException {
        try {
            Asserts.notNull(scalar, "The scalar value cannot be null");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        return left + scalar.intValue();
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        return left + scalar.longValue();
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        return left + scalar.doubleValue();
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to add scalar value to DataFrame", ex);
        }
    }


    @Override
    public final DataFrame<R,C> plus(DataFrame<?,?> other) throws DataFrameException {
        try {
            Asserts.notNull(other, "The frame cannot be null");
            Asserts.check(frame.rowCount() == other.rowCount(), "The row counts of the two frames must match");
            Asserts.check(frame.colCount() == other.colCount(), "The column counts of the two frames must match");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                final DataFrameColumn otherCol = other.colAt(column.ordinal());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        final int right = otherCol.getIntAt(v.rowOrdinal());
                        return left + right;
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        final long right = otherCol.getLongAt(v.rowOrdinal());
                        return left + right;
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        final double right = otherCol.getDoubleAt(v.rowOrdinal());
                        return left + right;
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to add two DataFrames", ex);
        }
    }


    @Override
    public final DataFrame<R,C> minus(Number scalar) throws DataFrameException {
        try {
            Asserts.notNull(scalar, "The scalar value cannot be null");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        return left - scalar.intValue();
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        return left - scalar.longValue();
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        return left - scalar.doubleValue();
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to subtract scalar value from DataFrame", ex);
        }
    }


    @Override
    public final DataFrame<R,C> minus(DataFrame<?,?> other) throws DataFrameException {
        try {
            Asserts.notNull(other, "The frame cannot be null");
            Asserts.check(frame.rowCount() == other.rowCount(), "The row counts of the two frames must match");
            Asserts.check(frame.colCount() == other.colCount(), "The column counts of the two frames must match");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                final DataFrameColumn otherCol = other.colAt(column.ordinal());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        final int right = otherCol.getIntAt(v.rowOrdinal());
                        return left - right;
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        final long right = otherCol.getLongAt(v.rowOrdinal());
                        return left - right;
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        final double right = otherCol.getDoubleAt(v.rowOrdinal());
                        return left - right;
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to subtract two DataFrames", ex);
        }
    }


    @Override
    public final DataFrame<R,C> times(Number scalar) throws DataFrameException {
        try {
            Asserts.notNull(scalar, "The scalar value cannot be null");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        return left * scalar.intValue();
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        return left * scalar.longValue();
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        return left * scalar.doubleValue();
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to multiply DataFrame by scalar", ex);
        }
    }


    @Override
    public final DataFrame<R,C> times(DataFrame<?,?> other) throws DataFrameException {
        try {
            Asserts.notNull(other, "The frame cannot be null");
            Asserts.check(frame.rowCount() == other.rowCount(), "The row counts of the two frames must match");
            Asserts.check(frame.colCount() == other.colCount(), "The column counts of the two frames must match");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                final DataFrameColumn otherCol = other.colAt(column.ordinal());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        final int right = otherCol.getIntAt(v.rowOrdinal());
                        return left * right;
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        final long right = otherCol.getLongAt(v.rowOrdinal());
                        return left * right;
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        final double right = otherCol.getDoubleAt(v.rowOrdinal());
                        return left * right;
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to multiply two DataFrames", ex);
        }
    }


    @Override
    public final <X,Y> DataFrame<R,Y> dot(DataFrame<X,Y> right) throws DataFrameException {
        try {
            final DataFrame<R,C> left = frame();
            final Array<R> rowKeys = left.rows().keyArray();
            final Array<Y> colKeys = right.cols().keyArray();
            final DataFrame<R,Y> result = DataFrame.ofDoubles(rowKeys, colKeys);
            final int count = result.rowCount() * result.colCount();
            if (frame().isParallel()) {
                final int threshold = Math.max(10, count / Runtime.getRuntime().availableProcessors());
                final DotProduct action = new DotProduct(left, right, result, 0, count, threshold);
                ForkJoinPool.commonPool().invoke(action);
            } else {
                final int threshold = Integer.MAX_VALUE;
                final DotProduct action = new DotProduct(left, right, result, 0, count, threshold);
                action.compute();
            }
            return result;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to calculate the dot product of two DataFrames", ex);
        }
    }


    @Override
    public final DataFrame<R,C> divide(Number scalar) throws DataFrameException {
        try {
            Asserts.notNull(scalar, "The scalar value cannot be null");
            Asserts.notNull(scalar.doubleValue() != 0d, "The scalar value cannot be zero");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        return left / scalar.intValue();
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        return left / scalar.longValue();
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        return left / scalar.doubleValue();
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to divide DataFrame by scalar", ex);
        }
    }


    @Override
    public final DataFrame<R,C> divide(DataFrame<?,?> other) throws DataFrameException {
        try {
            Asserts.notNull(other, "The frame cannot be null");
            Asserts.check(frame.rowCount() == other.rowCount(), "The row counts of the two frames must match");
            Asserts.check(frame.colCount() == other.colCount(), "The column counts of the two frames must match");
            final DataFrame<R,C> result = frame.copy();
            result.cols().forEach(column -> {
                final ArrayType type = ArrayType.of(column.dataClass());
                final DataFrameColumn otherCol = other.colAt(column.ordinal());
                if (type.isInteger()) {
                    column.applyInts(v -> {
                        final int left = v.getInt();
                        final int right = otherCol.getIntAt(v.rowOrdinal());
                        return left / right;
                    });
                } else if (type.isLong()) {
                    column.applyLongs(v -> {
                        final long left = v.getLong();
                        final long right = otherCol.getLongAt(v.rowOrdinal());
                        return left / right;
                    });
                } else if (type.isDouble()) {
                    column.applyDoubles(v -> {
                        final double left = v.getDouble();
                        final double right = otherCol.getDoubleAt(v.rowOrdinal());
                        return left / right;
                    });
                } else {
                    throw new DataFrameException("Column " + column.key() + " is not a numeric type: " + type);
                }
            });
            return result;
        } catch (DataFrameException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataFrameException("Failed to divide two DataFrames", ex);
        }
    }



    /**
     * A recursive task to implement a parallel computation of a dot product between two frames
     */
    private class DotProduct extends RecursiveAction {

        private int offset;
        private int length;
        private int threshold;
        private DataFrame<?,?> left;
        private DataFrame<?,?> right;
        private DataFrame<?,?> result;

        /**
         * Constructor
         * @param left      the left frame for dot product
         * @param right     the right frame for dot product
         * @param result    the frame to write the result to
         * @param offset    the offset index
         * @param length    the number of elements to calculate
         * @param threshold the split threshold below which not to split
         */
        private DotProduct(DataFrame<?,?> left, DataFrame<?,?> right, DataFrame<?,?> result, int offset, int length, int threshold) {
            this.left = left;
            this.right = right;
            this.result = result;
            this.offset = offset;
            this.length = length;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            try {
                if (length > threshold) {
                    var halfLength = length / 2;
                    var offset1 = offset;
                    var offset2 = offset + halfLength;
                    invokeAll(
                        new DotProduct(left, right, result, offset1, halfLength, threshold),
                        new DotProduct(left, right, result, offset2, length - halfLength, threshold)
                    );
                } else {
                    var rowCount = result.rowCount();
                    var innerDim = left.colCount();
                    var cursor = result.cursor();
                    var leftRow = left.rows().cursor();
                    var rightColumn = right.cols().cursor();
                    for (int i=0; i<length; ++i) {
                        var index = offset + i;
                        var rowOrdinal = index % rowCount;
                        var colOrdinal = index / rowCount;
                        leftRow.atOrdinal(rowOrdinal);
                        rightColumn.atOrdinal(colOrdinal);
                        double value = 0d;
                        for (int k=0; k<innerDim; ++k) {
                            var v1 = leftRow.getDoubleAt(k);
                            var v2 = rightColumn.getDoubleAt(k);
                            value += v1 * v2;
                        }
                        cursor.at(rowOrdinal, colOrdinal).setDouble(value);
                    }
                }
            } catch (Exception ex) {
                throw new DataFrameException("Failed to compute dot product of two frames", ex);
            }
        }
    }
}
