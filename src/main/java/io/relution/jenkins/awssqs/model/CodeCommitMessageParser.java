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

package io.relution.jenkins.awssqs.model;

import com.amazonaws.services.sqs.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.relution.jenkins.awssqs.interfaces.Event;
import io.relution.jenkins.awssqs.interfaces.MessageParser;
import io.relution.jenkins.awssqs.logging.Log;
import io.relution.jenkins.awssqs.model.entities.codecommit.MessageBody;
import io.relution.jenkins.awssqs.model.entities.codecommit.Record;
import io.relution.jenkins.awssqs.model.entities.codecommit.Records;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CodeCommitMessageParser implements MessageParser {

    private static final String EVENT_SOURCE_CODECOMMIT = "aws:codecommit";

    private final Gson gson;

    public CodeCommitMessageParser() {
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    @Override
    public List<Event> parseMessage(final Message message) {
        try {
            MessageBody body = this.gson.fromJson(message.getBody(), MessageBody.class);

            Log.info("Got message with subject: %s", body.getSubject());
            final String json = body.getMessage();
            Log.info("Body of the message: %s", json);
            if (StringUtils.isEmpty(json)) {
                Log.warning("Message contains no text");
                return Collections.emptyList();
            }

            if (!json.startsWith("{") || !json.endsWith("}")) {
                Log.warning("Message text is no JSON");
                return Collections.emptyList();
            }
            return this.parseRecords(json);
        } catch (final com.google.gson.JsonSyntaxException e) {
            io.relution.jenkins.awssqs.logging.Log.warning("JSON syntax exception, cannot parse message: %s", e);
        }
        return Collections.emptyList();
    }

    private List<Event> parseRecords(final String json) {
        Records records = this.gson.fromJson(json, Records.class);
        List<Event> events = new ArrayList<>(records.size());
        for (final Record record : records) {
            this.parseEvents(events, record);
        }

        return events;
    }

    private void parseEvents(final List<io.relution.jenkins.awssqs.interfaces.Event> events, final io.relution.jenkins.awssqs.model.entities.codecommit.Record record) {
        if (!this.isCodeCommitEvent(record)) {
            return;
        }

        final io.relution.jenkins.awssqs.model.entities.codecommit.CodeCommit codeCommit = record.getCodeCommit();

        for (final io.relution.jenkins.awssqs.model.entities.codecommit.Reference reference : codeCommit.getReferences()) {
            final io.relution.jenkins.awssqs.interfaces.Event event = new io.relution.jenkins.awssqs.model.entities.codecommit.CodeCommitEvent(record, reference);
            events.add(event);
        }
    }

    private boolean isCodeCommitEvent(final io.relution.jenkins.awssqs.model.entities.codecommit.Record record) {
        return StringUtils.equals(EVENT_SOURCE_CODECOMMIT, record.getEventSource());
    }
}
