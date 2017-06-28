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
 * Interface definition for classes that parse {@link Message}s that are returned by a message
 * request to an Amazon SQS queue into SCM {@link Event}s.
 */
public interface MessageParser {

    /**
     * Parses the specified message into one or more events.
     * @param message The {@link Message} to parse.
     * @return The collection of {@link Event} items contained in the message.
     */
    List<Event> parseMessage(Message message);
}
