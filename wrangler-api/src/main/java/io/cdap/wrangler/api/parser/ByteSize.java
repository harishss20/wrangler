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
 * ByteSize class represents token values for data size measurements with units (KB, MB, GB, TB).
 * It parses strings like "10KB", "1.5MB", "2GB" and provides methods to convert to bytes.
 */
@PublicEvolving
public class ByteSize implements Token {
  private String originalValue;
  private double numericValue;
  private String unit;
  
  /**
   * Constructs a ByteSize object by parsing a string with byte units.
   *
   * @param value String representation of byte size (e.g., "10KB", "1.5MB", "2GB", "1TB")
   */
  public ByteSize(String value) {
    this.originalValue = value;
    parseValue(value);
  }
  
  /**
   * Parses the byte size string into a numeric value and unit.
   *
   * @param value String representation of byte size
   */
  private void parseValue(String value) {
    // Find the point where unit starts
    int unitStartIdx = findUnitStart(value);
    if (unitStartIdx <= 0) {
      throw new IllegalArgumentException("Invalid byte size format: " + value);
    }
    
    try {
      this.numericValue = Double.parseDouble(value.substring(0, unitStartIdx));
      this.unit = value.substring(unitStartIdx).toUpperCase();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid byte size numeric value: " + value, e);
    }
    
    if (!isValidUnit(unit)) {
      throw new IllegalArgumentException("Invalid byte size unit: " + unit);
    }
  }
  
  /**
   * Finds the index where the unit part starts.
   *
   * @param value String representation of byte size
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
    return "KB".equalsIgnoreCase(unit) || 
           "MB".equalsIgnoreCase(unit) || 
           "GB".equalsIgnoreCase(unit) || 
           "TB".equalsIgnoreCase(unit);
  }
  
  /**
   * Gets the numeric value of the byte size.
   *
   * @return The numeric value
   */
  public double getNumericValue() {
    return numericValue;
  }
  
  /**
   * Gets the unit of the byte size.
   *
   * @return The unit (KB, MB, GB, TB)
   */
  public String getUnit() {
    return unit;
  }
  
  /**
   * Converts the byte size to bytes.
   * Uses 1024-based conversion (1 KB = 1024 bytes)
   *
   * @return The size in bytes
   */
  public long getBytes() {
    switch (unit.toUpperCase()) {
      case "KB":
        return (long) (numericValue * 1024);
      case "MB":
        return (long) (numericValue * 1024 * 1024);
      case "GB":
        return (long) (numericValue * 1024 * 1024 * 1024);
      case "TB":
        return (long) (numericValue * 1024 * 1024 * 1024 * 1024);
      default:
        throw new IllegalStateException("Unhandled byte unit: " + unit);
    }
  }
  
  /**
   * Converts the byte size to kilobytes.
   *
   * @return The size in kilobytes
   */
  public double getKilobytes() {
    return getBytes() / 1024.0;
  }
  
  /**
   * Converts the byte size to megabytes.
   *
   * @return The size in megabytes
   */
  public double getMegabytes() {
    return getBytes() / (1024.0 * 1024.0);
  }
  
  /**
   * Converts the byte size to gigabytes.
   *
   * @return The size in gigabytes
   */
  public double getGigabytes() {
    return getBytes() / (1024.0 * 1024.0 * 1024.0);
  }
  
  /**
   * Converts the byte size to terabytes.
   *
   * @return The size in terabytes
   */
  public double getTerabytes() {
    return getBytes() / (1024.0 * 1024.0 * 1024.0 * 1024.0);
  }
  
  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.BYTE_SIZE.name());
    object.addProperty("value", originalValue);
    object.addProperty("bytes", getBytes());
    object.addProperty("unit", unit);
    return object;
  }
  
  @Override
  public String toString() {
    return originalValue;
  }
}