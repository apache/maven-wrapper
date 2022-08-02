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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * @author Rafael Winterhalter
 */
public class HashAlgorithmVerifier implements Verifier
{

    @Override
    public void verify( Path file, String property, String algorithm, String expectedSum )
            throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance( algorithm );
        try ( InputStream inputStream = Files.newInputStream( file ) )
        {
            byte[] buffer = new byte[ 1024 * 8 ];
            int length;
            while ( ( length = inputStream.read( buffer ) ) != -1 )
            {
                digest.update( buffer, 0, length );
            }
        }
        byte[] hash = digest.digest();
        StringBuilder actualSum = new StringBuilder( hash.length * 2 );
        for ( byte aByte : hash )
        {
            actualSum.append( String.format( "%02x", aByte ) );
        }
        if ( expectedSum.contentEquals( actualSum ) )
        {
            Logger.info( String.format( Locale.ROOT,
                "Validated %s hash for %s to be equal (%s)",
                algorithm, file, expectedSum ) );
        }
        else
        {
            throw new RuntimeException( String.format( Locale.ROOT,
                "Failed to validate Maven distribution %s, your Maven distribution "
                    + "might be compromised. If you updated your Maven version, you need to "
                    + "update the specified %s property.", algorithm, property ) );
        }
    }
}
