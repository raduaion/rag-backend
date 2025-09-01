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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Value;
import com.google.firestore.v1.StructuredQuery.Filter;
import com.google.firestore.v1.StructuredQuery.Order;
import com.google.protobuf.Int32Value;
import com.google.firestore.v1.StructuredQuery.Direction;
import com.google.firestore.v1.StructuredQuery.FieldFilter;
import com.google.firestore.v1.StructuredQuery.FieldReference;
import com.hermes.data.CollectionPagedResult;
import com.hermes.data.CollectionState;
import com.hermes.data.DateComparator;
import com.hermes.data.PagedResult;
import com.hermes.exceptions.IndexNotFoundException;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.model.Conversation;
import com.hermes.model.Index;
import com.hermes.model.Message;
import com.hermes.repository.ConversationRepository;
import com.hermes.repository.IndexRepository;
import com.hermes.service.FileService;
import com.hermes.service.IndexService;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class IndexServiceImpl implements IndexService {

  @Autowired
  private IndexRepository indexRepository;

  @Autowired
  private ConversationRepository conversationRepository;

  @Autowired
  private FileService fileService;

  @Autowired
  private FirestoreTemplate firestoreTemplate;

  private static final String CREATED_AT_FIELD = "createdAt";

  private static final String SHARED_FIELD = "shared";

  private static final String CREATED_BY_FIELD = "createdBy";

  private static final String LOWERCASE_NAME_FIELD = "lowercaseName";

  @Override
  public Mono<PagedResult<Index>> find(final String userId, final Sort.Direction direction, final int page, final int size) {

    final Sort sort = Sort.by(direction, CREATED_AT_FIELD); // Create Composite Indexes before using sorting
    final Pageable pageable = PageRequest.of(page, size, sort);

    final Flux<Index> data = indexRepository.findByCreatedBy(userId, pageable);
    final Mono<Long> totalCountMono = indexRepository.countByCreatedBy(userId);

    return Mono.zip(data.collectList(), totalCountMono)
      .map(tuple -> new CollectionPagedResult<>(
        tuple.getT1(),
        tuple.getT2(),
        size,
        page,
        getAllFiles(tuple.getT1())
      ));
  }

  /**
   * Gets a paginated, sorted, and filtered list of indexes for a given user.
   */
  @Override
  public Mono<PagedResult<Index>> filter(final String userId, final String targetId, final String q, final boolean isPublicPath,
  final LocalDate date, final DateComparator dateCmp, final CollectionState state, final Sort.Direction direction, final int page, final int size) {

    final List<Filter> filters = new ArrayList<>();

    final String createdById = isPublicPath
      ? (targetId != null && !targetId.isEmpty() ? targetId : null)
      : userId;

    // Apply createdBy filter
    if (createdById != null) {
      filters.add(Filter.newBuilder()
        .setFieldFilter(StructuredQuery.FieldFilter.newBuilder()
        .setField(FieldReference.newBuilder().setFieldPath(CREATED_BY_FIELD))
        .setOp(StructuredQuery.FieldFilter.Operator.EQUAL)
        .setValue(Value.newBuilder().setStringValue(createdById)))
        .build()
      );
    }

    final Boolean sharedData = isPublicPath
      ? Boolean.TRUE
      : (state == CollectionState.ALL ? null : state == CollectionState.PUBLIC);

    // Apply 'shared' filter
    if (sharedData != null) {
      filters.add(Filter.newBuilder()
        .setFieldFilter(StructuredQuery.FieldFilter.newBuilder()
        .setField(FieldReference.newBuilder().setFieldPath(SHARED_FIELD))
        .setOp(StructuredQuery.FieldFilter.Operator.EQUAL)
        .setValue(Value.newBuilder().setBooleanValue(sharedData)))
        .build()
      );
    }

    // Apply 'name' filter (exact match as regex is not supported)
    if (q != null && !q.isEmpty()) {
      filters.add(Filter.newBuilder()
        .setFieldFilter(StructuredQuery.FieldFilter.newBuilder()
        .setField(FieldReference.newBuilder().setFieldPath(LOWERCASE_NAME_FIELD))
        .setOp(StructuredQuery.FieldFilter.Operator.EQUAL)
        .setValue(Value.newBuilder().setStringValue(q.toLowerCase())))
        .build()
      );
    }

    // Apply date filter based on DateComparator
    if (date != null && dateCmp != null && dateCmp != DateComparator.NONE) {

      final long startTimestampSeconds = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
      endTimestampSeconds = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toEpochSecond();

      final FieldReference createdAtFieldRef = FieldReference.newBuilder().setFieldPath(CREATED_AT_FIELD).build();

      final Value startValue = Value.newBuilder().setIntegerValue(startTimestampSeconds).build(),
      endValue = Value.newBuilder().setIntegerValue(endTimestampSeconds).build();

      final StructuredQuery.FieldFilter.Builder startDateFilterBuilder = StructuredQuery.FieldFilter.newBuilder()
        .setField(createdAtFieldRef)
        .setValue(startValue),

      endDateFilterBuilder = StructuredQuery.FieldFilter.newBuilder()
        .setField(createdAtFieldRef)
        .setValue(endValue);

      switch (dateCmp) {

        case EQ:
          filters.add(Filter.newBuilder()
            .setCompositeFilter(StructuredQuery.CompositeFilter.newBuilder()
              .setOp(StructuredQuery.CompositeFilter.Operator.AND)
              .addAllFilters(
                List.of(
                  Filter.newBuilder().setFieldFilter(startDateFilterBuilder.setOp(FieldFilter.Operator.GREATER_THAN_OR_EQUAL)).build(),
                  Filter.newBuilder().setFieldFilter(endDateFilterBuilder.setOp(FieldFilter.Operator.LESS_THAN_OR_EQUAL)).build()
                )
              )
            )
            .build()
          );
          break;

        case DIFF:
          filters.add(Filter.newBuilder()
            .setCompositeFilter(StructuredQuery.CompositeFilter.newBuilder()
              .setOp(StructuredQuery.CompositeFilter.Operator.OR)
              .addAllFilters(
                List.of(
                  Filter.newBuilder().setFieldFilter(startDateFilterBuilder.setOp(FieldFilter.Operator.LESS_THAN)).build(),
                  Filter.newBuilder().setFieldFilter(endDateFilterBuilder.setOp(FieldFilter.Operator.GREATER_THAN)).build()
                )
              )
            )
            .build()
          );
          break;

        case GT:
          filters.add(Filter.newBuilder().setFieldFilter(
            endDateFilterBuilder.setOp(FieldFilter.Operator.GREATER_THAN)).build());
          break;

        case GTE:
          filters.add(Filter.newBuilder().setFieldFilter(
            startDateFilterBuilder.setOp(StructuredQuery.FieldFilter.Operator.GREATER_THAN_OR_EQUAL)).build());
          break;

        case LT:
          filters.add(Filter.newBuilder().setFieldFilter(
            startDateFilterBuilder.setOp(StructuredQuery.FieldFilter.Operator.LESS_THAN)).build());
          break;

        default:
          filters.add(Filter.newBuilder().setFieldFilter(
            endDateFilterBuilder.setOp(FieldFilter.Operator.LESS_THAN_OR_EQUAL)).build());
          break;
      }
    }

    Filter finalFilter = null;
    if (!filters.isEmpty()) {
      if (filters.size() == 1) {
        finalFilter = filters.get(0); // If only one filter, use it directly
      }
      else {
        finalFilter = Filter.newBuilder()
          .setCompositeFilter(StructuredQuery.CompositeFilter.newBuilder()
          .setOp(StructuredQuery.CompositeFilter.Operator.AND)
          .addAllFilters(filters))
          .build();
      }
    }

    // Count aggregation
    final StructuredQuery.Builder countQueryBuilder = StructuredQuery.newBuilder();
    if (finalFilter != null) {
      countQueryBuilder.setWhere(finalFilter);
    }

    final Mono<Long> totalCountMono = firestoreTemplate.count(
      Index.class,
      countQueryBuilder
    )
    .defaultIfEmpty(0L);

    // Create the StructuredQuery.Builder for the paginated content ---
    final StructuredQuery.Builder contentQueryBuilder = StructuredQuery.newBuilder();
    if (finalFilter != null) {
      contentQueryBuilder.setWhere(finalFilter);
    }

    // Apply ordering for pagination
    contentQueryBuilder.addOrderBy(Order.newBuilder()
      .setField(FieldReference.newBuilder().setFieldPath(CREATED_AT_FIELD))
      .setDirection(direction == Sort.Direction.ASC ? Direction.ASCENDING : Direction.DESCENDING));

    // Apply pagination (offset and limit)
    contentQueryBuilder.setOffset(page * size);
    contentQueryBuilder.setLimit(Int32Value.of(size));

    // Get the paginated content ---
    final Flux<Index> contentFlux = firestoreTemplate.execute(contentQueryBuilder, Index.class);

    return Mono.zip(contentFlux.collectList(), totalCountMono)
      .map(tuple -> new CollectionPagedResult<>(
        tuple.getT1(),
        tuple.getT2(),
        size,
        page,
        getAllFiles(tuple.getT1())
      ));
  }

  @Override
	public Flux<Index> findAllById(final List<String> ids) {
    return indexRepository.findAllById(ids);
	}

  @Override
	public Mono<Index> getIndex(final String indexId, final String userId) throws IndexNotFoundException {

    return indexRepository
      .findByIdAndCreatedBy(indexId, userId)
      .switchIfEmpty(Mono.error(new IndexNotFoundException()));
	}

  @Override
	public Mono<Index> getIndex(final String indexId) throws IndexNotFoundException {

    return indexRepository
      .findById(indexId)
      .switchIfEmpty(Mono.error(new IndexNotFoundException()));
	}

	@Override
	public Mono<Index> createIndex(final String name, final List<String> fileIds, final String userId)
  throws InvalidOperationException {

    return fileService.validateFileIds(fileIds, userId)
      .flatMap(validFileIds -> {

        final HashSet<String> seen = new HashSet<>();
        for (final String key: fileIds) {
          if (!seen.add(key)) {
            return Mono.error(new IllegalArgumentException(String.format("Duplicate file %c %s %c", 171, key, 187)));
          }
        }

        final String lowercaseName = name.toLowerCase();
        return indexRepository.findByLowercaseName(lowercaseName)
          .hasElement()
          .flatMap(exists -> {
            if (exists) {
              return Mono.error(new IllegalArgumentException(
                String.format("A collection with name %c %s %c already exists", 171, lowercaseName, 187)
              ));
            }
            else {

              final Map<String, String> filesWithChecksums = new HashMap<>();
              for (final String fileId: validFileIds) {
                String checksum = fileService.getFileChecksum(fileId, userId).block();
                filesWithChecksums.put(fileId, checksum);
              }

              final Index index = new Index()
                .setName(name)
                .setLowercaseName(lowercaseName)
                .setFiles(filesWithChecksums)
                .setCreatedBy(userId)
                .setCreatedAt(Utils.getTimestamp());
              return indexRepository.save(index);
            }
          });
      });
	}

  @Override
  public Mono<Index> updateState(final String indexId, final String userId, final boolean shared)
  throws IndexNotFoundException {

    return this.getIndex(indexId, userId)
      .flatMap(index -> {

        final boolean shouldRemove = index.isShared() && !shared;

        return indexRepository
        .save(index
          .setShared(shared)
          .setUpdatedAt(Utils.getTimestamp())
        )
        .flatMap(saved -> {

          // If a public collection is made private, Remove the collection from other people's conversations
          if (shouldRemove) {
            this.removeCollectionFromConversations(saved.getId(), saved.getName(), userId, false);
          }

          return Mono.just(saved);
      });
    });
  }

  @Override
	public Mono<Void> deleteIndex(final String indexId, final String userId) throws IndexNotFoundException {

    return this.getIndex(indexId, userId)
      .flatMap(index -> {

        this.removeCollectionFromConversations(indexId, index.getName(), userId, true);

        return indexRepository
          .delete(index);
      });
	}

  @Override
  public Mono<Index> removeFile(final String indexId, final String userId, final String fileId) throws IndexNotFoundException {

    return this.getIndex(indexId, userId)
      .flatMap(index -> {

        index.getFiles().remove(fileId);
        return indexRepository.save(
          index.setUpdatedAt(Utils.getTimestamp())
        );
      }
    );
  }

  @Override
  public Mono<Index> addFiles(final String indexId, final String userId, final List<String> fileIds)
  throws IndexNotFoundException {

    return this.getIndex(indexId, userId)
      .flatMap(index -> {

        // Check for duplicate files
        final HashSet<String> seen = new HashSet<>();
        for (final String key: fileIds) {

          if (!seen.add(key) || (index.getFiles() != null && index.getFiles().containsKey(key))) {
            return Mono.error(new IllegalArgumentException(String.format("Duplicate file %c %s %c", 171, key, 187)));
          }
        }

        try {
          return fileService.validateFileIds(fileIds, userId)
          .flatMap(validFileIds -> {

            final Map<String, String> filesWithChecksums = new HashMap<>();
            for (final String fileId: validFileIds) {
              final String checksum = fileService.getFileChecksum(fileId, userId).block();
              filesWithChecksums.put(fileId, checksum);
            }

            index.getFiles().putAll(filesWithChecksums);
            return indexRepository.save(
              index.setUpdatedAt(Utils.getTimestamp())
            );
          });
        }
        catch (InvalidOperationException e) {
          return Mono.error(e);
        }
      }
    );
  }

  @Override
  public Flux<Map<String, Object>> getIndexFiles(final String indexId, final String userId) throws IndexNotFoundException {

   return indexRepository
    .findById(indexId)
    .switchIfEmpty(Mono.error(new IndexNotFoundException()))
    .flatMapMany(index -> {

      if (!userId.equals(index.getCreatedBy()) && !index.isShared()) {
        return Mono.error(new IllegalArgumentException("Unauthorized access"));
      }

      return Flux.fromIterable(getAllFiles(List.of(index)));
    });
  }

  private void removeCollectionFromConversations(final String collectionId, final String collectionName, final String userId,
  final boolean collectionDeletion) {

    conversationRepository
    .findAll()
    .filter(item -> item.getCollections() != null && item.getCollections().contains(collectionId)
      && (collectionDeletion || !item.getUserId().equals(userId)))
    .collectList()
    .flatMap(items -> {

      for (final Conversation conversation: items) {

        final String msg = collectionDeletion
          ? String.format("Collection %c %s %c was deleted %s removed from the discussion",
            171, collectionName, 187, userId.equals(conversation.getUserId()) ? "and" : "by their owner and was")
          : String.format("Collection %c %s %c was removed from public space!", 171, collectionName, 187);

        final List<Message> history = conversation.getHistory();
        history.add(new Message("system", msg));

        final List<String> collections = conversation.getCollections();
        collections.remove(collectionId);
        conversation
          .setCollections(collections)
          .setHistory(history)
          .setUpdatedAt(Utils.getTimestamp());
      }

      return conversationRepository
        .saveAll(items)
        .collectList();
    })
    .subscribe(
      resp -> log.info("Conversations updated successfully: Total: {}", resp.size()),
      error -> log.error("Remove collection from conversations: {}", error.getMessage())
    );
  }

  /**
   * Load the file details from the fileService
   * @param indexes
   * @return
   */
  private List<Map<String, Object>> getAllFiles(@NonNull final List<Index> indexes) {

    final Map<String, String> fileIds = new HashMap<>();
    for (final Index index: indexes) {

      if (index.getFiles() == null) continue;

      index.getFiles().forEach((fileId, checksum) -> {
        if (!fileIds.containsKey(fileId)) {
          fileIds.put(fileId, index.getCreatedBy());
        }
      });
    }

    if (fileIds.size() == 0) {
      return Collections.emptyList();
    }

    try {
      return fileService.getFileData(fileIds);
    }
    catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
