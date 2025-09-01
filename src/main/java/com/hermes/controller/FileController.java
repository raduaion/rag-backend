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

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.hermes.data.DateComparator;
import com.hermes.data.FileStatus;
import com.hermes.data.PagedResult;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.service.FileService;

import io.swagger.v3.oas.annotations.Operation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/files")
public class FileController {

  @Autowired
  private FileService fileService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
    summary = "Upload files",
    description = "Uploads up to 10 files to Google Cloud Storage"
  )
  public String uploadFiles(@RequestParam final List<MultipartFile> files, final Principal principal) {

    if (files.size() > 10) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only upload a maximum of 10 files at a time.");
    }

    final String userId = principal.getName();
    final List<String> uploadedFiles = new ArrayList<>();
    for (MultipartFile file: files) {
      uploadedFiles.add(fileService.uploadFile(file, userId).block());
    }

    return "Files uploaded successfully: " + String.join(", ", uploadedFiles);
  }

  @GetMapping("/list")
  public Flux<Map<String, Object>> listFiles(final Principal principal) {
    return fileService.listFiles(principal.getName());
  }

  @GetMapping("/filter")
  public Mono<PagedResult<Map<String, Object>>> filter(
  @RequestParam(required = false) final String q,
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
  @RequestParam(required = false, name = "datecmp") final DateComparator dateCmp,
  @RequestParam(defaultValue = "ALL") final FileStatus status,
  @RequestParam(required = false, name = "sortby") final String sortBy,
  @RequestParam(defaultValue = "DESC") final Direction direction,
  @RequestParam(defaultValue = "0") final int page,
  @RequestParam(defaultValue = "10") final int size,
  final Principal principal) {
    return fileService.filter(principal.getName(), q, date, dateCmp, status, sortBy, direction, page, size);
  }

  @GetMapping("/{fileId}")
  public Mono<Map<String, Object>> getFile(@PathVariable final String fileId, final Principal principal) {
    return fileService.getFile(fileId, principal.getName());
  }

  @DeleteMapping("/{fileId}")
  public Mono<String> deleteFile(@PathVariable final String fileId, final Principal principal) {
    return fileService.deleteFile(fileId, principal.getName())
      .onErrorResume(e -> Mono.error(new InvalidOperationException("Error deleting file: " + e.getMessage())));
  }
}
