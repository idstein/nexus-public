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
package org.sonatype.nexus.elasticsearch.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.node.LocalNodeAccess;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.plugins.PluginManager;
import org.elasticsearch.plugins.PluginManager.OutputMode;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * ElasticSearch {@link Node} provider.
 *
 * @since 3.0
 */
@Named
@Singleton
public class NodeProvider
    extends ComponentSupport
    implements Provider<Node>
{
  private final ApplicationDirectories directories;

  private final LocalNodeAccess localNodeAccess;
  
  private final List<String> plugins;

  private Node node;

  @Inject
  public NodeProvider(final ApplicationDirectories directories,
                      final LocalNodeAccess localNodeAccess,
                      @Nullable @Named("${nexus.elasticsearch.plugins}") final String plugins)
  {
    this.directories = checkNotNull(directories);
    this.localNodeAccess = checkNotNull(localNodeAccess);
    this.plugins = plugins == null ? new ArrayList<>() : Splitter.on(",").splitToList(plugins);
  }

  @Override
  public synchronized Node get() {
    if (node == null) {
      try {
        Node newNode = create();

        // yellow status means that node is up (green will mean that replicas are online but we have only one node)
        log.debug("Waiting for yellow-status");
        newNode.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

        this.node = newNode;
      }
      catch (Exception e) {
        // If we can not acquire an ES node reference, give up
        throw Throwables.propagate(e);
      }
    }
    return node;
  }

  private Node create() throws Exception {
    File file = new File(directories.getInstallDirectory(), "etc/elasticsearch.yml");
    checkState(file.exists(), "Missing configuration: %s", file);
    log.info("Creating node with config: {}", file);

    Settings.Builder settings = Settings.builder().loadFromPath(file.toPath());

    // assign node.name to local node-id
    settings.put("node.name", localNodeAccess.getId());
    settings.put("path.plugins", new File(directories.getInstallDirectory(), "plugins").getAbsolutePath());
    NodeBuilder builder = nodeBuilder().settings(settings);

    if (!plugins.isEmpty()) {
      PluginManager pluginManager = new PluginManager(new Environment(settings.build()), null, OutputMode.VERBOSE,
          new TimeValue(30000));

      for (String plugin : plugins) {
        try {
          pluginManager.downloadAndExtract(plugin, Terminal.DEFAULT, true);
        }
        catch (IOException e) {
          log.warn("Failed to install elasticsearch plugin: {}", plugin);
        }
      }
    }

    return builder.node();
  }

  @PreDestroy
  public synchronized void shutdown() {
    if (node != null) {
      log.debug("Shutting down");
      try {
        node.close();
      }
      finally {
        node = null;
      }
    }
  }
}
