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

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * TimeDuration class represents token values for time duration measurements with units (ms, s, min, h).
 * It parses strings like "100ms", "1.5s", "2min", "1h" and provides methods to convert to different time units.
 */
@PublicEvolving
public class TimeDuration implements Token {
  private String originalValue;
  private double numericValue;
  private String unit;
  
  /**
   * Constructs a TimeDuration object by parsing a string with time units.
   *
   * @param value String representation of time duration (e.g., "100ms", "1.5s", "2min", "1h")
   */
  public TimeDuration(String value) {
    this.originalValue = value;
    parseValue(value);
  }
  
  /**
   * Parses the time duration string into a numeric value and unit.
   *
   * @param value String representation of time duration
   */
  private void parseValue(String value) {
    // Find the point where unit starts
    int unitStartIdx = findUnitStart(value);
    if (unitStartIdx <= 0) {
      throw new IllegalArgumentException("Invalid time duration format: " + value);
    }
    
    try {
      this.numericValue = Double.parseDouble(value.substring(0, unitStartIdx));
      this.unit = value.substring(unitStartIdx).toLowerCase();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid time duration numeric value: " + value, e);
    }
    
    if (!isValidUnit(unit)) {
      throw new IllegalArgumentException("Invalid time duration unit: " + unit);
    }
  }
  
  /**
   * Finds the index where the unit part starts.
   *
   * @param value String representation of time duration
   * @return Index where the unit starts
   */
  private int findUnitStart(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (!Character.isDigit(c) && c != '.') {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * Checks if the unit is valid.
   *
   * @param unit The unit to check
   * @return True if the unit is valid, false otherwise
   */
  private boolean isValidUnit(String unit) {
    return "ms".equalsIgnoreCase(unit) || 
           "s".equalsIgnoreCase(unit) || 
           "min".equalsIgnoreCase(unit) || 
           "h".equalsIgnoreCase(unit);
  }
  
  /**
   * Gets the numeric value of the time duration.
   *
   * @return The numeric value
   */
  public double getNumericValue() {
    return numericValue;
  }
  
  /**
   * Gets the unit of the time duration.
   *
   * @return The unit (ms, s, min, h)
   */
  public String getUnit() {
    return unit;
  }
  
  /**
   * Converts the time duration to milliseconds.
   *
   * @return The duration in milliseconds
   */
  public long getMilliseconds() {
    switch (unit.toLowerCase()) {
      case "ms":
        return (long) numericValue;
      case "s":
        return (long) (numericValue * 1000);
      case "min":
        return (long) (numericValue * 60 * 1000);
      case "h":
        return (long) (numericValue * 60 * 60 * 1000);
      default:
        throw new IllegalStateException("Unhandled time unit: " + unit);
    }
  }
  
  /**
   * Converts the time duration to seconds.
   *
   * @return The duration in seconds
   */
  public double getSeconds() {
    return getMilliseconds() / 1000.0;
  }
  
  /**
   * Converts the time duration to minutes.
   *
   * @return The duration in minutes
   */
  public double getMinutes() {
    return getMilliseconds() / (60.0 * 1000.0);
  }
  
  /**
   * Converts the time duration to hours.
   *
   * @return The duration in hours
   */
  public double getHours() {
    return getMilliseconds() / (60.0 * 60.0 * 1000.0);
  }
  
  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.TIME_DURATION.name());
    object.addProperty("value", originalValue);
    object.addProperty("milliseconds", getMilliseconds());
    object.addProperty("unit", unit);
    return object;
  }
  
  @Override
  public String toString() {
    return originalValue;
  }
}