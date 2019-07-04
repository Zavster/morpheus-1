/*
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.d3x.morpheus.TestSuite;
import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.frame.DataFrameColumn;
import com.d3x.morpheus.index.Index;
import com.d3x.morpheus.stats.Stats;
import com.d3x.morpheus.util.PerfStat;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test on the data frame of quote data.
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
public class QuoteTests {

    private static Map<String,Double> meanMap = new HashMap<>();
    private static Map<String,Double> stdDevSSMap = new HashMap<>();
    private static Map<String,Double> stdDevPPMap = new HashMap<>();
    private static Map<String,Double> medianMap = new HashMap<>();

    /**
     * Static initializer
     */
    static {
        meanMap.put("Open", 529.8729204);
        meanMap.put("High", 534.7123894);
        meanMap.put("Low", 524.2312389);
        meanMap.put("Close", 529.4233097);
        meanMap.put("Volume", 112289609.2);

        stdDevSSMap.put("Open", 70.75617642);
        stdDevSSMap.put("High", 70.77320621);
        stdDevSSMap.put("Low", 69.96391932);
        stdDevSSMap.put("Close", 70.49638631);
        stdDevSSMap.put("Volume", 53259577.17);

        stdDevPPMap.put("Open", 70.69353261);
        stdDevPPMap.put("High", 70.71054732);
        stdDevPPMap.put("Low", 69.90197693);
        stdDevPPMap.put("Close", 70.43397249);
        stdDevPPMap.put("Volume", 53212423.92);

        medianMap.put("Open", 529.06);
        medianMap.put("High", 532.75);
        medianMap.put("Low", 523.30);
        medianMap.put("Close", 529.82);
        medianMap.put("Volume", 97936300d);

    }

    @Test()
    public void testPrint() throws Exception {
        TestDataFrames.getQuotes("blk").tail(20).out().print(30);
    }


    @Test()
    public void testStats() {
        var frame = DataFrame.read().<LocalDate>csv("/quotes/quote.csv").read(options -> {
            options.setRowKeyColumnName("Date");
        });
        frame.cols().keys().forEach(columnKey -> {
            if (meanMap.containsKey(columnKey)) {
                final DataFrameColumn<LocalDate,String> column = frame.col(columnKey);
                final Stats<Double> stats1 = column.stats();
                Assert.assertEquals(stats1.mean(), meanMap.get(columnKey), 0.01, "Mean of column " + columnKey);
                Assert.assertEquals(stats1.stdDev(), stdDevSSMap.get(columnKey), 0.01, "StdDev of column " + columnKey);
                Assert.assertEquals(stats1.median(), medianMap.get(columnKey), 0.01, "Median of column " + columnKey);

                final Stats<Double> stats2 = frame.col(columnKey).stats();
                Assert.assertEquals(stats2.mean(), meanMap.get(columnKey), 0.01, "Mean of column " + columnKey);
                Assert.assertEquals(stats2.stdDev(), stdDevSSMap.get(columnKey), 0.01, "StdDev of column " + columnKey);
                Assert.assertEquals(stats2.median(), medianMap.get(columnKey), 0.01, "Median of column " + columnKey);
            }
        });
    }


    @Test()
    public void testRowStats() {
        var frame = DataFrame.read().<LocalDate>csv("/quotes/quote.csv").read(options -> {
            options.setRowKeyColumnName("Date");
        });
        for (int i=0; i<10; ++i) {
            final long t1 = System.currentTimeMillis();
            final double[] sum = new double[1];
            frame.rows().keys().forEach(rowKey -> {
                final Stats<Double> stats = frame.row(rowKey).stats();
                final double mean = stats.mean();
                final double stddev = stats.stdDev();
                final double min = stats.min();
                final double max = stats.max();
                sum[0] = mean + stddev + min + max;

            });
            final long t2 = System.currentTimeMillis();
            System.out.println("Completed " + frame.rows().count() + " rows in " + (t2-t1) + " millis");
        }
    }


    @Test()
    public void testColumnStats()  {
        var frame = DataFrame.read().<LocalDate>csv("/quotes/quote.csv").read(options -> {
            options.setRowKeyColumnName("Date");
        });
        PerfStat.timeInMicros("Column stats", 20, () -> {
            final double[] sum = new double[1];
            frame.cols().keys().forEach(columnKey -> {
                final Stats<Double> stats = frame.col(columnKey).stats();
                final double mean = stats.mean();
                final double stddev = stats.stdDev();
                final double min = stats.min();
                final double max = stats.max();
                sum[0] = mean + stddev + min + max;
            });
            return frame;
        });
    }

    public void testRowDemean() {
        var frame = DataFrame.read().<LocalDate>csv("/quotes/quote.csv").read(options -> {
            options.setRowKeyColumnName("Date");
        });
        PerfStat.timeInMillis("Row demean", 20, () -> {
            frame.rows().keys().forEach(rowKey -> {
                final double mean = frame.row(rowKey).stats().mean();
                frame.row(rowKey).applyValues(v -> v.getDouble() - mean);
            });
            return frame;
        });
    }


    @Test()
    public void testCrossSectionalReturns() throws Exception {
        LocalDate startDate = LocalDate.MIN;
        LocalDate endDate = LocalDate.MAX;
        var rowKeys = Index.of(LocalDate.class, 100);
        var tickers = Index.ofObjects("BLK", "CSCO", "SPY", "YHOO", "VNQI", "VGLT", "VCLT");
        var closePrices = DataFrame.ofDoubles(rowKeys, tickers);
        for (String ticker : tickers) {
            System.out.println("Loading data for ticker " + ticker);
            var quotes = TestDataFrames.getQuotes(ticker);
            quotes.tail(10).out().print();
            closePrices.rows().addAll(quotes.rows().keyArray());
            var firstKey = quotes.rows().firstKey().get();
            var lastKey = quotes.rows().lastKey().get();
            startDate = firstKey.isAfter(startDate) ? firstKey : startDate;
            endDate = lastKey.isBefore(endDate) ? lastKey : endDate;
            quotes.rows().forEach(row -> {
                final LocalDate date = row.key();
                final double price = row.getDouble("Adj Close");
                closePrices.setDouble(date, ticker, price);
            });
        }

        final Set<LocalDate> nanDates = new HashSet<>();
        closePrices.rows().forEach(row -> row.forEachValue(v -> {
            final double value = v.getDouble();
            if (Double.isNaN(value)) {
                final LocalDate rowKey = row.key();
                nanDates.add(rowKey);
                if (rowKey.getYear() == 2014) {
                    System.out.println(rowKey);
                }
            }
        }));

        var selection = closePrices.rows().select(row -> !nanDates.contains(row.key()));
        var sorted = selection.rows().sort((row1, row2) -> row1.key().compareTo(row2.key()));
        var returns = sorted.calc().percentChanges();
        returns.rows().first().get().applyDoubles(v -> 0d);
        returns.head(10).out().print();
        returns.cols().stats().correlation().out().print();
    }


    @Test()
    public void testSimpleMovingAverage() throws Exception {
        var file = TestSuite.getOutputFile("quote-tests", "sma.csv");
        var quotes = TestDataFrames.getQuotes("blk");
        var prices = quotes.cols().select(column -> !column.key().equalsIgnoreCase("Volume"));
        var sma = prices.calc().sma(50).cols().mapKeys(col -> col.key() + "(SMA)");
        sma.update(prices, false, true);
        sma.write().csv(file).apply();
    }

}
