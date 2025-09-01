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

import com.hermes.data.DateComparator;
import com.hermes.data.FileStatus;
import com.hermes.data.PagedResult;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FileService {

  Mono<List<String>> validateFileIds(List<String> fileIds, String userId) throws InvalidOperationException;

  Mono<String> uploadFile(MultipartFile file, String userId);

  Flux<Map<String, Object>> listFiles(String userId);

  Mono<PagedResult<Map<String, Object>>> filter(final String userId, final String q, final LocalDate date,
  final DateComparator dateCmp, final FileStatus status, final String sortBy, final Direction direction, final int page, final int size);

  Mono<String> deleteFile(String fileId, String userId);

  Mono<String> getFileChecksum(String fileId, String userId);

  Mono<Map<String, Object>> getFile(String fileId, String userId);

  Map<String, Object> getFileData(final String fileId, final String userId) throws IOException, NotFoundException;

  List<Map<String, Object>> getFileData(final Map<String, String> fileIds) throws IOException;
}
