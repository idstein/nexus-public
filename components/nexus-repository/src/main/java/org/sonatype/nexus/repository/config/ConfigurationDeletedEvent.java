/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.config;

import org.sonatype.nexus.common.entity.EntityDeletedEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Repository {@link Configuration} deleted event.
 *
 * @since 3.1
 */
public class ConfigurationDeletedEvent
    extends EntityDeletedEvent
    implements ConfigurationEvent
{
  private final String repositoryName;

  public ConfigurationDeletedEvent(final EntityMetadata metadata, final boolean isLocal, final String repositoryName) {
    super(metadata, isLocal);
    this.repositoryName = checkNotNull(repositoryName);
  }

  @Override
  public String getRepositoryName() {
    return repositoryName;
  }

  @Override
  public Configuration getConfiguration() {
    return getEntity();
  }
}
