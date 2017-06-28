/*
 * Copyright 2017 Ribose Inc. <https://www.ribose.com>
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

package io.relution.jenkins.awssqs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.relution.jenkins.awssqs.model.entities.codecommit.MessageBody;
import io.relution.jenkins.awssqs.model.entities.codecommit.Record;
import io.relution.jenkins.awssqs.model.entities.codecommit.Records;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GsonParserTest {

    private final Gson gson =  new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    @Test
    public void testParseSampleSqsResponse() throws IOException {
        String sqsResponse = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("sqscc-msg.json"), StandardCharsets.UTF_8);
        Assertions.assertThat(sqsResponse).isNotNull().isNotEmpty();

        MessageBody messageBody = gson.fromJson(sqsResponse, MessageBody.class);
        Assertions.assertThat(messageBody).isNotNull()
            .extracting("MessageId", "Message").isNotNull().isNotEmpty();

        Records records = gson.fromJson(messageBody.getMessage(), Records.class);
        Assertions.assertThat(records).isNotNull().hasOnlyElementsOfTypes(Record.class);
    }
}
