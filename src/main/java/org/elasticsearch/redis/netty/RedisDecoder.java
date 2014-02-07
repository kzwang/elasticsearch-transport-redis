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
 * Modified from {@link org.elasticsearch.memcached.netty.MemcachedDecoder}
 */

package org.elasticsearch.redis.netty;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;
import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.ExceptionEvent;
import org.elasticsearch.common.netty.handler.codec.frame.FrameDecoder;
import org.elasticsearch.redis.RedisRestRequest;
import org.elasticsearch.redis.RedisTransportException;
import org.elasticsearch.rest.RestRequest;

import java.nio.charset.Charset;

public class RedisDecoder extends FrameDecoder {

    private final ESLogger logger;

    private static final String NOT_SUPPORT_STRING = "Not supported command\r\n";

    public static final ChannelBuffer NOT_SUPPORT = ChannelBuffers.copiedBuffer(NOT_SUPPORT_STRING, Charset.forName("US-ASCII"));

    public RedisDecoder(ESLogger logger) {
        super(false);
        this.logger = logger;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        int readableBytes = buffer.readableBytes();
        if (readableBytes < 8) {
            return null;
        }

        buffer.resetReaderIndex();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < readableBytes; i++) {
            byte next = buffer.readByte();
            sb.append((char) next);
        }

        String[] command = sb.toString().split("\r\n");

        if (logger.isDebugEnabled()) {
            logger.debug("Received command: {}", sb.toString());
        }

        if (command.length < 2) {
            return null;
        }

        String cmd = command[2];
        String uri = null;
        String data = null;
        RestRequest.Method method;

        if (command.length >= 4) {
            uri = command[4];
        }

        if (command.length >= 6) {
            data = command[6];
        }

        if (cmd.equalsIgnoreCase("set")) {
            if (data != null && (data.startsWith("put") || data.startsWith("PUT"))) {
                data = data.substring(3);  // remove start 'PUT'
                method = RestRequest.Method.PUT;
            } else {
                method = RestRequest.Method.POST;
            }
        } else if (cmd.equalsIgnoreCase("get")) {
            method = RestRequest.Method.GET;
        } else if (cmd.equalsIgnoreCase("del")) {
            method = RestRequest.Method.DELETE;
        } else if (cmd.equalsIgnoreCase("exists")) {
            method = RestRequest.Method.HEAD;
        } else if (cmd.equalsIgnoreCase("quit")) {
            if (channel.isConnected()) {
                channel.disconnect();
            }
            return null;
        } else {
            logger.error("Unsupported command [{}], ignoring", cmd);

            ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer(1 + NOT_SUPPORT_STRING.length());
            writeBuffer.writeByte('-');
            writeBuffer.writeBytes(NOT_SUPPORT.duplicate());
            channel.write(writeBuffer);
            return null;
        }

        RedisRestRequest request = new RedisRestRequest(method, uri);
        if (data != null) {
            request.setData(new BytesArray(data));
        }

        return request;

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (ctx.getChannel().isConnected()) {
            ctx.getChannel().disconnect();
        }

        logger.error("caught exception on redis decoder", e.getCause());
    }
}
