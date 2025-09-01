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

import com.hermes.data.ApprovalStatus;
import com.hermes.exceptions.InvalidOperationException;
import com.hermes.exceptions.NotFoundException;
import com.hermes.model.UserApproval;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserApprovalService {

  Flux<UserApproval> list();

  Mono<UserApproval> getById(String id) throws NotFoundException;

  Mono<UserApproval> create(UserApproval userApproval) throws InvalidOperationException;

  Mono<UserApproval> update(String id, UserApproval userApproval) throws NotFoundException, InvalidOperationException;

  Mono<UserApproval> updateStatus(String id, ApprovalStatus status) throws NotFoundException, InvalidOperationException;

  Mono<String> delete(String id) throws NotFoundException;
}
