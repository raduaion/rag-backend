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

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.hermes.data.ApprovalStatus;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.model.UserApproval;
import com.hermes.repository.UserApprovalRepository;
import com.hermes.service.EmailNotificationService;
import com.hermes.service.UserApprovalService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserApprovalServiceImpl implements UserApprovalService {

  @Autowired
  private UserApprovalRepository repository;

  @Autowired
  private EmailNotificationService emailNotificationService;

  @Override
  public Flux<UserApproval> list() {
    return repository.findAll();
  }

  @Override
  public Mono<UserApproval> getById(final String id) throws NotFoundException {
    return repository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Approval Not found")));
  }

  @Override
  public Mono<UserApproval> create(final UserApproval userApproval) throws InvalidOperationException {

    userApproval.setCreatedOn(Utils.getTimestamp());
    validateData(userApproval);

    return repository
      .save(userApproval)
      .doOnNext(user -> sendNotificationMessage(user.getStatus(), user.getEmail()));
  }

  @Override
  public Mono<UserApproval> update(final String id, final UserApproval userApproval)
  throws NotFoundException, InvalidOperationException {

    final UserApproval saved = this.getById(id).block();
    saved
      .setEmail(userApproval.getEmail())
      .setComment(userApproval.getComment())
      .setStatus(userApproval.getStatus())
      .setUpdatedOn(Utils.getTimestamp());

    validateData(saved);

    return repository.save(saved);
  }

  @Override
  public Mono<UserApproval> updateStatus(final String id, final ApprovalStatus status)
  throws NotFoundException, InvalidOperationException {

    return this.getById(id)
      .flatMap(user -> repository.save(
        user
          .setStatus(status)
          .setUpdatedOn(Utils.getTimestamp())
      ))
      .doOnNext(user -> sendNotificationMessage(user.getStatus(), user.getEmail()));
  }

  @Override
  public Mono<String> delete(final String id) throws NotFoundException {
    return this.getById(id)
      .flatMap(item -> repository.delete(item)
      .then(Mono.just("OK")));
  }

  private void validateData(final UserApproval data) {

    final String id = data.getId(),
      email = data.getEmail();
    final ApprovalStatus status = data.getStatus();

    if (email == null) {
      throw new IllegalArgumentException("Email is required!");
    }

    if (!Utils.isValidEmail(email)) {
      throw new IllegalArgumentException("Invalid email!");
    }

    if (status == null) {
      throw new IllegalArgumentException("Status is required!");
    }

    final UserApproval user = repository.findByEmail(email).block();
    if (user != null) {
      if (id == null || !user.getId().equals(id)) {
        throw new IllegalArgumentException("This email already exists!");
      }
    }
  }

  private void sendNotificationMessage(final ApprovalStatus status, @NonNull final String email) {

    String username = email.substring(0, email.indexOf("@"));
    username = username.substring(0, 1).toUpperCase() + username.substring(1);

    if (status == ApprovalStatus.PENDING) {
      emailNotificationService.sendCreationNotification(Locale.ENGLISH, username, email);
    }
    else {
      emailNotificationService.sendApprovalStatusNotification(Locale.ENGLISH, username, email, status);
    }
  }
}
