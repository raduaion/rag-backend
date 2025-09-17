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
import com.hermes.data.UserRole;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.model.User;
import com.hermes.repository.UserRepository;
import com.hermes.service.EmailNotificationService;
import com.hermes.service.UserService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EmailNotificationService emailNotificationService;

  @Override
  public Flux<User> list() {
    return userRepository.findAll();
  }

  @Override
  public Mono<User> getById(final String id) throws NotFoundException {
    return userRepository.findById(id)
      .switchIfEmpty(Mono.error(new NotFoundException("User Not found")));
  }

  @Override
  public Mono<User> create(final User user) throws InvalidOperationException {

    user.setCreatedOn(Utils.getTimestamp());
    validateData(user);

    return userRepository
      .save(user)
      .doOnNext(u -> sendNotificationMessage(u.getStatus(), u.getFullName(), u.getEmail()));
  }

  @Override
  public Mono<User> update(final String id, final User user)
  throws NotFoundException, InvalidOperationException {

    final User saved = this.getById(id).block();
    saved
      .setEmail(user.getEmail())
      .setComment(user.getComment())
      .setStatus(user.getStatus())
      .setRole(user.getRole())
      .setUpdatedOn(Utils.getTimestamp());

    validateData(saved);

    return userRepository.save(saved);
  }

  @Override
  public Mono<User> updateStatus(final String id, final ApprovalStatus status)
  throws NotFoundException, InvalidOperationException {

    return this.getById(id)
      .flatMap(user -> userRepository.save(
        user
          .setStatus(status)
          .setUpdatedOn(Utils.getTimestamp())
      ))
      .doOnNext(user -> sendNotificationMessage(user.getStatus(), user.getFullName(), user.getEmail()));
  }

  @Override
  public Mono<User> updateUserRole(final String id, final UserRole role) throws NotFoundException {

    return this.getById(id)
      .flatMap(user -> {

        if (user.getRole() == role) {
          return Mono.error(new InvalidOperationException("This role is already assigned!"));
        }
  
        return userRepository.save(
        user
          .setRole(role)
          .setUpdatedOn(Utils.getTimestamp()));
      })
      .doOnNext(user -> 
        emailNotificationService.sendUserRoleNotification(
          Locale.ENGLISH,
          user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : getNameFromEmail(user.getEmail()),
          user.getEmail(),
          role
        )
      );
  }

  @Override
  public Mono<String> delete(final String id) throws NotFoundException {

    return this.getById(id)
      .flatMap(item -> userRepository.delete(item)
      .then(Mono.just("OK")));
  }

  /**
   * Validate data before creation
   * @param data
   */
  private void validateData(final User data) {

    final String id = data.getId(),
      email = data.getEmail();

    final ApprovalStatus status = data.getStatus();
    final UserRole role = data.getRole();

    if (email == null) {
      throw new IllegalArgumentException("Email is required!");
    }

    if (!Utils.isValidEmail(email)) {
      throw new IllegalArgumentException("Invalid email!");
    }

    if (status == null) {
      throw new IllegalArgumentException("Status is required!");
    }

    if (role == null) {
      throw new IllegalArgumentException("User role is required!");
    }

    final User user = userRepository.findByEmail(email).block();
    if (user != null) {
      if (id == null || !user.getId().equals(id)) {
        throw new IllegalArgumentException("This email already exists!");
      }
    }
  }

  // Send status update notification
  private void sendNotificationMessage(final ApprovalStatus status, final String fullName, @NonNull final String email) {

    final String username = fullName != null && !fullName.isEmpty() ? fullName : getNameFromEmail(email);

    if (status == ApprovalStatus.PENDING) {
      emailNotificationService.sendCreationNotification(Locale.ENGLISH, username, email);
    }
    else {
      emailNotificationService.sendApprovalStatusNotification(Locale.ENGLISH, username, email, status);
    }
  }

  /**
   * Get name from email
   * 
   * @param email
   * @return
   */
  private String getNameFromEmail(final String email) {

    final String username = email.substring(0, email.indexOf("@"));
    return username.substring(0, 1).toUpperCase() + username.substring(1);
  }
}
