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

package io.relution.jenkins.awssqs.it;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import io.findify.sqsmock.SQSService;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class MockAwsSqs {

    private static final MockAwsSqs instance = new MockAwsSqs();

    private final String sqsMessageTemplate;
    private int port = 8001;
    private boolean started = false;
    private SQSService api;
    private AmazonSQSClient sqsClient;
    private String sqsUrl;

    private MockAwsSqs() {
        try {
            this.sqsMessageTemplate = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("sqscc-msg.json.tpl"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Port " + this.port + " might not available", e);
        }
    }

    public void shutdown() {
        if (!this.started) {
            throw new IllegalStateException("Server might not started yet");
        }

        this.api.shutdown();
        this.started = false;
    }

    private void start() throws IOException {
        //this.port = findFreeLocalPort(); @see https://github.com/findify/sqsmock/pull/7
        this.api = new SQSService(this.port, 1);
        this.api.start();

        AWSCredentials credentials = new AnonymousAWSCredentials();
        this.sqsClient = new MockSQSClient(credentials);

        String endpoint = String.format("http://localhost:%s", this.port);
        this.sqsClient.setEndpoint(endpoint);

        this.sqsUrl = this.sqsClient.createQueue(this.getClass().getSimpleName()).getQueueUrl();
        ((MockSQSClient)this.sqsClient).setQueueUrl(this.sqsUrl);

        this.started = true;
    }

    public static MockAwsSqs get() {
        if (!instance.started) {
            try {
                instance.start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    public void send(final int nom, final String ref) {//@param nom - number of messages should sends
        for (int i = 0; i < nom; i++) {
            send(randomSqsMessageString(ref));
        }
    }

    public void sendRandom(final int nom) {//@param nom - number of messages should sends
        for (int i = 0; i < nom; i++) {
            send(randomSqsMessageString(UUID.randomUUID().toString()));
        }
    }


    public void clearMessages() {
        List<Message> messages = this.sqsClient.receiveMessage(this.sqsUrl).getMessages();
        for (Message message : messages) {
            this.sqsClient.deleteMessage(this.sqsUrl, message.getReceiptHandle());
        }
    }

    public void clearAndSend(final String... refs) {
        clearMessages();
        this.send(refs);
    }

    public void send(final String... refs) {
        for (String ref : refs) {
            this.sqsClient.sendMessage(this.sqsUrl, randomSqsMessageString(ref));
        }
    }

    public int getPort() {
        return this.port;
    }

    public boolean isStarted() {
        return this.started;
    }

    public AmazonSQSClient getSqsClient() {
        return sqsClient;
    }

    public String getSqsUrl() {
        return sqsUrl;
    }

    private int findFreeLocalPort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }

    private String randomSqsMessageString(final String ref) {
        String messageId = this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString();
        String eventId = this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString();
        return this.sqsMessageTemplate
            .replace("${MessageId}", messageId)
            .replace("${EventId}", eventId)
            .replace("${Ref}", ref);
    }
}
