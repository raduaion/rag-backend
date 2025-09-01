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
package com.hermes.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.hermes.model.Index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface IndexRepository extends FirestoreReactiveRepository<Index> {

  Flux<Index> findByCreatedBy(final String createdBy, Pageable pageable);

  Mono<Long> countByCreatedBy(String createdBy);
  
  Mono<Index> findByIdAndCreatedBy(final String id, final String createdBy);

  Flux<Index> findByShared(final boolean shared, Pageable pageable);

  Mono<Long> countByShared(final boolean shared);

  Mono<Index> findByLowercaseName(final String name);
}
