/*
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

package io.relution.jenkins.awssqs;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.relution.jenkins.awssqs.interfaces.SQSFactory;
import io.relution.jenkins.awssqs.interfaces.SQSQueue;


public class SQSTriggerQueueTest {

    @Mock
    private SQSFactory        sqsFactory;

    @Mock
    private AmazonSQSAsync    sqs;

    @Mock
    private GetQueueUrlResult getQueueUrlResult;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(this.sqsFactory.createSQS(Matchers.any(SQSQueue.class))).thenReturn(this.sqs);
        Mockito.when(this.sqsFactory.createSQSAsync(Matchers.any(SQSQueue.class))).thenReturn(this.sqs);

        Mockito.when(this.sqs.getQueueUrl(Matchers.anyString())).thenReturn(this.getQueueUrlResult);

        Mockito.when(this.getQueueUrlResult.getQueueUrl()).thenReturn("mock://sqs.url");
    }

    @Test
    public void shouldSetDefaults() {
        // Cannot mock or create an instance of final hudson.util.Secret, so null it is
        final SQSTriggerQueue queue = new SQSTriggerQueue(null, "name", null, 0, 0);
        queue.setFactory(this.sqsFactory);

        assertThat(queue.getUuid()).isNotEmpty();

        assertThat(queue.getUrl()).isEqualTo("mock://sqs.url");
        assertThat(queue.getName()).isEqualTo("name");
        assertThat(queue.getEndpoint()).isNull();
        assertThat(queue.getNameOrUrl()).isEqualTo("name");

        assertThat(queue.getCredentialsId()).isNull();

        assertThat(queue.getWaitTimeSeconds()).isEqualTo(20);
        assertThat(queue.getMaxNumberOfMessages()).isEqualTo(10);
    }

    @Test
    public void shouldHaveNoExplicitEndpoint() {
        final SQSTriggerQueue queue = new SQSTriggerQueue(null, "test-queue", null, 0, 0);
        queue.setFactory(this.sqsFactory);

        assertThat(queue.getUrl()).isEqualTo("mock://sqs.url");
        assertThat(queue.getName()).isEqualTo("test-queue");
        assertThat(queue.getEndpoint()).isNull();
        assertThat(queue.getNameOrUrl()).isEqualTo("test-queue");
    }

    @Test
    public void shouldHaveExplicitEndpoint() {
        final SQSTriggerQueue queue = new SQSTriggerQueue(
                null,
                "https://sqs.us-east-1.amazonaws.com/929548749884/test-queue",
                null,
                0,
                0);
        queue.setFactory(this.sqsFactory);

        assertThat(queue.getUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/929548749884/test-queue");
        assertThat(queue.getName()).isEqualTo("test-queue");
        assertThat(queue.getEndpoint()).isEqualTo("sqs.us-east-1.amazonaws.com");
        assertThat(queue.getNameOrUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/929548749884/test-queue");
    }

    @Test
    public void shouldAcceptAnyUrl() {
        final SQSTriggerQueue queue = new SQSTriggerQueue(
                null,
                "https://git-codecommit.us-east-1.amazonaws.com/v1/repos/test",
                null,
                0,
                0);
        queue.setFactory(this.sqsFactory);

        assertThat(queue.getUrl()).isEqualTo("mock://sqs.url");
        assertThat(queue.getName()).isEqualTo("https://git-codecommit.us-east-1.amazonaws.com/v1/repos/test");
        assertThat(queue.getEndpoint()).isNull();
        assertThat(queue.getNameOrUrl()).isEqualTo("https://git-codecommit.us-east-1.amazonaws.com/v1/repos/test");
    }
}
