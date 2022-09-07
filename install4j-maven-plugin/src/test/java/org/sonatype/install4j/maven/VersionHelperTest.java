/*
 * Copyright (c) 2012-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.install4j.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Before;
import org.junit.Test;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link VersionHelper}.
 */
public class VersionHelperTest
    extends TestSupport
{
  private VersionHelper helper;

  @Before
  public void setUp() throws Exception {
    helper = new VersionHelper(new SystemStreamLog());
  }

  @Test
  public void parseSingleVersion() {
    String input = "install4j version 5.1.3 (build 5521), built on 2012-09-21";

    String version = helper.parseVersion(input);
    assertThat(version, is("5.1.3"));
  }

  @Test
  public void parseMultiLineVersion() {
    String input = "testing JVM in /usr …\n" +
        "install4j version 5.1.3 (build 5521), built on 2012-09-21\n" +
        "Using Java 1.8.0_25 from /Applications/install4j.app/Contents/Resources/jre.bundle/Contents/Home/jre";

    String version = helper.parseVersion(input);
    assertThat(version, is("5.1.3"));
  }

  @Test
  public void parseMultiLineCrLFVersion() {
    String input = "testing JVM in /usr …\r\n" +
        "install4j version 5.1.3 (build 5521), built on 2012-09-21";

    String version = helper.parseVersion(input);
    assertThat(version, is("5.1.3"));
  }

  @Test
  public void parseMultiLineVersionWithExtraJunk() {
    String input = System.currentTimeMillis() + "\n" +
        System.currentTimeMillis() + "\n" +
        System.currentTimeMillis() + "\n" +
        System.currentTimeMillis() + "\n" +
        "testing JVM in /usr …\n" +
        "install4j version 5.1.3 (build 5521), built on 2012-09-21";

    String version = helper.parseVersion(input);
    assertThat(version, is("5.1.3"));
  }

  @Test
  public void ensureVersionCompatible() throws Exception {
    helper.ensureVersionCompatible("install4j version 6.0.4 XXX");
  }

  @Test(expected = MojoExecutionException.class)
  public void ensureOldVersionFails() throws Exception {
    helper.ensureVersionCompatible("install4j version 5.1.3 XXX");
  }
}
