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

package io.relution.jenkins.awssqs.model.matchers;

import io.relution.jenkins.awssqs.interfaces.EventTriggerMatcher;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEventTriggerMatcher implements EventTriggerMatcher {

    protected final List<EventTriggerMatcher> matchers;

    public AbstractEventTriggerMatcher(EventTriggerMatcher... matchers) {
        this.matchers = new ArrayList<>();
        CollectionUtils.addAll(this.matchers, matchers);
    }

    public AbstractEventTriggerMatcher(List<EventTriggerMatcher> matchers) {
        this.matchers = new ArrayList<>(matchers);
    }

    public AbstractEventTriggerMatcher and(EventTriggerMatcher... matchers) {
        CollectionUtils.addAll(this.matchers, matchers);
        return new AndEventTriggerMatcher(this.matchers);
    }

    public AbstractEventTriggerMatcher or(EventTriggerMatcher... matchers) {
        CollectionUtils.addAll(this.matchers, matchers);
        return new OrEventTriggerMatcher(this.matchers);
    }
}
