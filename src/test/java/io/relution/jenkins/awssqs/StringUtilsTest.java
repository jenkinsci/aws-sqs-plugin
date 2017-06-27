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

import io.relution.jenkins.awssqs.util.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtilsTest {

    @Test
    public void testBranchMatch() {
        String regex = "(.*)feature/sqstest";
        String amzCodecCommitRef = "refs/heads/feature/sqstest";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(amzCodecCommitRef);
        String found = null;
        while (matcher.find()) {
            found = matcher.group();
        }
        Assertions.assertThat(found).matches(regex);
    }

    @Test
    public void testParseCsvString() {
        String str = "test1, test2, test3";
        List<String> items = StringUtils.parseCsvString(str);
        Assertions.assertThat(items).containsExactly("test1", "test2", "test3");

        str = "test1, test2, test3,,,";
        items = StringUtils.parseCsvString(str);
        Assertions.assertThat(items).containsExactly("test1", "test2", "test3");

        str = "test1, test2, test3, , , test4, \"  test5 test6\"";
        items = StringUtils.parseCsvString(str);
        Assertions.assertThat(items).containsExactly("test1", "test2", "test3", "test4", "test5 test6");
    }

    @Test
    public void testFindByUniqueJsonKey() throws IOException {
        String sqsResponse = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("sqscc-msg.json"), StandardCharsets.UTF_8);
        String messageId = StringUtils.findByUniqueJsonKey(sqsResponse, "MessageId");
        String timestamp = StringUtils.findByUniqueJsonKey(sqsResponse, "Timestamp");
        String topicArn = StringUtils.findByUniqueJsonKey(sqsResponse, "TopicArn");
        String signature = StringUtils.findByUniqueJsonKey(sqsResponse, "Signature");
        Assertions.assertThat(sqsResponse).contains(messageId, timestamp, topicArn, signature);

        sqsResponse = "not a json string";
        messageId = StringUtils.findByUniqueJsonKey(sqsResponse, "MessageId");
        Assertions.assertThat(messageId).isNull();
    }

    @Test
    public void testParseWildcard() {
        final List<String> branches = Arrays.asList(
            "master",
            "refs/heads/master",
            "tags/master",

            "feature/sqstest",
            "sqstest",
            "sqstest-foo",
            "sqstest/csv",
            "sqstest-foo/csv",
            "sqstest/csv/something"
        );

        final Map<String, List<String>> fixtures = new LinkedMap();
        fixtures.put("*", Arrays.asList("master", "sqstest", "sqstest-foo"));
        fixtures.put("**", branches);
        fixtures.put("master", Arrays.asList("master"));
        fixtures.put("*master", Arrays.asList("master"));
        fixtures.put("*sqstest", Arrays.asList("sqstest"));
        fixtures.put("sqstest*", Arrays.asList("sqstest", "sqstest-foo"));
        fixtures.put("**sqstest", Arrays.asList("feature/sqstest", "sqstest"));
        fixtures.put("sqstest**", Arrays.asList("sqstest", "sqstest-foo", "sqstest-foo/csv", "sqstest/csv", "sqstest/csv/something"));
        fixtures.put("sqstest/**", Arrays.asList("sqstest/csv", "sqstest/csv/something"));
        fixtures.put("sqstest/*", Arrays.asList("sqstest/csv"));
        fixtures.put("sqstest/**", Arrays.asList("sqstest/csv", "sqstest/csv/something"));

        class WildcardPredicate implements Predicate {

            private final String pattern;

            public WildcardPredicate(String pattern) {
                this.pattern = StringUtils.parseWildcard(pattern);
            }

            @Override
            public boolean evaluate(Object o) {
                String str = o.toString();
                return str.matches(pattern);
            }
        }

        for (String pattern : fixtures.keySet()) {
            System.out.println(String.format("Assert match pattern '%s' => result: %s", pattern, fixtures.get(pattern)));
            final List<String> matchedBranchs = (List<String>) CollectionUtils.select(branches, new WildcardPredicate(pattern));
            Assertions.assertThat(matchedBranchs).containsExactlyInAnyOrder(fixtures.get(pattern).toArray(new String[]{}));
        }
    }

    @Test
    public void testRegexNotSupported() {
        String pattern = StringUtils.parseWildcard("(foo|bar)");
        Assertions.assertThat("foo").doesNotMatch(pattern);
        Assertions.assertThat("bar").doesNotMatch(pattern);
    }
}
