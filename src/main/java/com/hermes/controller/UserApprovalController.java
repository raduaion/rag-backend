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
import com.hermes.exceptions.ForbiddenException;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.model.UserApproval;
import com.hermes.service.AuthenticationFacade;
import com.hermes.service.UserApprovalService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/userapproval")
public class UserApprovalController {

  @Autowired
  private UserApprovalService service;

  @Autowired
  private AuthenticationFacade authFacade;
  
  @GetMapping
  public Flux<UserApproval> list() throws ForbiddenException {
    authFacade.checkAdmin();
    return service.list();
  }

  @GetMapping("/{id}")
  public Mono<UserApproval> getById(@PathVariable final String id) throws NotFoundException, ForbiddenException {
    authFacade.checkAdmin();
    return service.getById(id);
  }

  @PostMapping
  public Mono<UserApproval> create(@RequestBody final UserApproval userApproval) throws InvalidOperationException, ForbiddenException {
    authFacade.checkAdmin();
    return service.create(userApproval);
  }

  @PutMapping("/{id}")
  public Mono<UserApproval> update(@PathVariable final String id, @RequestBody final UserApproval userApproval)
  throws NotFoundException, InvalidOperationException, ForbiddenException {
    authFacade.checkAdmin();
    return service.update(id, userApproval);
  }

  @PostMapping("/{id}")
  public Mono<UserApproval> updateStatus(@PathVariable final String id, @RequestParam final ApprovalStatus status)
  throws NotFoundException, InvalidOperationException, ForbiddenException {
    authFacade.checkAdmin();
    return service.updateStatus(id, status);
  }

  @DeleteMapping("/{id}")
  public Mono<String> delete(@PathVariable final String id) throws NotFoundException, ForbiddenException {
    authFacade.checkAdmin();
    return service.delete(id);
  }
}
