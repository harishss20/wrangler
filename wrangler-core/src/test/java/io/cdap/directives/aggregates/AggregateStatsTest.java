/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */



package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link AggregateStats}
 */
public class AggregateStatsTest {

    @Test
    public void testBasicAggregation() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        // Create test data
        List<Row> rows = Arrays.asList(
            createRow("10MB", "1.5s"),
            createRow("20MB", "2.5s"),
            createRow("15MB", "1.0s")
        );

        // Execute directives
        List<Row> results = TestingRig.execute(directives, rows);

        // Verify results
        Assert.assertEquals(3, results.size());
        Row firstRow = results.get(0);
        double totalSizeMB = (Double) firstRow.getValue("total_size_mb");
        double totalTimeSec = (Double) firstRow.getValue("total_time_sec");
        
        Assert.assertEquals(45.0, totalSizeMB, 0.001); // 10 + 20 + 15 = 45MB
        Assert.assertEquals(5.0, totalTimeSec, 0.001); // 1.5 + 2.5 + 1.0 = 5.0s
    }

    @Test
    public void testMixedUnits() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList(
            createRow("1024KB", "1500ms"), // 1MB, 1.5s
            createRow("2GB", "0.5s"),      // 2048MB, 0.5s
            createRow("512KB", "2000ms")   // 0.5MB, 2.0s
        );

        List<Row> results = TestingRig.execute(directives, rows);
        Assert.assertEquals(3, results.size());
        
        Row firstRow = results.get(0);
        double totalSizeMB = (Double) firstRow.getValue("total_size_mb");
        double totalTimeSec = (Double) firstRow.getValue("total_time_sec");

        Assert.assertEquals(2049.5, totalSizeMB, 0.001); // 1 + 2048 + 0.5 = 2049.5MB
        Assert.assertEquals(4.0, totalTimeSec, 0.001);   // 1.5 + 0.5 + 2.0 = 4.0s
    }

    @Test
    public void testEmptyRows() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList();
        List<Row> results = TestingRig.execute(directives, rows);
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testNullValues() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList(
            createRow("10MB", null),
            createRow(null, "1.5s"),
            createRow("5MB", "2.5s")
        );

        List<Row> results = TestingRig.execute(directives, rows);
        Assert.assertEquals(3, results.size());
        
        Row firstRow = results.get(0);
        double totalSizeMB = (Double) firstRow.getValue("total_size_mb");
        double totalTimeSec = (Double) firstRow.getValue("total_time_sec");

        Assert.assertEquals(15.0, totalSizeMB, 0.001); // 10 + 0 + 5 = 15MB
        Assert.assertEquals(4.0, totalTimeSec, 0.001); // 0 + 1.5 + 2.5 = 4.0s
    }

    @Test
    public void testUnitConversions() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList(
            createRow("1024KB", "1000ms"),  // 1MB, 1sec
            createRow("1MB", "2000000us"),  // 1MB, 2sec
            createRow("0.001GB", "180000ms") // ~1MB, 180sec
        );

        List<Row> results = TestingRig.execute(directives, rows);
        Assert.assertEquals(3, results.size());
        
        Row firstRow = results.get(0);
        double totalSizeMB = (Double) firstRow.getValue("total_size_mb");
        double totalTimeSec = (Double) firstRow.getValue("total_time_sec");

        // Each input is roughly 1MB, so total should be ~3MB
        Assert.assertEquals(3.0, totalSizeMB, 0.001);
        // Time inputs convert to 1 + 2 + 180 = 183 seconds
        Assert.assertEquals(183.0, totalTimeSec, 0.001);
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testInvalidInput() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList(
            createRow("invalid", "1.5s")
        );

        TestingRig.execute(directives, rows);
    }

    @Test(expected = DirectiveParseException.class)
    public void testSameInputOutputColumns() throws Exception {
        String[] directives = new String[]{
            "aggregate-stats :data_size :response_time data_size response_time"
        };

        List<Row> rows = Arrays.asList(
            createRow("10MB", "1.5s")
        );

        TestingRig.execute(directives, rows);
    }

    private Row createRow(String size, String time) {
        Row row = new Row();
        if (size != null) {
            row.add("data_size", new ByteSize(size));
        } else {
            row.add("data_size", null);
        }
        if (time != null) {
            row.add("response_time", new TimeDuration(time));
        } else {
            row.add("response_time", null);
        }
        return row;
    }
}