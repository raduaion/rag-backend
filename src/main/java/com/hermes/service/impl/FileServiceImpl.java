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
package com.hermes.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.hermes.data.DateComparator;
import com.hermes.data.FileStatus;
import com.hermes.data.PagedResult;
import com.hermes.exceptions.FileInUseException;
import com.hermes.exceptions.InvalidFilesException;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.exceptions.UploadFailedException;
import com.hermes.repository.IndexRepository;
import com.hermes.service.FileService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

  @Value("${gcp.bucket.name}")
  private String bucketName;

  @Value("${gcp.credentials}")
  private String credentialsFile;

  private Storage storage;

  @Autowired
  private IndexRepository indexRepository;

  private void initStorage() throws IOException {
    if (storage == null) {
      storage = StorageOptions.newBuilder()
        .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsFile)))
        .build()
        .getService();
    }
  }

  @Override
  public Mono<List<String>> validateFileIds(final List<String> fileIds, final String userId) throws InvalidOperationException {

    final List<String> invalidFileIds = new ArrayList<>();

    final String folder = "users/" + userId + "/";
    for (String fileId : fileIds) {
      if (!fileExistsInStorage(folder + fileId)) {
        invalidFileIds.add(fileId);
      }
    }

    if (!invalidFileIds.isEmpty()) {
      return Mono.error(new InvalidFilesException());
    }

    return Mono.just(fileIds);
  }

  @Override
  public Mono<String> uploadFile(final MultipartFile file, final String userId) {

    try {

      initStorage();

      final String folder = "users/" + userId + "/",
      fileName = UUID.randomUUID() + "-" + file.getOriginalFilename(),
      fullPath = folder + fileName;

      final BlobId blobId = BlobId.of(bucketName, fullPath);
      final BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
      storage.create(blobInfo, file.getBytes());
      return Mono.just(fileName);
    }
    catch (IOException e) {
      return Mono.error(new UploadFailedException("Error uploading file: " + e.getMessage()));
    }
  }

  @Override
  public Flux<Map<String, Object>> listFiles(final String userId) {

    try {

      initStorage();

      final String folder = "users/" + userId + "/";
      final Iterable<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(folder)).iterateAll();
      final List<Map<String, Object>> fileDetails = new ArrayList<>();

      for (final Blob blob : blobs) {

        final String fullPath = blob.getName();
        if (fullPath.startsWith(folder)) {
          fileDetails.add(getFileDetails(blob, folder));
        }
      }

      return Flux.fromIterable(fileDetails);
    }
    catch (IOException e) {
      log.error("{}", e);
      return Flux.error(new InvalidOperationException("Error listing files"));
    }
  }

  /**
   * Gets a paginated, sorted, and filtered list of files for a given user.
   * 
   * <p>The filters are applied here as Google Cloud Storage does not offer built-in query capabilities for metadata
   */
  @Override
  public Mono<PagedResult<Map<String, Object>>> filter(final String userId, final String q, final LocalDate date,
  final DateComparator dateCmp, final FileStatus status, final String sortBy, final Direction direction,
  final int page, final int size) {

    try {

      initStorage();
      final String folder = "users/" + userId + "/";

      // Step 1: List blobs
      List<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(folder))
        .streamAll()

      // Step 2: Aply filtering/searching
        .filter(blob -> {

          final boolean startWithPathOk = blob.getName().startsWith(folder); // Is this necessary?

          boolean searchTextOk = true;
          if (q != null && !q.trim().isEmpty()) {

            final String keywords = blob.getMetadata().get("keywords");
            searchTextOk = blob.getName().toLowerCase().contains(q.toLowerCase())
              || (keywords != null ? keywords.toLowerCase().contains(q.toLowerCase()) : false);
          }

          boolean statusOk = true;
          if (status != FileStatus.ALL) {
            statusOk = getFileStatus(blob) == status;
          }

          final DateComparator cmp = dateCmp == null ? DateComparator.EQ : dateCmp;
          boolean dateOk = true;
          if (date != null && cmp != DateComparator.NONE) {
            dateOk = compareDateFilter(blob.getCreateTimeOffsetDateTime().toLocalDate(), date, cmp);
          }

          return startWithPathOk && searchTextOk && statusOk && dateOk;
        })
        .collect(Collectors.toList());

      // Get the total items
      final int totalElements = blobs.size();

      // Step 3: Apply sorting
      if (sortBy != null) {
        final Comparator<Blob> comparator = switch (sortBy) {
          case "name" -> Comparator.comparing(
            blob -> this.extractOriginalName(blob.getName().substring(folder.length())),
            String.CASE_INSENSITIVE_ORDER
          );
          case "size" -> Comparator.comparing(Blob::getSize);
          case "updateTime" -> Comparator.comparing(Blob::getUpdateTimeOffsetDateTime, Comparator.nullsLast(OffsetDateTime::compareTo));
          case "createTime" -> Comparator.comparing(Blob::getCreateTimeOffsetDateTime, Comparator.nullsLast(OffsetDateTime::compareTo));
          default -> null;
        };
        if (comparator != null) {
          blobs.sort(direction == Direction.ASC ? comparator : comparator.reversed());
        }
      }

      // Step 4: Apply paging
      final List<Map<String, Object>> fileDetails = blobs.stream()
        .skip((long) page * size)
        .limit(size)

      // Step 5: Get file details
        .map(blob -> getFileDetails(blob, folder))
        .toList();

      return Mono.just(new PagedResult<>(
        fileDetails,
        totalElements,
        size,
        page
      ));
    }
    catch (IOException e) {
      log.error("{}", e);
      return Mono.error(new InvalidOperationException("Error listing files"));
    }
  }

  @Override
  public Mono<String> deleteFile(final String fileId, final String userId) {

    return indexRepository.findAll()
    .filter(index -> index.getFiles().containsKey(fileId))
    .hasElements()
    .flatMap(isInIndex -> {
      if (isInIndex) {
        return Mono.error(new FileInUseException());
      }

      try {
        initStorage();
        final String folder = "users/" + userId + "/",
        fullPath = folder + fileId;

        final Blob blob = storage.get(bucketName, fullPath);
        if (blob == null || !blob.exists()) {
          return Mono.error(new InvalidOperationException("File not found"));
        }

        final boolean deleted = storage.delete(blob.getBlobId());
        if (deleted) {
          return Mono.just("File deleted successfully");
        }
        else {
          return Mono.error(new InvalidOperationException("Error deleting file"));
        }
      }
      catch (IOException e) {
        return Mono.error(new InvalidOperationException("Error initializing storage: " + e.getMessage()));
      }
    });
  }

  @Override
  public Mono<String> getFileChecksum(final String fileId, final String userId) {

    try {

      initStorage();

      final String folder = "users/" + userId + "/",
      fullPath = folder + fileId;
      final Blob blob = storage.get(bucketName, fullPath);
      if (blob == null || !blob.exists()) {
        return Mono.error(new InvalidOperationException("File not found"));
      }
      return Mono.just(blob.getMd5ToHexString());
    }
    catch (IOException e) {
      return Mono.error(new InvalidOperationException("Error initializing storage: " + e.getMessage()));
    }
  }

  @Override
  public Mono<Map<String, Object>> getFile(final String fileId, final String userId) {

    try {
      return Mono.just(getFileData(fileId, userId));
    }
    catch (NotFoundException e) {
      return Mono.error(new NotFoundException(e.getMessage()));
    }
    catch (IOException e) {
      return Mono.error(new InvalidOperationException("Error initializing storage: " + e.getMessage()));
    }
  }

  @Override
  public Map<String, Object> getFileData(final String fileId, final String userId) throws IOException, NotFoundException {

    initStorage();

    final String folder = "users/" + userId + "/",
    fullPath = folder + fileId;
    final Blob blob = storage.get(bucketName, fullPath);

    if (blob == null || !blob.exists()) {
      throw new NotFoundException("File not found");
    }

    return getFileDetails(blob, folder);
  }

  @Override
  public List<Map<String, Object>> getFileData(final Map<String, String> fileIds) throws IOException {

    initStorage();

    List<BlobId> blobIds = new ArrayList<>();

    fileIds.forEach((fileId, userId) -> {
      final String folder = "users/" + userId + "/";
      blobIds.add(BlobId.of(bucketName, folder + fileId));
    });

    final List<Blob> blobs = storage.get(blobIds);

    final List<Map<String, Object>> result = new ArrayList<>();
    String[] parts;

    for (final Blob blob: blobs) {
      parts = blob.getName().split("/");
      if (parts.length > 1) {
        result.add(getFileDetails(blob, String.format("%s/%s/", parts[0], parts[1])));
      } 
    }

    return result;
  }

  private boolean fileExistsInStorage(final String fileId) throws InvalidOperationException {

    try {
      initStorage();
      final Blob blob = storage.get(bucketName, fileId);
      return blob != null && blob.exists();
    }
    catch (IOException e) {
      throw new InvalidOperationException("Error initializing storage: " + e.getMessage());
    }
  }

  private String humanReadableByteCount(final long bytes) {

    if (bytes < 1024) return bytes + " B";
    final int exp = (int) (Math.log(bytes) / Math.log(1024));
    final char pre = "KMGTPE".charAt(exp - 1);
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
  }

  private Map<String, Object> getFileDetails(final Blob blob, final String folder) {

    final String fileName = blob.getName().substring(folder.length());

    final Map<String, Object> details = new HashMap<>();
    details.put("name", fileName);
    details.put("originalName", this.extractOriginalName(fileName));
    details.put("dateUploaded", blob.getCreateTimeOffsetDateTime().toEpochSecond());
    details.put("sizeInBytes", blob.getSize());
    details.put("sizeReadable", humanReadableByteCount(blob.getSize()));
    details.put("checksum", blob.getMd5ToHexString());
    details.put("metadata", blob.getMetadata());

    return details;
  }

  /**
   * Extract original name of the file from blob filename
   * 
   * @param fileName
   * @return
   */
  private String extractOriginalName(final String fileName) {
    return fileName.substring(fileName.indexOf("-") + 1).split("-", 5)[4];
  }

  /**
   * Get File status from Metadata
   * @param blob
   * @return
   */
  private FileStatus getFileStatus(final Blob blob) {

    try {
      final String statusStr = blob.getMetadata().get("status");
      return statusStr == null ? FileStatus.PENDING : FileStatus.valueOf(statusStr.toUpperCase());
    } catch (Exception e) {
      return FileStatus.PENDING;
    }
  }

  /**
   * Compare dates using a comparator
   * 
   * @param date
   * @param chosenDate
   * @param dateComp
   * @return true if chosenDate corresponds 
   */
  private boolean compareDateFilter (final LocalDate date, final LocalDate chosenDate, final DateComparator dateComp) {

    return switch (dateComp) {
      case DIFF -> date.isBefore(chosenDate) || date.isAfter(chosenDate);

      case GT -> date.isAfter(chosenDate);

      case GTE -> date.equals(chosenDate) || date.isAfter(chosenDate);

      case LT -> date.isBefore(chosenDate);

      case LTE -> date.equals(chosenDate) || date.isBefore(chosenDate);

      case EQ -> date.equals(chosenDate);
    
      default -> false;
    };
  }
}
