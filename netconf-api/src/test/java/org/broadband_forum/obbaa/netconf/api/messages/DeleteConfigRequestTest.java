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

import static org.broadband_forum.obbaa.netconf.api.util.TestXML.assertXMLEquals;
import static org.broadband_forum.obbaa.netconf.api.util.TestXML.loadAsXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.broadband_forum.obbaa.netconf.api.util.NetconfMessageBuilderException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class DeleteConfigRequestTest {

    private DeleteConfigRequest m_deleteConfigRequest = new DeleteConfigRequest();
    private static final String TEST_TARGET = "startup";
    private String m_target = TEST_TARGET;
    private String RUNNING = "running";
    private String m_messageId = "101";

    @Test
    public void testGetRequestDocument() throws NetconfMessageBuilderException, SAXException, IOException {

        m_deleteConfigRequest.setTarget(m_target);
        m_deleteConfigRequest.setMessageId(m_messageId);
        assertNotNull(m_deleteConfigRequest.getRequestDocument());
        assertXMLEquals(loadAsXml("deleteConfig.xml"), m_deleteConfigRequest.getRequestDocument().getDocumentElement());
    }

    @Test
    public void testSetAndGetTarget() {

        assertEquals(m_deleteConfigRequest, m_deleteConfigRequest.setTarget(m_target));
        assertEquals(TEST_TARGET, m_deleteConfigRequest.getTarget());
    }

    @Test
    public void testSetTargetRunning() {
        DeleteConfigRequest deleteConfigRequest1 = m_deleteConfigRequest.setTargetRunning();
        assertEquals(RUNNING, deleteConfigRequest1.getTarget());

    }

}
