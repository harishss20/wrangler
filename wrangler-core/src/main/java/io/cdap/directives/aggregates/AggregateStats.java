package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.UsageDefinition;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;

import java.util.List;

/**
 * A directive for aggregating byte sizes and time durations across rows.
 * This directive processes ByteSize and TimeDuration values from specified columns and 
 * calculates aggregated totals, converting them to standard units (MB and seconds).
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates byte sizes and time durations from specified columns")
public class AggregateStats implements Directive {
    public static final String NAME = "aggregate-stats";
    private String sizeColumn;
    private String timeColumn;
    private String totalSizeColumn;
    private String totalTimeColumn;
    private long totalBytes = 0;
    private long totalNanos = 0;
    private int rowCount = 0;

    @Override
    public UsageDefinition define() {
        // Define the directive's usage with required parameters
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("size_column", TokenType.COLUMN_NAME);
        builder.define("time_column", TokenType.COLUMN_NAME);
        builder.define("total_size_column", TokenType.COLUMN_NAME);
        builder.define("total_time_column", TokenType.COLUMN_NAME);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.sizeColumn = ((ColumnName) args.value("size_column")).value();
        this.timeColumn = ((ColumnName) args.value("time_column")).value();
        this.totalSizeColumn = ((ColumnName) args.value("total_size_column")).value();
        this.totalTimeColumn = ((ColumnName) args.value("total_time_column")).value();
        
        if (totalSizeColumn.equals(sizeColumn) || totalTimeColumn.equals(timeColumn)) {
            throw new DirectiveParseException(
                NAME, "Output columns cannot be the same as input columns");
        }
    }

    @Override
    public void destroy() {
        // Reset aggregation state
        totalBytes = 0;
        totalNanos = 0;
        rowCount = 0;
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        if (rows.isEmpty()) {
            return rows;
        }

        // Get the transient store to store running totals
        TransientStore store = context.getTransientStore();
        
        for (Row row : rows) {
            int sizeIdx = row.find(sizeColumn);
            int timeIdx = row.find(timeColumn);
            
            if (sizeIdx != -1) {
                Object sizeObj = row.getValue(sizeIdx);
                if (sizeObj != null) {
                    try {
                        if (sizeObj instanceof ByteSize) {
                            ByteSize byteSize = (ByteSize) sizeObj;
                            totalBytes += byteSize.getBytes();
                        } else {
                            throw new DirectiveExecutionException(
                                NAME, String.format("Column '%s' contains invalid byte size value: %s", 
                                sizeColumn, sizeObj));
                        }
                    } catch (Exception e) {
                        throw new DirectiveExecutionException(
                            NAME, String.format("Error processing byte size in column '%s': %s", 
                            sizeColumn, e.getMessage()));
                    }
                }
            }
            
            if (timeIdx != -1) {
                Object timeObj = row.getValue(timeIdx);
                if (timeObj != null) {
                    try {
                        if (timeObj instanceof TimeDuration) {
                            TimeDuration timeDuration = (TimeDuration) timeObj;
                            totalNanos += timeDuration.getNanoseconds();
                        } else {
                            throw new DirectiveExecutionException(
                                NAME, String.format("Column '%s' contains invalid time duration value: %s", 
                                timeColumn, timeObj));
                        }
                    } catch (Exception e) {
                        throw new DirectiveExecutionException(
                            NAME, String.format("Error processing time duration in column '%s': %s", 
                            timeColumn, e.getMessage()));
                    }
                }
            }
            
            rowCount++;
        }

        // Calculate totals in target units (MB and seconds)
        double totalSizeMB = totalBytes / (1024.0 * 1024.0);
        double totalTimeSec = totalNanos / 1_000_000_000.0;

        // Add totals to first row
        if (!rows.isEmpty()) {
            Row firstRow = rows.get(0);
            firstRow.add(totalSizeColumn, totalSizeMB);
            firstRow.add(totalTimeColumn, totalTimeSec);
        }

        return rows;
    }
}