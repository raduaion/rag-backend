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

import com.hermes.data.QueryRequest;
import com.hermes.model.Conversation;
import com.hermes.service.ConversationService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hermes")
@Slf4j
public class QueryController {

  private ConversationService conversationService;
  private final WebClient webClient;

  @Value("${hermes.processor.url}")
  private String processorUrl;

  public QueryController(WebClient.Builder webClientBuilder, ConversationService conversationService) {
    this.webClient = webClientBuilder.build();
    this.conversationService = conversationService;
  }

  @PostMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Map<String, Object>> handleQuery(@RequestBody final QueryRequest queryRequest, final Principal principal) {

    // Validate request parameters
    if (queryRequest.getQuestion() == null || queryRequest.getQuestion().isEmpty()) {
      log.warn("Request validation failed: question is required");
      return Flux.error(new IllegalArgumentException("Question is required."));
    }

    if (queryRequest.getIndexes() == null || queryRequest.getIndexes().size() == 0) {
      log.warn("Empty indexes received.");
    }

    final Mono<Conversation> conversationMono =
      (queryRequest.getConversationId() == null || queryRequest.getConversationId().isEmpty())
      ? conversationService.createNewConversation(principal.getName(), queryRequest.getIndexes())
      : conversationService.getConversationById(queryRequest.getConversationId());

    return conversationMono
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Conversation not found")))
      .flatMapMany(conversation -> {

        if(!conversation.getUserId().equals(principal.getName())) {
          log.warn("Unauthorized access: User {} is not authorized to access conversation {}", principal.getName(), conversation.getId());
          return Mono.error(new IllegalArgumentException("Unauthorized access"));
        }

        // Prepare payload for Python Processor
        final Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("conversationId", conversation.getId());
        processorPayload.put("userId", principal.getName());
        processorPayload.put("history", conversation.getHistory());
        processorPayload.put("newQuestion", queryRequest.getQuestion());
        processorPayload.put("indexes", conversation.getCollections());

        log.info("Sending request to processor for conversation [{}]", conversation.getId());

        // Call Python Processor (Streaming Response)
        return webClient
          .post()
          .uri(processorUrl + "/processor/query")
          .bodyValue(processorPayload)
          .retrieve()
          .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})  // Process JSON stream
          .doOnNext(event -> handleStatusUpdate(conversation.getId(), event))
          .flatMap(event -> {

            final String status = ((String) event.get("status")).toUpperCase();
            event.put("id", conversation.getId());

            if (List.of("COMPLETED", "ERROR").contains(status)) {

              final String finalAnswer = status.equals("COMPLETED") ? (String) event.get("answer") : "Error processing request.",
              rephrasedQuestion = status.equals("COMPLETED") ? (String) event.get("rephrasedQuestion") : "";

              return conversationService
                .updateConversationWithAnswer(conversation.getId(), queryRequest.getQuestion(), rephrasedQuestion, finalAnswer)
                .thenMany(Flux.just(event));
            }

            return Flux.just(event);
          });
      })
      .onErrorResume(e -> {
        log.error("Error processing request: {}", e.getMessage());
        return Flux.error(new IllegalArgumentException("Encountered an error while processing request."));
      });
  }

  private void handleStatusUpdate(final String conversationId, final Map<String, Object> update) {

    final String status = (String) update.get("status");
    if (status == null) return;

    String result = "[" + conversationId + "]:";
    switch (status.toUpperCase()) {
      case "STARTED": result += "Processing started..."; break;
      case "REPHRASED": result += "Rephrased question: " + update.get("rephrasedQuestion"); break;
      case "RETRIEVING_INDEX": result += "Downloading collection of documents..."; break;
      case "SEARCHING": result += "Searching for relevant documents..."; break;
      case "PARTIAL_ANSWER": result += "Partial answer received: " + update.get("answer"); return;
      case "COMPLETED": result += "Final answer: " + update.get("answer") + "References: " + update.get("references"); break;
      case "ERROR": result += "Error: " + update.get("message"); break;
      default: result += "Unknown status: " + status;
    }
    log.info(result);
  }
}
