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
 * Modified from {@link org.elasticsearch.memcached.netty.MemcachedRestChannel}
 */

package org.elasticsearch.redis.netty;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;
import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.redis.RedisRestRequest;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.XContentRestResponse;

import java.io.IOException;
import java.nio.charset.Charset;


public class RedisRestChannel implements RestChannel {

    private final ESLogger logger = ESLoggerFactory.getLogger(RedisRestChannel.class.getSimpleName());

    public static final ChannelBuffer CRLF = ChannelBuffers.copiedBuffer("\r\n", Charset.forName("US-ASCII"));

    private final Channel channel;

    private final RedisRestRequest request;

    public RedisRestChannel(Channel channel, RedisRestRequest request) {
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void sendResponse(RestResponse response) {
        if (request.method().equals(RestRequest.Method.GET) || request.method().equals(RestRequest.Method.POST)) {
            int contentLength = 0;
            try {
                contentLength = response.contentLength();
            } catch (IOException e) {
                logger.error("Failed to get content length", e);
            }
            ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer(3 + contentLength);
            writeBuffer.writeByte('+');
            ChannelBuffer buf = ((XContentRestResponse) response).builder().bytes().toChannelBuffer();
            writeBuffer.writeBytes(buf);
            writeBuffer.writeBytes(CRLF.duplicate());
            channel.write(writeBuffer);
        } else if (request.method().equals(RestRequest.Method.DELETE) || request.method().equals(RestRequest.Method.HEAD)) {
            ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer(4);
            writeBuffer.writeByte(':');
            if (response.status().getStatus() > 400) {
                writeBuffer.writeByte('0');
            } else {
                writeBuffer.writeByte('1');
            }
            writeBuffer.writeBytes(CRLF.duplicate());
            channel.write(writeBuffer);
        }
    }
}
