/*
 * Copyright 2018 Broadband Forum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadband_forum.obbaa.netconf.client.ssh;

import org.broadband_forum.obbaa.netconf.api.util.DocumentUtils;
import org.broadband_forum.obbaa.netconf.api.util.NetconfMessageBuilderException;
import org.broadband_forum.obbaa.netconf.api.util.NetconfResources;
import org.apache.log4j.Logger;
import org.apache.sshd.client.channel.ChannelSubsystem;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.buffer.Buffer;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SshNetconfEOMClientSessionListener implements SshFutureListener<IoReadFuture> {
    ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
    private ChannelSubsystem clientChannel;
    private SshNetconfClientSession clientSession;
    private static final Logger logger = Logger.getLogger(SshNetconfEOMClientSessionListener.class);

    public SshNetconfEOMClientSessionListener(ChannelSubsystem channel, SshNetconfClientSession clientSession) {
        this.clientChannel = channel;
        this.clientSession = clientSession;
    }

    @Override
    public void operationComplete(IoReadFuture future) {
        try {
            if (!(clientChannel.isClosed() || clientChannel.isClosing())) {
                future.verify();
                Buffer buffer = future.getBuffer();
                baosOut.write(buffer.array(), buffer.rpos(), buffer.available());
                if (baosOut.toString().endsWith(NetconfResources.RPC_EOM_DELIMITER)) {
                    String rpcReply = baosOut.toString();
                    rpcReply = rpcReply.substring(0, rpcReply.indexOf(NetconfResources.RPC_EOM_DELIMITER));
                    Document replyDoc = DocumentUtils.stringToDocument(rpcReply);
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("NC Response received from %s : %s", clientChannel.getSession()
                                .getIoSession()
                                .getRemoteAddress(), rpcReply));
                    }
                    clientSession.responseRecieved(replyDoc);
                    baosOut = new ByteArrayOutputStream();
                }
                buffer.rpos(buffer.rpos() + buffer.available());
                buffer.compact();
                clientChannel.getAsyncOut().read(buffer).addListener(this);
            }
        } catch (NetconfMessageBuilderException | IOException e) {
            logger.error("Error while processing request ", e);
        }
    }

}
