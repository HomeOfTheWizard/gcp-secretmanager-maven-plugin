/*
 * Copyright 2017 Decipher Technology Studios LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.homeofthewizard.maven.plugins.gcp.secretmanager.config;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Represents a mapping between a Vault key and a Maven property.
 */
public class ComplexMapping implements Serializable {

  private String key;
  private List<Mapping> mappings;

  /**
   * Initializes a new instance of the {@link ComplexMapping} class.
   */
  public ComplexMapping() { }

  /**
   * Initializes a new instance of the {@link ComplexMapping} class.
   *
   * @param key the key
   * @param mappings the path
   */
  public ComplexMapping(String key, List<Mapping> mappings) {
    this.key = key;
    this.mappings = mappings;
  }

  /**
   * Gets the key for this mapping.
   *
   * @return the key
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Gets the property for this mapping.
   *
   * @return the property
   */
  public List<Mapping> getMappings() {
    return this.mappings;
  }

  /**
   * Returns a hash code value for this mapping.
   *
   * @return the hash code
   */
  public int hashCode() {
    return Objects.hash(this.key, this.mappings);
  }

  /**
   * Returns a value indicating whether this mapping is equal to another object.
   *
   * @return {@code true} if the this mapping is equal to the object; otherwise, {@code false}
   */
  public boolean equals(Object object) {
    if (object instanceof ComplexMapping that) {
        return Objects.equals(this.key, that.key)
          && new HashSet<>(mappings).containsAll(this.mappings);
    }
    return false;
  }

}
