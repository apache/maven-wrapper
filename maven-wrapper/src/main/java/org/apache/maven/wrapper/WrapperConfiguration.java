package org.apache.maven.wrapper;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.net.URI;

/**
 * The Wrapper Configuration
 */
public class WrapperConfiguration
{
    public static final String ALWAYS_UNPACK_ENV = "MAVEN_WRAPPER_ALWAYS_UNPACK";

    public static final String ALWAYS_DOWNLOAD_ENV = "MAVEN_WRAPPER_ALWAYS_DOWNLOAD";

    private boolean alwaysUnpack = Boolean.parseBoolean( System.getenv( ALWAYS_UNPACK_ENV ) );

    private boolean alwaysDownload = Boolean.parseBoolean( System.getenv( ALWAYS_DOWNLOAD_ENV ) );

    private URI distribution;

    private String distributionBase = PathAssembler.MAVEN_USER_HOME_STRING;

    private String distributionPath = Installer.DEFAULT_DISTRIBUTION_PATH;

    private String zipBase = PathAssembler.MAVEN_USER_HOME_STRING;

    private String zipPath = Installer.DEFAULT_DISTRIBUTION_PATH;

    public boolean isAlwaysDownload()
    {
        return alwaysDownload;
    }

    public void setAlwaysDownload( boolean alwaysDownload )
    {
        this.alwaysDownload = alwaysDownload;
    }

    public boolean isAlwaysUnpack()
    {
        return alwaysUnpack;
    }

    public void setAlwaysUnpack( boolean alwaysUnpack )
    {
        this.alwaysUnpack = alwaysUnpack;
    }

    public URI getDistribution()
    {
        return distribution;
    }

    public void setDistribution( URI distribution )
    {
        this.distribution = distribution;
    }

    public String getDistributionBase()
    {
        return distributionBase;
    }

    public void setDistributionBase( String distributionBase )
    {
        this.distributionBase = distributionBase;
    }

    public String getDistributionPath()
    {
        return distributionPath;
    }

    public void setDistributionPath( String distributionPath )
    {
        this.distributionPath = distributionPath;
    }

    public String getZipBase()
    {
        return zipBase;
    }

    public void setZipBase( String zipBase )
    {
        this.zipBase = zipBase;
    }

    public String getZipPath()
    {
        return zipPath;
    }

    public void setZipPath( String zipPath )
    {
        this.zipPath = zipPath;
    }
}
