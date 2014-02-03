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
 * Modified from {@link org.elasticsearch.memcached.netty.MemcachedDispatcher}
 */

package org.elasticsearch.redis.netty;

import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.MessageEvent;
import org.elasticsearch.common.netty.channel.SimpleChannelUpstreamHandler;
import org.elasticsearch.redis.RedisRestRequest;
import org.elasticsearch.rest.RestController;


public class RedisDispatcher extends SimpleChannelUpstreamHandler {

    public static final Object IGNORE_REQUEST = new Object();

    private final RestController restController;

    public RedisDispatcher(RestController restController) {
        this.restController = restController;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() == IGNORE_REQUEST) {
            super.messageReceived(ctx, e);
            return;
        }
        RedisRestRequest request = (RedisRestRequest) e.getMessage();
        RedisRestChannel channel = new RedisRestChannel(ctx.getChannel(), request);
        restController.dispatchRequest(request, channel);
        super.messageReceived(ctx, e);
    }
}
