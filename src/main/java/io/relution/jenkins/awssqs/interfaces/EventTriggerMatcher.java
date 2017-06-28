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

import hudson.model.AbstractProject;

import java.util.List;


/**
 * Interface definition for classes that match events to {@link AbstractProject}s. If an event
 * matches a project its build process should be triggered.
 */
public interface EventTriggerMatcher {

    /**
     * Returns a value indicating whether any of the specified events matches the specified job.
     * @param events The collection of {@link Event}s to test against the job.
     * @param job The {@link AbstractProject} to test against.
     * @return {@code true} if any of the specified events matches the specified job; otherwise,
     * {@code false}.
     */
    boolean matches(List<Event> events, AbstractProject<?, ?> job);

    /**
     * @see io.relution.jenkins.awssqs.model.entities.codecommit.ExecuteJenkinsJobEvent
     **/
//  boolean matches(List<ExecuteJenkinsJobEvent> events, AbstractProject<?, ?> job);
}
