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

import java.util.List;

import com.hermes.model.Conversation;
import com.hermes.model.Index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConversationService {

  Flux<Conversation> getAllConversations(final String userId);

  Mono<Conversation> getConversationById(String conversationId);

  Mono<Conversation> getConversationById(String conversationId, String userId);

  Mono<Conversation> createNewConversation(String userId, List<String> collections);

  Mono<Conversation> updateConversationTitle(final String userId, final String conversationId, final String title);

  Mono<Conversation> updateConversationWithAnswer(String conversationId, String question, String rephrasedQuestion, String answer);

  Mono<String> deleteConversation(final String conversationId, final String userId);

  Mono<Conversation> addCollections(final String conversationId, final List<String> collections, final String userId);

  Mono<Conversation> removeCollection(final String conversationId, final String collectionId, final String userId);

  Flux<Index> getCollections(final String conversationId, final String userId);

  Mono<Conversation> clearHistory(final String conversationId, final String userId);

  Mono<Conversation> deleteAllCollections(final String conversationId, final String userId);
}
