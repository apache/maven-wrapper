/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hans Dockter
 */
public class SystemPropertiesHandler {

    private static final Pattern SYSPROP_PATTERN = Pattern.compile("systemProp\\.(.*)");

    public static Map<String, String> getSystemProperties(Path propertiesFile) {
        Map<String, String> propertyMap = new HashMap<>();
        if (!Files.isRegularFile(propertiesFile)) {
            return propertyMap;
        }
        Properties properties = new Properties();
        try (InputStream inStream = Files.newInputStream(propertiesFile)) {
            properties.load(inStream);
        } catch (IOException e) {
            throw new RuntimeException("Error when loading properties file=" + propertiesFile, e);
        }

        for (Entry<Object, Object> entrySet : properties.entrySet()) {
            Matcher matcher = SYSPROP_PATTERN.matcher(entrySet.getKey().toString());
            if (matcher.find()) {
                String key = matcher.group(1);
                if (key.length() > 0) {
                    propertyMap.put(key, entrySet.getValue().toString());
                }
            }
        }
        return propertyMap;
    }
}
