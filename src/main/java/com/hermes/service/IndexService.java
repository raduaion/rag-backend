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
package com.hermes.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;

import com.hermes.data.CollectionState;
import com.hermes.data.DateComparator;
import com.hermes.data.PagedResult;
import com.hermes.exceptions.IndexNotFoundException;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.model.Index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IndexService {

  Mono<PagedResult<Index>> find(final String userId, final Sort.Direction direction, final int page, final int size);

  Mono<PagedResult<Index>> filter(final String userId, final String targetId, final String q, final boolean isPublicPath,
  final LocalDate date, final DateComparator dateCmp, final CollectionState state, final Sort.Direction direction, final int page, final int size);

  Flux<Index> findAllById(final List<String> ids);

  Mono<Index> getIndex(String indexId, String userId) throws IndexNotFoundException;

  Mono<Index> getIndex(String indexId) throws IndexNotFoundException;

  Mono<Index> createIndex(String name, List<String> fileIds, String userId) throws InvalidOperationException;

  Mono<Index> updateState(String indexId, String userId, boolean shared) throws IndexNotFoundException;

  Mono<Void> deleteIndex(String indexId, final String userId) throws IndexNotFoundException;

  Mono<Index> removeFile(String indexId, String userId, String fileId) throws IndexNotFoundException;

  Mono<Index> addFiles(String indexId, String userId, List<String> fileIds) throws IndexNotFoundException;

  /** Get the file details of an Index */
  Flux<Map<String, Object>> getIndexFiles(String indexId, String userId) throws IndexNotFoundException;
}
