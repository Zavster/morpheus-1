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
package com.d3x.morpheus.reference;

import java.io.IOException;

import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.frame.DataFrameAsserts;
import com.d3x.morpheus.stats.StatType;
import com.d3x.morpheus.index.Index;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * A unit test for expanding window statistics in both the row and column dimension
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
public class StatsExpandingTests {

    @DataProvider(name="style")
    public Object[][] style() {
        return new Object[][] { {false}, {true} };
    }

    private DataFrame<Integer,String> loadSourceData() throws IOException {
        return DataFrame.read().csv("/stats-expanding/source-data.csv").read();
    }


    private DataFrame<Integer,String> loadExpectedRowStats(StatType stat) throws IOException {
        switch (stat) {
            case MIN:           return DataFrame.read().csv("/stats-expanding/row-min.csv").read();
            case MAX:           return DataFrame.read().csv("/stats-expanding/row-max.csv").read();
            case SUM:           return DataFrame.read().csv("/stats-expanding/row-sum.csv").read();
            case MEAN:          return DataFrame.read().csv("/stats-expanding/row-mean.csv").read();
            case COUNT:         return DataFrame.read().csv("/stats-expanding/row-count.csv").read();
            case SKEWNESS:      return DataFrame.read().csv("/stats-expanding/row-skew.csv").read();
            case KURTOSIS:      return DataFrame.read().csv("/stats-expanding/row-kurt.csv").read();
            case VARIANCE:      return DataFrame.read().csv("/stats-expanding/row-var.csv").read();
            case STD_DEV:       return DataFrame.read().csv("/stats-expanding/row-std.csv").read();
            case MEDIAN:        return DataFrame.read().csv("/stats-expanding/row-median.csv").read();
            case PERCENTILE:    return DataFrame.read().csv("/stats-expanding/row-percentile-80th.csv").read();
            default:    throw new IllegalArgumentException("Unexpected stat type: " + stat);
        }
    }

    private DataFrame<Integer,String> loadExpectedColStats(StatType stat) throws IOException {
        switch (stat) {
            case MIN:           return DataFrame.read().csv("/stats-expanding/column-min.csv").read();
            case MAX:           return DataFrame.read().csv("/stats-expanding/column-max.csv").read();
            case SUM:           return DataFrame.read().csv("/stats-expanding/column-sum.csv").read();
            case MEAN:          return DataFrame.read().csv("/stats-expanding/column-mean.csv").read();
            case COUNT:         return DataFrame.read().csv("/stats-expanding/column-count.csv").read();
            case SKEWNESS:      return DataFrame.read().csv("/stats-expanding/column-skew.csv").read();
            case KURTOSIS:      return DataFrame.read().csv("/stats-expanding/column-kurt.csv").read();
            case VARIANCE:      return DataFrame.read().csv("/stats-expanding/column-var.csv").read();
            case STD_DEV:       return DataFrame.read().csv("/stats-expanding/column-std.csv").read();
            case MEDIAN:        return DataFrame.read().csv("/stats-expanding/column-median.csv").read();
            case PERCENTILE:    return DataFrame.read().csv("/stats-expanding/column-percentile-80th.csv").read();
            default:    throw new IllegalArgumentException("Unexpected stat type: " + stat);
        }
    }


    @Test(dataProvider = "style")
    public void expandingCount(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> rawRowStats = loadExpectedRowStats(StatType.COUNT);
        final DataFrame<Integer,String> rawColStats = loadExpectedColStats(StatType.COUNT);
        final DataFrame<Integer,String> expectedRowStats = DataFrame.ofDoubles(Index.of(source.rows().keyArray()), Index.of(source.cols().keyArray()));
        final DataFrame<Integer,String> expectedColStats = DataFrame.ofDoubles(Index.of(source.rows().keyArray()), Index.of(source.cols().keyArray()));
        expectedRowStats.applyDoubles(v -> rawRowStats.getDoubleAt(v.rowOrdinal(), v.colOrdinal()));
        expectedColStats.applyDoubles(v -> rawColStats.getDoubleAt(v.rowOrdinal(), v.colOrdinal()));
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).count();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).count();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).count();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).count();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingMin(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.MIN);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.MIN);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).min();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).min();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).min();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).min();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingMax(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.MAX);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.MAX);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).max();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).max();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).max();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).max();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingSum(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.SUM);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.SUM);
        expectedRowStats.out().print();
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).sum();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).sum();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).sum();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).sum();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingMean(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.MEAN);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.MEAN);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).mean();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).mean();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).mean();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).mean();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingSkew(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.SKEWNESS);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.SKEWNESS);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).skew();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).skew();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).skew();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).skew();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingKurtosis(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.KURTOSIS);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.KURTOSIS);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).kurtosis();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).kurtosis();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).kurtosis();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).kurtosis();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingVariance(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.VARIANCE);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.VARIANCE);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).variance();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).variance();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).variance();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).variance();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingStdDev(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.STD_DEV);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.STD_DEV);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).stdDev();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).stdDev();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).stdDev();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).stdDev();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingMedian(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.MEDIAN);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.MEDIAN);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).median();
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).median();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).median();
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).median();
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }


    @Test(dataProvider = "style")
    public void expandingPercentile(boolean parallel) throws Exception {
        final DataFrame<Integer,String> source = loadSourceData();
        final DataFrame<Integer,String> expectedRowStats = loadExpectedRowStats(StatType.PERCENTILE);
        final DataFrame<Integer,String> expectedColStats = loadExpectedColStats(StatType.PERCENTILE);
        if (parallel) {
            final DataFrame<Integer,String> rowStats = source.rows().parallel().stats().expanding(20).percentile(0.8);
            final DataFrame<Integer,String> colStats = source.cols().parallel().stats().expanding(20).percentile(0.8);
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        } else {
            final DataFrame<Integer,String> rowStats = source.rows().sequential().stats().expanding(20).percentile(0.8);
            final DataFrame<Integer,String> colStats = source.cols().sequential().stats().expanding(20).percentile(0.8);
            DataFrameAsserts.assertEqualsByIndex(expectedRowStats, rowStats);
            DataFrameAsserts.assertEqualsByIndex(expectedColStats, colStats);
        }
    }

}
