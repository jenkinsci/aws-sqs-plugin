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

package io.relution.jenkins.awssqs.model.entities.codecommit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * not sure this class used for any model
 * since there is no {@link ExecuteJenkinsJobEvent#jobName} in the Amazon SQS json response
 *
 * @deprecated use {@link io.relution.jenkins.awssqs.interfaces.Event} instead.
 */
@Deprecated
public class ExecuteJenkinsJobEvent {

    @Expose
    @SerializedName("jobName")
    private String jobName;

    public String getJobName() {
        return jobName;
    }

}
