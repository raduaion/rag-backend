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
package com.hermes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.hermes.data.CollectionState;
import com.hermes.data.DateComparator;
import com.hermes.data.PagedResult;
import com.hermes.exceptions.IndexNotFoundException;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.model.Index;
import com.hermes.service.IndexService;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/indexes")
public class IndexController {

  @Autowired
  private IndexService indexService;

  @GetMapping
  public Mono<PagedResult<Index>> find(@RequestParam(defaultValue = "DESC") final Direction direction,
  @RequestParam(defaultValue = "0") final int page, @RequestParam(defaultValue = "200") final int size,
  final Principal principal) {
    return indexService.find(principal.getName(), direction, page, size);
  }

  @GetMapping("/filter")
  public Mono<PagedResult<Index>> filter(
  @RequestParam(name = "publicpath") final boolean isPublicPath,
  @RequestParam(required = false) final String q,
  @RequestParam(required = false, name = "targetid" ) final String targetId,
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
  @RequestParam(required = false, name = "datecmp") final DateComparator dateCmp,
  @RequestParam(defaultValue = "ALL") final CollectionState state,
  @RequestParam(defaultValue = "DESC") final Direction direction,
  @RequestParam(defaultValue = "0") final int page,
  @RequestParam(defaultValue = "10") final int size,
  final Principal principal) {
    return indexService.filter(principal.getName(), targetId, q, isPublicPath, date, dateCmp, state, direction, page, size);
  }

  @PostMapping("/create")
  public Mono<Index> createIndex(@RequestParam final String name, final @RequestBody List<String> fileIds,
  final Principal principal) throws InvalidOperationException {
    return indexService.createIndex(name, fileIds, principal.getName());
  }

  @GetMapping("/{indexId}")
  public Mono<Index> getIndex(@PathVariable final String indexId, final Principal principal)
  throws IndexNotFoundException {
    return indexService.getIndex(indexId, principal.getName());
  }

  @DeleteMapping("/{indexId}")
  public Mono<Void> deleteIndex(@PathVariable final String indexId, final Principal principal) throws IndexNotFoundException {
    return indexService.deleteIndex(indexId, principal.getName());
  }

  @PostMapping("/{indexId}/updatestate")
  public Mono<Index> updateIndexState(@PathVariable final String indexId, @RequestParam final boolean shared,
  final Principal principal) throws IndexNotFoundException {
    return indexService.updateState(indexId, principal.getName(), shared);
  }

  @DeleteMapping("/{indexId}/files/{fileId}")
  public Mono<Index> removeFile(@PathVariable final String indexId, @PathVariable final String fileId,
  final Principal principal) throws IndexNotFoundException {
    return indexService.removeFile(indexId, principal.getName(), fileId);
  }

  @PostMapping("/{indexId}/addfiles")
  public Mono<Index> addFiles(@PathVariable final String indexId, @RequestBody final List<String> fileIds,
  final Principal principal) throws IndexNotFoundException {
    return indexService.addFiles(indexId, principal.getName(), fileIds);
  }

  @GetMapping("/{indexId}/getfiles")
  public Flux<Map<String, Object>> getIndexFiles(@PathVariable final String indexId, final Principal principal)
  throws IndexNotFoundException {
    return indexService.getIndexFiles(indexId, principal.getName());
  }
}
