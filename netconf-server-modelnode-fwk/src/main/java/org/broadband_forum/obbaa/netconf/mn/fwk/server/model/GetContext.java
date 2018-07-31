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

package org.broadband_forum.obbaa.netconf.mn.fwk.server.model;

import org.w3c.dom.Document;


public class GetContext {
    private Document m_doc;
    private FilterNode m_filter;
    private StateAttributeGetContext m_stateAttributeContext;

    public GetContext(Document doc, FilterNode filter, StateAttributeGetContext getStateAttributeContext) {
        m_doc = doc;
        m_filter = filter;
        m_stateAttributeContext = getStateAttributeContext;
    }

    public Document getDoc() {
        return m_doc;
    }

    public void setDoc(Document doc) {
        m_doc = doc;
    }

    public FilterNode getFilter() {
        return m_filter;
    }

    public void setFilter(FilterNode filterNode) {
        m_filter = filterNode;
    }

    public StateAttributeGetContext getStateAttributeContext() {
        return m_stateAttributeContext;
    }

    public void setStateAttributeContext(StateAttributeGetContext stateAttributeContext) {
        this.m_stateAttributeContext = stateAttributeContext;
    }


}