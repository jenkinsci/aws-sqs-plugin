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
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.scm.NullSCM;
import hudson.util.StreamTaskListener;
import io.relution.jenkins.awssqs.logging.Log;
import io.relution.jenkins.awssqs.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;


public class SQSTriggerBuilder implements Runnable {

    private final SQSTrigger trigger;
    private final AbstractProject<?, ?> job;
    private final DateFormat formatter = DateFormat.getDateTimeInstance();

    public SQSTriggerBuilder(final SQSTrigger trigger, final AbstractProject<?, ?> job) {
        this.trigger = trigger;
        this.job = job;
    }

    @Override
    public void run() {
        if (this.job == null) {
            Log.severe("Unexpected error, 'job' object was null!");
            return;
        }

        final File log = this.trigger.getLogFile();

        try (final StreamTaskListener listener = new StreamTaskListener(log)) {
            this.buildIfChanged(listener);
        } catch (final IOException e) {
            io.relution.jenkins.awssqs.logging.Log.severe(e, "Failed to record SCM polling");
        }
    }

    private void buildIfChanged(final StreamTaskListener listener) {
        final PrintStream logger = listener.getLogger();
        final long now = System.currentTimeMillis();

        logger.format("Started on %s", this.toDateTime(now));

        final boolean hasChanges = this.job.getScm().getClass().isAssignableFrom(NullSCM.class) // always trigger Job if NoSCM found
            || this.job.poll(listener).hasChanges();

        if (hasChanges) {
            logger.println("Changes found");
            this.build(logger, now);
        } else {
            logger.println("No changes");
            Log.info("Ignore the build since no changes found for job SCM '%s'", job.getName());
        }

        logger.println("Done. Took " + this.toTimeSpan(now));
    }

    private void build(final PrintStream logger, final long now) {
        Message message = this.trigger.getUpcomingMessagesQueue().poll();
        if (message == null) {
            Log.severe("Unexpected error, 'message' object unable taken from queue!");
            return;
        }

        String messageId = StringUtils.findByUniqueJsonKey(message.getBody(), "MessageId");
        this.startJob(logger, messageId, now);
    }

    private void startJob(final PrintStream logger, String messageId, final long now) {
        String triggerMsg = String.format("Triggering Job for SQS Message [%s] on [%s]", messageId, this.toDateTime(now));
        Log.warning(triggerMsg);

        // setup default cause...
        Cause cause = new Cause.RemoteCause("SQS trigger", triggerMsg);

        logger.println(triggerMsg);

        //sometime a Job can be represent for 1+ SQS messages, @see https://jenkins.io/blog/2010/08/11/quiet-period-feature/
        if (job.scheduleBuild(cause)) {
            logger.println("Job queued");
        } else {
            logger.println("Job NOT queued - it was determined that this job has been queued already.");
        }

        Log.info("Job '%s' is queued? or is building?: %s , %s", job.getName(), job.isInQueue(), job.isBuilding());

        logger.println("Triggering job [COMPLETED]");
    }

    private String toDateTime(final long timestamp) {
        final Date date = new Date(timestamp);
        return this.formatter.format(date);
    }

    private String toTimeSpan(final long timestamp) {
        final long now = System.currentTimeMillis();
        return Util.getTimeSpanString(now - timestamp);
    }
}
