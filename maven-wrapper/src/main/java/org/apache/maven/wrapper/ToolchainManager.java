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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Maven toolchains.xml file for JDK installations.
 * Provides functionality to read, update, and create toolchain entries.
 */
class ToolchainManager {
    
    private static final String TOOLCHAINS_XML_TEMPLATE = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<toolchains>\n" +
        "  <!-- JDK toolchains -->\n" +
        "%s" +
        "</toolchains>\n";
    
    private static final String JDK_TOOLCHAIN_TEMPLATE = 
        "  <toolchain>\n" +
        "    <type>jdk</type>\n" +
        "    <provides>\n" +
        "      <version>%s</version>\n" +
        "      <vendor>%s</vendor>\n" +
        "    </provides>\n" +
        "    <configuration>\n" +
        "      <jdkHome>%s</jdkHome>\n" +
        "    </configuration>\n" +
        "  </toolchain>\n";
    
    /**
     * Represents a JDK toolchain entry.
     */
    static class JdkToolchain {
        private final String version;
        private final String vendor;
        private final Path jdkHome;
        
        JdkToolchain(String version, String vendor, Path jdkHome) {
            this.version = version;
            this.vendor = vendor;
            this.jdkHome = jdkHome;
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getVendor() {
            return vendor;
        }
        
        public Path getJdkHome() {
            return jdkHome;
        }
    }
    
    /**
     * Gets the default toolchains.xml file path.
     * 
     * @return Path to ~/.m2/toolchains.xml
     */
    Path getDefaultToolchainsPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".m2", "toolchains.xml");
    }
    
    /**
     * Gets the toolchains.xml file path, checking for custom location via system property.
     * 
     * @return Path to toolchains.xml file
     */
    Path getToolchainsPath() {
        String customPath = System.getProperty("maven.toolchains.file");
        if (customPath != null && !customPath.trim().isEmpty()) {
            return Paths.get(customPath);
        }
        return getDefaultToolchainsPath();
    }
    
    /**
     * Adds or updates a JDK toolchain entry in the toolchains.xml file.
     * 
     * @param jdkToolchain the JDK toolchain to add or update
     * @throws IOException if file operations fail
     */
    void addOrUpdateJdkToolchain(JdkToolchain jdkToolchain) throws IOException {
        Path toolchainsPath = getToolchainsPath();
        
        // Ensure .m2 directory exists
        Files.createDirectories(toolchainsPath.getParent());
        
        List<JdkToolchain> existingToolchains = readExistingToolchains(toolchainsPath);
        
        // Remove existing toolchain with same version and vendor
        existingToolchains.removeIf(existing -> 
            existing.getVersion().equals(jdkToolchain.getVersion()) && 
            existing.getVendor().equals(jdkToolchain.getVendor()));
        
        // Add the new toolchain
        existingToolchains.add(jdkToolchain);
        
        // Write updated toolchains.xml
        writeToolchainsFile(toolchainsPath, existingToolchains);
        
        Logger.info("Updated toolchains.xml with JDK " + jdkToolchain.getVersion() + 
                   " (" + jdkToolchain.getVendor() + ") at " + jdkToolchain.getJdkHome());
    }
    
    /**
     * Reads existing JDK toolchains from the toolchains.xml file.
     * 
     * @param toolchainsPath path to toolchains.xml file
     * @return list of existing JDK toolchains
     * @throws IOException if file reading fails
     */
    private List<JdkToolchain> readExistingToolchains(Path toolchainsPath) throws IOException {
        List<JdkToolchain> toolchains = new ArrayList<>();
        
        if (!Files.exists(toolchainsPath)) {
            return toolchains;
        }
        
        try {
            String content = new String(Files.readAllBytes(toolchainsPath));
            
            // Simple XML parsing for JDK toolchains
            // In a production implementation, you might want to use a proper XML parser
            String[] toolchainBlocks = content.split("<toolchain>");
            
            for (String block : toolchainBlocks) {
                if (block.contains("<type>jdk</type>")) {
                    String version = extractXmlValue(block, "version");
                    String vendor = extractXmlValue(block, "vendor");
                    String jdkHome = extractXmlValue(block, "jdkHome");
                    
                    if (version != null && vendor != null && jdkHome != null) {
                        toolchains.add(new JdkToolchain(version, vendor, Paths.get(jdkHome)));
                    }
                }
            }
        } catch (IOException e) {
            Logger.warn("Failed to read existing toolchains.xml: " + e.getMessage());
        }
        
        return toolchains;
    }
    
    /**
     * Writes the toolchains.xml file with the given JDK toolchains.
     * 
     * @param toolchainsPath path to toolchains.xml file
     * @param toolchains list of JDK toolchains to write
     * @throws IOException if file writing fails
     */
    private void writeToolchainsFile(Path toolchainsPath, List<JdkToolchain> toolchains) throws IOException {
        StringBuilder toolchainEntries = new StringBuilder();
        
        for (JdkToolchain toolchain : toolchains) {
            String entry = String.format(JDK_TOOLCHAIN_TEMPLATE,
                toolchain.getVersion(),
                toolchain.getVendor(),
                toolchain.getJdkHome().toString());
            toolchainEntries.append(entry);
        }
        
        String content = String.format(TOOLCHAINS_XML_TEMPLATE, toolchainEntries.toString());
        
        Files.write(toolchainsPath, content.getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Extracts a value from XML content using simple string matching.
     * This is a basic implementation - a production version should use proper XML parsing.
     * 
     * @param xmlContent the XML content
     * @param tagName the tag name to extract
     * @return the extracted value or null if not found
     */
    private String extractXmlValue(String xmlContent, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        
        int startIndex = xmlContent.indexOf(startTag);
        if (startIndex == -1) {
            return null;
        }
        
        int valueStart = startIndex + startTag.length();
        int endIndex = xmlContent.indexOf(endTag, valueStart);
        if (endIndex == -1) {
            return null;
        }
        
        return xmlContent.substring(valueStart, endIndex).trim();
    }
    
    /**
     * Checks if a JDK toolchain with the given version and vendor already exists.
     * 
     * @param version JDK version
     * @param vendor JDK vendor
     * @return true if toolchain exists, false otherwise
     */
    boolean hasJdkToolchain(String version, String vendor) {
        try {
            Path toolchainsPath = getToolchainsPath();
            List<JdkToolchain> toolchains = readExistingToolchains(toolchainsPath);
            
            return toolchains.stream().anyMatch(toolchain -> 
                toolchain.getVersion().equals(version) && toolchain.getVendor().equals(vendor));
        } catch (IOException e) {
            Logger.warn("Failed to check existing toolchains: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a JDK toolchain entry for the given parameters.
     * 
     * @param version JDK version
     * @param vendor JDK vendor
     * @param jdkHome path to JDK installation
     * @return JdkToolchain instance
     */
    JdkToolchain createJdkToolchain(String version, String vendor, Path jdkHome) {
        return new JdkToolchain(version, vendor, jdkHome);
    }
}
