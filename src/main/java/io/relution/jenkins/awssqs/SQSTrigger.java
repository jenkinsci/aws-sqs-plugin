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

package io.relution.jenkins.awssqs;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.SequentialExecutionQueue;
import io.relution.jenkins.awssqs.i18n.sqstrigger.Messages;
import io.relution.jenkins.awssqs.interfaces.*;
import io.relution.jenkins.awssqs.logging.Log;
import io.relution.jenkins.awssqs.model.events.ConfigurationChangedEvent;
import io.relution.jenkins.awssqs.model.events.EventBroker;
import net.sf.json.JSONObject;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SQSTrigger extends Trigger<AbstractProject<?, ?>> implements io.relution.jenkins.awssqs.interfaces.SQSQueueListener, Runnable {
    private final String queueUuid;
    private final String subscribedBranches;

    private transient SQSQueueMonitorScheduler scheduler;

    private transient MessageParserFactory messageParserFactory;
    private transient EventTriggerMatcher eventTriggerMatcher;

    private transient ExecutorService executor;
    private transient Queue<Message> upcomingMessagesQueue;

    @DataBoundConstructor
    public SQSTrigger(final String queueUuid, final String subscribedBranches) {
        this.queueUuid = queueUuid;
        this.subscribedBranches = subscribedBranches;
    }

    public File getLogFile() {
        if (this.job == null) {
            return null;
        } else {
            return new File(this.job.getRootDir(), "sqs-polling.log");
        }
    }

    @Override
    public void start(final AbstractProject<?, ?> project, final boolean newInstance) {
        super.start(project, newInstance);

        final DescriptorImpl descriptor = (DescriptorImpl) this.getDescriptor();
        descriptor.queue.execute(new Runnable() {

            @Override
            public void run() {
                Log.info("Start trigger for project %s", project);
                SQSTrigger.this.getScheduler().register(SQSTrigger.this);
            }
        });
    }

    @Override
    public void run() {
        final SQSTriggerBuilder builder = new SQSTriggerBuilder(this, this.job);
        builder.run();
    }

    @Override
    public void stop() {
        super.stop();

        final DescriptorImpl descriptor = (DescriptorImpl) this.getDescriptor();
        descriptor.queue.execute(new Runnable() {

            @Override
            public void run() {
                Log.info("Stop trigger (%s)", this);
                SQSTrigger.this.getScheduler().unregister(SQSTrigger.this);
            }
        });
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new SQSTriggerPollingAction());
    }

    @Override
    public void handleMessages(final List<Message> messages) {
        for (final Message message : messages) {
            this.handleMessage(message);
        }
    }

    @Override
    public String getQueueUuid() {
        return this.queueUuid;
    }

    @Override
    public String getSubscribedBranches() {
        return this.subscribedBranches;
    }

    @Inject
    public void setScheduler(final SQSQueueMonitorScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public SQSQueueMonitorScheduler getScheduler() {
        if (this.scheduler == null) {
            io.relution.jenkins.awssqs.Context.injector().injectMembers(this);
        }
        return this.scheduler;
    }

    @Inject
    public void setMessageParserFactory(final MessageParserFactory factory) {
        this.messageParserFactory = factory;
    }

    public MessageParserFactory getMessageParserFactory() {
        if (this.messageParserFactory == null) {
            io.relution.jenkins.awssqs.Context.injector().injectMembers(this);
        }
        return this.messageParserFactory;
    }

    @Inject
    public void setEventTriggerMatcher(final EventTriggerMatcher matcher) {
        this.eventTriggerMatcher = matcher;
    }

    public EventTriggerMatcher getEventTriggerMatcher() {
        if (this.eventTriggerMatcher == null) {
            io.relution.jenkins.awssqs.Context.injector().injectMembers(this);
        }
        return this.eventTriggerMatcher;
    }

    @Inject
    public void setExecutorService(final ExecutorService executor) {
        this.executor = executor;

        // init associated queue and list used to process message
        this.upcomingMessagesQueue = new ConcurrentLinkedQueue<>();
    }

    public ExecutorService getExecutorService() {
        if (this.executor == null) {
            io.relution.jenkins.awssqs.Context.injector().injectMembers(this);
        }
        return this.executor;
    }

    private void handleMessage(final Message message) {
        Log.info("Message '%s' received...", message.getMessageId());
        final MessageParser parser = this.messageParserFactory.createParser(message);
        final EventTriggerMatcher matcher = this.getEventTriggerMatcher();
        final List<Event> events = parser.parseMessage(message);
        if (matcher.matches(events, this.job)) {
            Log.info("Event matched, executing job '%s'", this.job.getName());
            if (this.upcomingMessagesQueue.add(message)) {
                Log.info("Job is being in queue?: %s",this.job.isInQueue());
                this.execute();
            }
        }
    }

    private void execute() {
        if (this.job == null) {
            Log.severe("Unexpected error Job is Null");
            return;
        }

        Log.info("SQS event triggered build of %s", this.job.getFullDisplayName());
        this.executor.execute(this);
    }

    public Queue<Message> getUpcomingMessagesQueue() {
        return upcomingMessagesQueue;
    }

    public final class SQSTriggerPollingAction implements Action {

        public AbstractProject<?, ?> getOwner() {
            return SQSTrigger.this.job;
        }

        @Override
        public String getIconFileName() {
            return "clipboard.png";
        }

        @Override
        public String getDisplayName() {
            return "SQS Activity Log";
        }

        @Override
        public String getUrlName() {
            return "SQSActivityLog";
        }

        public String getLog() throws IOException {
            return Util.loadFile(SQSTrigger.this.getLogFile());
        }

        public void writeLogTo(final XMLOutput out) throws IOException {
            final AnnotatedLargeText<?> log = new AnnotatedLargeText<SQSTriggerPollingAction>(
                    SQSTrigger.this.getLogFile(),
                    Charset.defaultCharset(),
                    true,
                    this);

            log.writeHtmlTo(0, out.asWriter());
        }
    }

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        public static final String KEY_SQS_QUEUES = "sqsQueues";
        private volatile List<io.relution.jenkins.awssqs.SQSTriggerQueue> sqsQueues;

        private volatile transient Map<String, io.relution.jenkins.awssqs.SQSTriggerQueue> sqsQueueMap;
        private transient boolean isLoaded;

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newSingleThreadExecutor());

        public static DescriptorImpl get() {
            final DescriptorExtensionList<Trigger<?>, TriggerDescriptor> triggers = Trigger.all();
            return triggers.get(DescriptorImpl.class);
        }

        public DescriptorImpl() {
            super(SQSTrigger.class);
        }

        @Override
        public synchronized void load() {
            super.load();
            this.initQueueMap();
            this.isLoaded = true;
        }

        @Override
        public boolean isApplicable(final Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return Messages.displayName();
        }

        public ListBoxModel doFillQueueUuidItems() {
            final List<io.relution.jenkins.awssqs.SQSTriggerQueue> queues = this.getSqsQueues();
            final ListBoxModel items = new ListBoxModel();

            for (final io.relution.jenkins.awssqs.SQSTriggerQueue queue : queues) {
                items.add(queue.getName(), queue.getUuid());
            }

            return items;
        }

        public FormValidation doCheckQueueUuid(@QueryParameter final String value) {
            if (this.getSqsQueues().size() == 0) {
                return FormValidation.error(Messages.errorQueueUnavailable());
            }

            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok(Messages.infoQueueDefault());
            }

            final SQSQueue queue = this.getSqsQueue(value);

            if (queue == null) {
                return FormValidation.error(Messages.errorQueueUuidUnknown());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckSubscribedBranches(@QueryParameter String subscribedBranches) {
            if (subscribedBranches == null || subscribedBranches.trim().length() == 0) {
                return FormValidation.warning(Messages.warningSubscribedBranches());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
            final Object sqsQueues = json.get(KEY_SQS_QUEUES);

            this.sqsQueues = req.bindJSONToList(io.relution.jenkins.awssqs.SQSTriggerQueue.class, sqsQueues);
            this.initQueueMap();
            this.save();

            EventBroker.getInstance().post(new ConfigurationChangedEvent());
            return true;
        }

        public List<io.relution.jenkins.awssqs.SQSTriggerQueue> getSqsQueues() {
            if (!this.isLoaded) {
                this.load();
            }
            if (this.sqsQueues == null) {
                return Collections.emptyList();
            }
            return this.sqsQueues;
        }

        public SQSQueue getSqsQueue(final String uuid) {
            if (!this.isLoaded) {
                this.load();
            }
            if (this.sqsQueueMap == null) {
                return null;
            }
            return this.sqsQueueMap.get(uuid);
        }

        private void initQueueMap() {
            if (this.sqsQueues == null) {
                return;
            }

            this.sqsQueueMap = Maps.newHashMapWithExpectedSize(this.sqsQueues.size());

            for (final io.relution.jenkins.awssqs.SQSTriggerQueue queue : this.sqsQueues) {
                this.sqsQueueMap.put(queue.getUuid(), queue);
            }
        }
    }
}
