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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.relution.jenkins.awssqs.interfaces.SQSFactory;
import io.relution.jenkins.awssqs.interfaces.SQSQueue;
import io.relution.jenkins.awssqs.interfaces.SQSQueueMonitor;
import io.relution.jenkins.awssqs.net.SQSChannel;

import java.util.concurrent.ExecutorService;

public class MockSQSFactory implements SQSFactory {

    @Override
    public AmazonSQS createSQS(SQSQueue queue) {
        return MockAwsSqs.get().getSqsClient();
    }

    @Override
    public AmazonSQSAsync createSQSAsync(SQSQueue queue) {
        return null;
    }

    @Override
    public SQSChannel createChannel(SQSQueue queue) {
        return null;
    }

    @Override
    public SQSQueueMonitor createMonitor(ExecutorService executor, SQSQueue queue) {
        return null;
    }

    @Override
    public SQSQueueMonitor createMonitor(SQSQueueMonitor monitor, SQSQueue queue) {
        return null;
    }
}
