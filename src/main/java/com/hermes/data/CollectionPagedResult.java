/*
 * Copyright 2025 Aion Sigma Inc.
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
package com.hermes.data;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for Collections (DTO)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionPagedResult<T> extends PagedResult<T> {

  private List<Map<String, Object>> files;

  public CollectionPagedResult(final List<T> content, final long totalElements, final int pageSize,
  final int currentPage, final List<Map<String, Object>> files) {

    super(content, totalElements, pageSize, currentPage);
    this.files = files;
  }
}
