/*
 * Copyright 2017 Ribose Inc. <https://www.ribose.com>
 * Copyright 2016 M-Way Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.relution.jenkins.awssqs.interfaces;

import com.amazonaws.services.sqs.model.Message;

import java.util.List;


/**
 * Interface definition for classes that listen for {@link Message}s that are returned by a request
 * to an Amazon SQS queue.
 */
public interface SQSQueueListener {

    /**
     * The unique identifier of an Amazon SQS configuration that identifies the queue
     * this listener is associated with.
     * @return The unique identifier of the {@link SQSQueue} this listener is associated with.
     */
    String getQueueUuid();

    /**
     * The subscribed branches this listener is associated with.
     * @return The subscribed branches this listener is associated with.
     */
    String getSubscribedBranches();

    /**
     * The method to be invoked when new messages arrive in the SQS queue this listener is
     * associated with.
     * @param messages The collection of {@link Message} instances that were posted to the queue.
     */
    void handleMessages(List<Message> messages);
}
