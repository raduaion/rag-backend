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
import org.springframework.web.bind.annotation.*;

import com.hermes.model.Conversation;
import com.hermes.model.Index;
import com.hermes.service.ConversationService;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

  @Autowired
  private ConversationService conversationService;

  @GetMapping
  public Flux<Conversation> getAllConversations(final Principal principal) {
    return conversationService.getAllConversations(principal.getName());
  }

  @GetMapping("/{conversationId}")
  public Mono<Conversation> getConversationById(@PathVariable final String conversationId,
  final Principal principal) {
    return conversationService.getConversationById(conversationId, principal.getName());
  }

  @DeleteMapping("/{conversationId}")
  public Mono<String> deleteConversation(@PathVariable final String conversationId, final Principal principal) {
    return conversationService.deleteConversation(conversationId, principal.getName());
  }

  @PostMapping("/{conversationId}/addcollections")
  public Mono<Conversation> addCollections(@PathVariable final String conversationId, @RequestBody final List<String> collections,
  final Principal principal) {
    return conversationService.addCollections(conversationId, collections, principal.getName());
  }

  @PostMapping("/{conversationId}/removecollection")
  public Mono<Conversation> removeCollection(@PathVariable final String conversationId, @RequestParam final String collectionId,
  final Principal principal) {
    return conversationService.removeCollection(conversationId, collectionId, principal.getName());
  }

  @GetMapping("/{conversationId}/collections")
  public Flux<Index> getCollections(@PathVariable final String conversationId, final Principal principal) {
    return conversationService.getCollections(conversationId, principal.getName());
  }

  @PostMapping("/{conversationId}/updatetitle")
  public Mono<Conversation> updateConversationTitle(@PathVariable final String conversationId, @RequestParam final String title,
  final Principal principal) {
    return conversationService.updateConversationTitle(principal.getName(), conversationId, title);
  }

  @PostMapping("/{conversationId}/clearhistory")
  public Mono<Conversation> clearHistory(@PathVariable final String conversationId, final Principal principal) {
    return conversationService.clearHistory(conversationId, principal.getName());
  }

  @PostMapping("/{conversationId}/deleteallcollections")
  public Mono<Conversation> deleteAllCollections(@PathVariable final String conversationId, final Principal principal) {
    return conversationService.deleteAllCollections(conversationId, principal.getName());
  }
}
