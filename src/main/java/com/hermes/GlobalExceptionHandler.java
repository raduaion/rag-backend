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
package com.hermes;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.hermes.exceptions.ConversationNotFoundException;
import com.hermes.exceptions.FileInUseException;
import com.hermes.exceptions.ForbiddenException;
import com.hermes.exceptions.IndexNotFoundException;
import com.hermes.exceptions.InvalidFilesException;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotAuthorizedException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.exceptions.UploadFailedException;

import java.util.Map;

@ControllerAdvice
class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  @ResponseBody
  @ResponseStatus
  public Map<String, Object> handleResponseStatusException(ResponseStatusException ex) {
    return Map.of(
      "message", ex.getReason(),
      "code", ex.getStatusCode()
    );
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Map<String, Object> handleRuntimeException(RuntimeException ex) {
    return Map.of(
      "message", ex.getMessage(),
      "code", HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  @ExceptionHandler(FileInUseException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleFileInUseException(final FileInUseException exception) {
    return Map.of(
      "message", "The file is part of an index and cannot be deleted.",
      "code", HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(InvalidOperationException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleInvalidOperationException(final InvalidOperationException exception) {
    return Map.of(
      "message", "Invalid operation: " + exception.getMessage(),
      "code", HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(IndexNotFoundException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, Object> handleIndexNotFoundException(final IndexNotFoundException exception) {
    return Map.of(
      "message", "Index not found or access denied",
      "code", HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(UploadFailedException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleUploadFailedException(final UploadFailedException exception) {
    return Map.of(
      "message", "Upload failed: " + exception.getMessage(),
      "code", HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(InvalidFilesException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleInvalidFilesException(final InvalidFilesException exception) {
    return Map.of(
      "message", "Invalid file IDs provided.",
      "code", HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(NotAuthorizedException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Map<String, Object> handleNotAuthorizedException(final NotAuthorizedException exception) {
    return Map.of(
      "message", "Not authorized: " + exception.getMessage(),
      "code", HttpStatus.UNAUTHORIZED
    );
  }

  @ExceptionHandler(ConversationNotFoundException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, Object> handleConversationNotFoundException(final ConversationNotFoundException exception) {
    return Map.of(
      "message", "Conversation not found or access denied",
      "code", HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, Object> handleNotFoundException(final NotFoundException exception) {
    return Map.of(
      "message", exception.getMessage(),
      "code", HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(ForbiddenException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, Object> handleForbiddenException(final ForbiddenException exception) {
    return Map.of(
      "message", exception.getMessage(),
      "code", HttpStatus.FORBIDDEN
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleIllegalArgumentException(final IllegalArgumentException exception) {
    return Map.of(
      "message", exception.getMessage(),
      "code", HttpStatus.BAD_REQUEST
    );
  }
}
