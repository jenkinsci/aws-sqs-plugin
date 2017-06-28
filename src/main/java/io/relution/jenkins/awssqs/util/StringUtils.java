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

package io.relution.jenkins.awssqs.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides static methods that can be used to work with {@link String}
 */
public final class StringUtils {

    /**
     * Parse csv string and return list of trimmed strings
     *
     * @param str The csv string, can be null
     * @return list of trimmed strings
     */
    public static List<String> parseCsvString(final String str) {
        if (str == null || str.trim().length() == 0) {
            return Collections.emptyList();
        }

        List<String> strs = Arrays.asList(str.split("\\s*,\\s*"));
        List<String> result = new ArrayList<>();
        for (String s : strs) {
            String item = s.replaceAll("\"", "").replaceAll("'", "").trim();
            if (item.length() > 0) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Find and return value of uniqueKey in jsonString
     *
     * @param jsonString json string, can be null
     * @param uniqueKey string, unique key in jsonString
     * @return value of <code>jsonString.uniqueKey</code>, or <code>null</code>  if not found
     */
    public static String findByUniqueJsonKey(String jsonString, String uniqueKey) {
        if (jsonString == null || jsonString.trim().length() == 0 || uniqueKey == null || uniqueKey.trim().length() == 0) {
            return null;
        }

        jsonString = jsonString.trim();
        uniqueKey = uniqueKey.trim();

        String regex = String.format("\"%s\"\\s*:\\s*[^\"]*\"([^\"]+)\"", uniqueKey);
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(jsonString);
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Parse string containing wildcards to Java Regex string
     *
     * @param str string containing wildcards, can be null
     * @return regex can be used in {@link String#matches(String)}, or <code>null</code>  if not found
     */
    public static String parseWildcard(String str) {
        if (str == null || str.trim().length() == 0) {
            return null;
        }

        str = str.trim();
        StringBuffer regexBuilder = new StringBuffer(str.length());
        regexBuilder.append('^');
        for (int i = 0, is = str.length(); i < is; i++) {
            char c = str.charAt(i);
            switch(c) {
                case '*':
                    char nc = i + 1 < str.length() ? str.charAt(i + 1) : 0;
                    if (nc == '*') {//detect '**'
                        i++;// move i to next
                        regexBuilder.append(".*");
                    }
                    else {
                        regexBuilder.append("[^/]*");
                    }
                    break;
                case '?':
                    regexBuilder.append(".");
                    break;
                // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    regexBuilder.append("\\").append(c);
                    break;
                default:
                    regexBuilder.append(c);
                    break;
            }
        }
        regexBuilder.append('$');
        return regexBuilder.toString();
    }
}
