/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modified from {@link org.elasticsearch.plugin.transport.memcached.MemcachedPlugin}
 */

package org.elasticsearch.plugin.transport.redis;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.redis.RedisServer;
import org.elasticsearch.redis.RedisServerModule;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

/**
 */
public class RedisTransportPlugin extends AbstractPlugin {

    private final Settings settings;

    public RedisTransportPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "transport-redis";
    }

    @Override
    public String description() {
        return "Exports Elasticsearch APIs over redis";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        if (settings.getAsBoolean("redis.enabled", true)) {
            modules.add(RedisServerModule.class);
        }
        return modules;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = newArrayList();
        if (settings.getAsBoolean("redis.enabled", true)) {
            services.add(RedisServer.class);
        }
        return services;
    }
}
