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
 * Modified from {@link org.elasticsearch.memcached.MemcachedServer}
 */

package org.elasticsearch.redis;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.settings.ClusterDynamicSettings;
import org.elasticsearch.cluster.settings.DynamicSettings;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.redis.netty.RedisRestChannel;
import org.elasticsearch.rest.RestController;

public class RedisServer extends AbstractLifecycleComponent<RedisServer> {

    private final RedisServerTransport transport;

    private final NodeService nodeService;

    private final RestController restController;

    class ApplySettings implements NodeSettingsService.Listener{

        @Override
        public void onRefreshSettings(Settings settings) {
            String setOption = settings.get("redis.response.set", RedisRestChannel.setOption);
            if (!setOption.equals(RedisRestChannel.setOption)) {
                RedisRestChannel.setOption = setOption;
            }

            String delOption = settings.get("redis.response.del", RedisRestChannel.delOption);
            if (!delOption.equals(RedisRestChannel.delOption)) {
                RedisRestChannel.delOption = delOption;
            }
        }
    }


    @Inject
    public RedisServer(Settings settings, RedisServerTransport transport,
                       RestController restController, NodeService nodeService,
                       NodeSettingsService nodeSettingsService,
                       @ClusterDynamicSettings DynamicSettings dynamicSettings) {
        super(settings);
        this.transport = transport;
        this.restController = restController;
        this.nodeService = nodeService;

        nodeSettingsService.addListener(new ApplySettings());

        dynamicSettings.addDynamicSetting("redis.response.set", RedisResponseSettingValidator.INSTANCE);
        dynamicSettings.addDynamicSetting("redis.response.del", RedisResponseSettingValidator.INSTANCE);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        transport.start();
        if (logger.isInfoEnabled()) {
            logger.info("{}", transport.boundAddress());
        }
        nodeService.putAttribute("redis_address", transport.boundAddress().publishAddress().toString());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        nodeService.removeAttribute("redis_address");
        transport.stop();
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        transport.close();
    }
}
