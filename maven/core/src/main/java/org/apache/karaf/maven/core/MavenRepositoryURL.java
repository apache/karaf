/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.maven.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.ops4j.pax.url.mvn.ServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An URL of Maven repository that knows if it contains SNAPSHOT versions and/or releases.
 *
 * @author Alin Dreghiciu
 * @author Guillaume Nodet
 * @since 0.2.1, February 07, 2008
 *
 * @see org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL
 */
public class MavenRepositoryURL
{
    /*
     * String OPTION_ALLOW_SNAPSHOTS = "snapshots";
     * String OPTION_DISALLOW_RELEASES = "noreleases";
     * String OPTION_MULTI = "multi";
     * String OPTION_ID = "id";
     * String OPTION_UPDATE = "update";
     * String OPTION_RELEASES_UPDATE = "releasesUpdate";
     * String OPTION_SNAPSHOTS_UPDATE = "snapshotsUpdate";
     * String OPTION_CHECKSUM = "checksum";
     * String OPTION_RELEASES_CHECKSUM = "releasesChecksum";
     * String OPTION_SNAPSHOTS_CHECKSUM = "snapshotsChecksum";
     */

    private static final Logger LOG = LoggerFactory.getLogger( MavenRepositoryURL.class );

    /**
     * Repository Id.
     */
    private final String m_id;
    /**
     * Repository URL.
     */
    private URL m_repositoryURL;
    /**
     * Repository file (only if URL is a file URL).
     */
    private final File m_file;
    /**
     * True if the repository contains snapshots.
     */
    private boolean m_snapshotsEnabled;
    /**
     * True if the repository contains releases.
     */
    private boolean m_releasesEnabled;
    /**
     * Repository update policy
     */
    private String m_releasesUpdatePolicy;
    /**
     * Repository update policy
     */
    private String m_snapshotsUpdatePolicy;
    /**
     * Repository checksum policy
     */
    private String m_releasesChecksumPolicy;
    /**
     * Repository checksum policy
     */
    private String m_snapshotsChecksumPolicy;

    private final boolean m_multi;
    /**
     * Where the repository was defined (PID or settings.xml)
     */
    private final FROM m_from;

    /**
     * Creates a maven repository URL bases on a string spec. The path can be marked with @snapshots and/or @noreleases
     * (not case sensitive).
     *
     * @param repositorySpec url spec of repository
     *
     * @throws MalformedURLException if spec contains a malformed maven repository url
     */
    public MavenRepositoryURL( final String repositorySpec )
        throws MalformedURLException
    {
        final String[] segments = repositorySpec.split( ServiceConstants.SEPARATOR_OPTIONS );
        final StringBuilder urlBuilder = new StringBuilder();
        boolean snapshotEnabled = false;
        boolean releasesEnabled = true;
        boolean multi = false;

        String name = null;
        String update = null;
        String updateReleases = null;
        String updateSnapshots = null;
        String checksum = null;
        String checksumReleases = null;
        String checksumSnapshots = null;
        FROM from = null;

        for( int i = 0; i < segments.length; i++ )
        {
            String segment = segments[i].trim();
            if( segment.equalsIgnoreCase( ServiceConstants.OPTION_ALLOW_SNAPSHOTS ) )
            {
                snapshotEnabled = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_DISALLOW_RELEASES ) )
            {
                releasesEnabled = false;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_MULTI ) )
            {
                multi = true;
            }
            else if( segment.startsWith( ServiceConstants.OPTION_ID + "=" ) )
            {
                try {
                    name = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_RELEASES_UPDATE + "=" ) )
            {
                try {
                    updateReleases = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SNAPSHOTS_UPDATE + "=" ) )
            {
                try {
                    updateSnapshots = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_UPDATE + "=" ) )
            {
                try {
                    update = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_RELEASES_CHECKSUM + "=" ) )
            {
                try {
                    checksumReleases = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SNAPSHOTS_CHECKSUM + "=" ) )
            {
                try {
                    checksumSnapshots = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_CHECKSUM + "=" ) )
            {
                try {
                    checksum = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( "_from=" ) )
            {
                try {
                    from = FROM.valueOf( segments[ i ].split( "=" )[1].trim() );
                } catch (Exception ignored) {
                }
            }
            else
            {
                if( i > 0 )
                {
                    urlBuilder.append( ServiceConstants.SEPARATOR_OPTIONS );
                }
                urlBuilder.append( segments[ i ] );
            }
        }
        String spec = buildSpec( urlBuilder );
        m_repositoryURL = new URL( spec );
        m_snapshotsEnabled = snapshotEnabled;
        m_releasesEnabled = releasesEnabled;
        m_multi = multi;
        if (name == null) {
            String warn = "Repository spec " + spec + " does not contain an identifier. This is deprecated & discouraged & just evil.";
            LOG.warn( warn );
            name = "repo_" + spec.hashCode();
        }
        m_id = name;
        m_releasesUpdatePolicy = updateReleases != null ? updateReleases : update;
        m_snapshotsUpdatePolicy = updateSnapshots != null ? updateSnapshots : update;
        m_releasesChecksumPolicy = checksumReleases != null ? checksumReleases : checksum;
        m_snapshotsChecksumPolicy = checksumSnapshots != null ? checksumSnapshots : checksum;

        m_from = from != null ? from : FROM.PID;

        if( m_repositoryURL.getProtocol().equals( "file" ) )
        {
            try
            {
                // You must transform to URI to decode the path (manage a path with a space or non
                // us character)
                // like D:/documents%20and%20Settings/SESA170017/.m2/repository
                // the path can be store in path part or in scheme specific part (if is relatif
                // path)
                // the anti-slash character is not a valid character for uri.
                spec = spec.replaceAll( "\\\\", "/" );
                spec = spec.replaceAll( " ", "%20" );
                URI uri = new URI( spec );
                String path = uri.getPath();
                if( path == null )
                    path = uri.getSchemeSpecificPart();
                m_file = new File( path );

            }
            catch ( URISyntaxException e )
            {
                throw new MalformedURLException( e.getMessage() );
            }
        }
        else
        {
            m_file = null;
        }
    }

    private String buildSpec( StringBuilder urlBuilder )
    {
        String spec = urlBuilder.toString().trim();
        if( !spec.endsWith( "\\" ) && !spec.endsWith( "/" ) )
        {
            spec = spec + "/";
        }
        return spec;
    }

    /**
     * Getter.
     *
     * @return repository id
     */
    public String getId()
    {
        return m_id;
    }

    /**
     * Getter.
     *
     * @return repository URL
     */
    public URL getURL()
    {
        return m_repositoryURL;
    }

    public void setURL(URL url) {
        this.m_repositoryURL = url;
    }

    /**
     * Getter.
     *
     * @return repository file
     */
    public File getFile()
    {
        return m_file;
    }

    /**
     * Getter.
     *
     * @return true if the repository contains releases
     */
    public boolean isReleasesEnabled()
    {
        return m_releasesEnabled;
    }

    public void setReleasesEnabled(boolean enabled)
    {
        m_releasesEnabled = enabled;
    }

    /**
     * Getter.
     *
     * @return true if the repository contains snapshots
     */
    public boolean isSnapshotsEnabled()
    {
        return m_snapshotsEnabled;
    }

    public void setSnapshotsEnabled(boolean enabled)
    {
        m_snapshotsEnabled = enabled;
    }

    public String getReleasesUpdatePolicy() {
        return m_releasesUpdatePolicy;
    }

    public String getSnapshotsUpdatePolicy() {
        return m_snapshotsUpdatePolicy;
    }

    public String getReleasesChecksumPolicy() {
        return m_releasesChecksumPolicy;
    }

    public String getSnapshotsChecksumPolicy() {
        return m_snapshotsChecksumPolicy;
    }

    public void setReleasesUpdatePolicy(String policy) {
        m_releasesUpdatePolicy = policy;
    }

    public void setSnapshotsUpdatePolicy(String policy) {
        m_snapshotsUpdatePolicy = policy;
    }

    public void setReleasesChecksumPolicy(String policy) {
        m_releasesChecksumPolicy = policy;
    }

    public void setSnapshotsChecksumPolicy(String policy) {
        m_snapshotsChecksumPolicy = policy;
    }

    public FROM getFrom() {
        return m_from;
    }

    /**
     * Getter.
     *
     * @return true if the repository is a parent path of repos
     */
    public boolean isMulti()
    {
        return m_multi;
    }

    /**
     * Getter.
     *
     * @return if the repository is a file based repository.
     */
    public boolean isFileRepository()
    {
        return m_file != null;
    }

    @Override
    public String toString()
    {
        return m_repositoryURL.toString()
            + ",releases=" + m_releasesEnabled
            + ",snapshots=" + m_snapshotsEnabled;
    }

    public String asRepositorySpec() {
        StringBuilder sb = new StringBuilder();

        sb.append(m_repositoryURL.toString());
        if (m_id != null) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_ID).append("=").append(m_id);
        }
        if (!m_releasesEnabled) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_DISALLOW_RELEASES);
        }
        if (m_snapshotsEnabled) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_ALLOW_SNAPSHOTS);
        }
        if (m_releasesEnabled) {
            if (!m_snapshotsEnabled) {
                if (m_releasesUpdatePolicy != null) {
                    sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_RELEASES_UPDATE).append("=").append(m_releasesUpdatePolicy);
                }
                if (m_releasesChecksumPolicy != null) {
                    sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_RELEASES_CHECKSUM).append("=").append(m_releasesChecksumPolicy);
                }
            }
        }
        if (m_snapshotsEnabled) {
            if (!m_releasesEnabled) {
                if (m_snapshotsUpdatePolicy != null) {
                    sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_SNAPSHOTS_UPDATE).append("=").append(m_snapshotsUpdatePolicy);
                }
                if (m_snapshotsChecksumPolicy != null) {
                    sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_SNAPSHOTS_CHECKSUM).append("=").append(m_snapshotsChecksumPolicy);
                }
            }
        }
        if (m_snapshotsEnabled && m_releasesEnabled) {
            // compact snapshots & release update & checksum policies
            if (m_releasesUpdatePolicy != null && Objects.equals(m_releasesUpdatePolicy, m_snapshotsUpdatePolicy)) {
                sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_UPDATE).append("=").append(m_releasesUpdatePolicy);
            }
            if (m_releasesChecksumPolicy != null && Objects.equals(m_releasesChecksumPolicy, m_snapshotsChecksumPolicy)) {
                sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_CHECKSUM).append("=").append(m_releasesChecksumPolicy);
            }
        }

        return sb.toString();
    }

    public static enum FROM {
        PID("PID configuration"),
        SETTINGS("Maven XML settings"),
        FALLBACK("Fallback repository");

        private String source;

        FROM(String source) {
            this.source = source;
        }

        public String getSource() {
            return source;
        }
    }

}
