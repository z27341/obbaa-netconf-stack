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

package org.broadband_forum.obbaa.netconf.api.messages;

import org.broadband_forum.obbaa.netconf.api.util.NetconfMessageBuilderException;
import org.broadband_forum.obbaa.netconf.api.NetconfMessage;
import org.broadband_forum.obbaa.netconf.api.NetconfRpcPayLoadType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfRpcRequest extends AbstractNetconfRequest implements NetconfMessage {

    private RpcName m_rpcName;

    private Element m_inputElement;

    public Element getRpcInput() {
        return m_inputElement;
    }

    public NetconfRpcRequest setRpcInput(Element input) {
        this.m_inputElement = input;
        m_rpcName = new RpcName(input.getNamespaceURI(), input.getLocalName());
        return this;
    }

    public RpcName getRpcName() {
        return m_rpcName;
    }

    @Override
    public Document getRequestDocumentInternal() throws NetconfMessageBuilderException {
        Document doc = new PojoToDocumentTransformer().newNetconfRpcDocument(m_messageId).addRpcElement
                (m_inputElement).build();
        return doc;
    }

    @Override
    public NetconfRpcPayLoadType getType() {
        return NetconfRpcPayLoadType.REQUEST;
    }


}
