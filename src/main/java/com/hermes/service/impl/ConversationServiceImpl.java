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

import com.hermes.exceptions.ConversationNotFoundException;
import com.hermes.exceptions.IndexNotFoundException;
import com.hermes.model.Conversation;
import com.hermes.model.Index;
import com.hermes.model.Message;
import com.hermes.repository.ConversationRepository;
import com.hermes.service.ConversationService;
import com.hermes.service.IndexService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ConversationServiceImpl implements ConversationService {

  @Autowired
  private ConversationRepository conversationRepository;

  @Autowired
  private IndexService indexService;

  @Override
	public Flux<Conversation> getAllConversations(final String userId) {
    return conversationRepository.findByUserId(userId);
  }

  @Override
  public Mono<Conversation> getConversationById(final String conversationId) {

    return conversationRepository
      .findById(conversationId)
      .switchIfEmpty(Mono.error(new ConversationNotFoundException()));
  }

  @Override
  public Mono<Conversation> getConversationById(final String conversationId, final String userId) {
 
    return conversationRepository
      .findByIdAndUserId(conversationId, userId)
      .switchIfEmpty(Mono.error(new ConversationNotFoundException()));
  }

  @Override
  public Mono<Conversation> createNewConversation(final String userId, final List<String> collections) {

    final Conversation newConversation = new Conversation()
      .setUserId(userId)
      .setUpdatedAt(Utils.getTimestamp());
    final Mono<Conversation> result = conversationRepository.save(newConversation);

    return collections == null || collections.size() == 0 ? result
      : result.flatMap(c -> this.doAddCollection(c, collections, true));
  }

  @Override
  public Mono<Conversation> updateConversationTitle(final String userId, final String conversationId, final String title) {

    return this.getConversationById(conversationId, userId)
      .flatMap(conversation -> {

        if (title == null || title.isBlank()) {
          return Mono.error(new IllegalArgumentException("Invalid title!"));
        }

        conversation
          .setTitle(title)
          .setUpdatedAt(Utils.getTimestamp());
    
        return conversationRepository.save(conversation);
      });
  }

  @Override
  public Mono<Conversation> updateConversationWithAnswer(final String conversationId, final String question,
  final String rephrasedQuestion, final String answer) {

    return this.getConversationById(conversationId)
      .flatMap(conversation -> {

        // Add new question and answer to conversation history
        final List<Message> history = conversation.getHistory();
        history.add(new Message("user", question, rephrasedQuestion));
        history.add(new Message("assistant", answer));

        final String title = conversation.getTitle();

        conversation
          .setHistory(history)
          .setTitle(title != null && !title.isEmpty() ? title : question)
          .setUpdatedAt(Utils.getTimestamp());

        // Save updated conversation
        return conversationRepository.save(conversation);
      });
  }

  @Override
	public Mono<String> deleteConversation(final String conversationId, final String userId) {

    return this.getConversationById(conversationId, userId)
      .flatMap(index -> conversationRepository.delete(index)
        .then(Mono.just("Conversation deleted successfully.")));
	}

  @Override
  public Mono<Conversation> addCollections(final String conversationId, final List<String> collections, final String userId) {

    return this.getConversationById(conversationId, userId)
      .flatMap(c -> this.doAddCollection(c, collections, false));
  }

  @Override
  public Mono<Conversation> removeCollection(final String conversationId, final String collectionId, final String userId) {

    return this.getConversationById(conversationId, userId)
      .flatMap(conversation -> {

        try {
          return indexService
          .getIndex(collectionId)
          .flatMap(collection -> {

            List<String> existingCollections = conversation.getCollections();
            existingCollections = existingCollections != null ? existingCollections : new ArrayList<>();

            if (!existingCollections.contains(collection.getId())) {
              return Mono.error(new IllegalArgumentException(String.format("The collection %c %s %c wasn't added!", 171, collection.getName(), 187)));
            }

            final List<Message> history = conversation.getHistory();
            history.add(new Message(
              "system",
              String.format("Collection %c %s %c removed from the discussion", 171, collection.getName(), 187)
            ));

            existingCollections.remove(collectionId);
            conversation
              .setCollections(existingCollections)
              .setHistory(history)
              .setUpdatedAt(Utils.getTimestamp());
            return conversationRepository.save(conversation);
          });
        }
        catch (IndexNotFoundException e) {
          return Mono.error(e);
        }
      });
  }

  @Override
  public Flux<Index> getCollections(final String conversationId, final String userId) {

    return this.getConversationById(conversationId, userId)
      .flatMapMany(conversation ->
       conversation.getCollections() == null || conversation.getCollections().size() == 0
        ? Flux.empty()
        : indexService.findAllById(conversation.getCollections())
      );
  }

  @Override
  public Mono<Conversation> clearHistory(final String conversationId, final String userId) {

    return this.getConversationById(conversationId, userId)
      .flatMap(conversation -> {

        conversation
          .setHistory(Collections.emptyList())
          .setUpdatedAt(Utils.getTimestamp());

        return conversationRepository.save(conversation);
      });
  }

  @Override
  public Mono<Conversation> deleteAllCollections(final String conversationId, final String userId) {

    return this.getConversationById(conversationId, userId)
      .flatMap(conversation -> {

        final List<Message> history = conversation.getHistory();
        history.add(new Message("system", "All collections were removed from the discussion"));

        conversation
          .setCollections(List.of())
          .setHistory(history)
          .setUpdatedAt(Utils.getTimestamp());
        return conversationRepository.save(conversation);
      });
  }

  private Mono<Conversation> doAddCollection(final Conversation conversation, final List<String> collections,
  final boolean newConversation) {

    return collections == null 
    ? Mono.error(new IllegalArgumentException("Collections shouldn't be null"))
    : indexService
      .findAllById(collections)
      .collectList()
      .flatMap(collectionList -> {
  
        List<String> existingCollections = conversation.getCollections();
        existingCollections = existingCollections != null ? existingCollections : new ArrayList<>();
        final List<String> collectionNames = new ArrayList<>();

        for (final String collId: collections) {

          final Index item = collectionList.stream().filter(elt -> elt.getId().equals(collId)).findFirst().orElse(null);

          if (item == null) {
            return Mono.error(new IllegalArgumentException(String.format("Collection %c %s %c not found!", 171, collId, 187)));
          }
          else if (existingCollections.contains(item.getId())) {
            return Mono.error(new IllegalArgumentException(String.format("Collection %c %s %c already added!", 171, item.getName(), 187)));
          }

          collectionNames.add(item.getName());
        }

        final List<Message> history = conversation.getHistory();
        final int collectionSize = collectionNames.size();

        if (collectionSize > 0) {

          final String allNames = String.join(", ", collectionNames),
          pluralStr = collectionSize > 1 ? "s" : "";
          history.add(new Message(
            "system",
            newConversation
            ? String.format("Conversation started with %d collection%s: %s", collectionSize, pluralStr, allNames)
            : String.format("Collection%s %c %s %c added to the discussion", pluralStr, 171, allNames, 187)
          ));
          existingCollections.addAll(collections);
        }

        conversation
          .setCollections(existingCollections)
          .setHistory(history)
          .setUpdatedAt(Utils.getTimestamp());
        return conversationRepository.save(conversation);
      });
  }
}
