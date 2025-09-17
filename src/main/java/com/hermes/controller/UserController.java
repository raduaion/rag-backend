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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hermes.data.ApprovalStatus;
import com.hermes.data.UserRole;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.model.User;
import com.hermes.service.UserService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  @GetMapping
  public Flux<User> list() {
    return userService.list();
  }

  @GetMapping("/{id}")
  public Mono<User> getById(@PathVariable final String id) throws NotFoundException {
    return userService.getById(id);
  }

  @PostMapping
  public Mono<User> create(@RequestBody final User user) throws InvalidOperationException {
    return userService.create(user);
  }

  @PutMapping("/{id}")
  public Mono<User> update(@PathVariable final String id, @RequestBody final User user)
  throws NotFoundException, InvalidOperationException {
    return userService.update(id, user);
  }

  @PostMapping("/{id}/updatestatus")
  public Mono<User> updateStatus(@PathVariable final String id, @RequestParam final ApprovalStatus status)
  throws NotFoundException, InvalidOperationException {
    return userService.updateStatus(id, status);
  }

  @PostMapping("/{id}/updaterole")
  public Mono<User> updateUserRole(@PathVariable final String id, @RequestParam final UserRole role)
  throws NotFoundException {
    return userService.updateUserRole(id, role);
  }

  @DeleteMapping("/{id}")
  public Mono<String> delete(@PathVariable final String id) throws NotFoundException {
    return userService.delete(id);
  }
}
