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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ProjectFixture {
    private static final Long TIMEOUT = 36_000L;//in milliseconds, e.g: 300_000 ~ 5 mins

    private String listenBranches;
    private String[] sendBranches;
    private Boolean shouldStarted;
    private Long timeout = TIMEOUT;

    private static final Gson gson = new GsonBuilder()
        .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    public String getListenBranches() {
        return listenBranches;
    }

    public ProjectFixture setListenBranches(String listenBranches) {
        this.listenBranches = listenBranches;
        return this;
    }

    public Boolean getShouldStarted() {
        return shouldStarted;
    }

    public ProjectFixture setShouldStarted(Boolean shouldStarted) {
        this.shouldStarted = shouldStarted;
        return this;
    }

    public String[] getSendBranches() {
        return sendBranches;
    }

    public ProjectFixture setSendBranches(String... sendBranches) {
        this.sendBranches = sendBranches;
        return this;
    }

    public Long getTimeout() {
        return timeout;
    }

    public ProjectFixture setTimeout(Long timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }
}
