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

package org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.dsm;

import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.ALBUM_LOCAL_NAME;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.ALBUM_SCHEMA_PATH;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.ARTIST_LOCAL_NAME;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.JB_NS;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.JUKEBOX_LOCAL_NAME;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.LIBRARY_LOCAL_NAME;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.NAME_QNAME;
import static org.broadband_forum.obbaa.netconf.persistence.test.entities.jukebox3.JukeboxConstants.SINGER_QNAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNodeId;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNodeRdn;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.SetAttributeException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.datastore.ModelNodeKey;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeWithAttributes;
import org.broadband_forum.obbaa.netconf.mn.fwk.util.NoLockService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaBuildException;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaRegistryImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.GetAttributeException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.datastore.ModelNodeDataStoreManager;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ConfigLeafAttribute;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.GenericConfigAttribute;

import org.broadband_forum.obbaa.netconf.server.util.TestUtil;

public class DsmChildLeafListHelperTest {

    private static final String EXAMPLE_JUKEBOX_YANGFILE = "/dsmchildleaflisthelpertest/example-jukebox.yang";
    private static final String EXAMPLE_JUKEBOX_YANGTYPES = "/dsmchildleaflisthelpertest/example-jukebox-types.yang";
    private SchemaRegistry m_schemaRegistry;
    ModelNodeDataStoreManager m_modelNodeDSM;
    private static final String ARTIST_NAME = "Artist";
    private static final String ALBUM_NAME = "album";
    private ModelNodeId m_jukeboxNodeId = new ModelNodeId().addRdn(new ModelNodeRdn(ModelNodeRdn.CONTAINER, JB_NS,
            JUKEBOX_LOCAL_NAME));
    private ModelNodeId m_libraryNodeId = new ModelNodeId(m_jukeboxNodeId).addRdn(new ModelNodeRdn(ModelNodeRdn
            .CONTAINER, JB_NS, LIBRARY_LOCAL_NAME));
    private ModelNodeId m_artistNodeId = new ModelNodeId(m_libraryNodeId).addRdn(new ModelNodeRdn(ModelNodeRdn
            .CONTAINER, JB_NS, ARTIST_LOCAL_NAME))
            .addRdn(new ModelNodeRdn(NAME_QNAME, ARTIST_NAME));
    private ModelNodeId m_albumNodeId = new ModelNodeId(m_artistNodeId).addRdn(new ModelNodeRdn(ModelNodeRdn
            .CONTAINER, JB_NS, ALBUM_LOCAL_NAME))
            .addRdn(new ModelNodeRdn(NAME_QNAME, ALBUM_NAME));

    @Before
    public void setup() throws SchemaBuildException {
        m_modelNodeDSM = mock(ModelNodeDataStoreManager.class);
        List<YangTextSchemaSource> yangs = TestUtil.getJukeBoxDeps();
        yangs.add(TestUtil.getByteSource(EXAMPLE_JUKEBOX_YANGTYPES));
        yangs.add(TestUtil.getByteSource(EXAMPLE_JUKEBOX_YANGFILE));
        m_schemaRegistry = new SchemaRegistryImpl(yangs, new NoLockService());
    }

    @Test
    public void testAddChild() throws SetAttributeException, GetAttributeException {
        DsmChildLeafListHelper childLeafListHelper = Mockito.spy(new DsmChildLeafListHelper(mock(LeafListSchemaNode
                .class), SINGER_QNAME,
                m_modelNodeDSM, m_schemaRegistry));

        ModelNodeWithAttributes albumModelNode = new ModelNodeWithAttributes(ALBUM_SCHEMA_PATH, m_albumNodeId,
                null, null, m_schemaRegistry, m_modelNodeDSM);
        populateValuesInModelNode(albumModelNode);

        ModelNodeWithAttributes albumModelNode1 = new ModelNodeWithAttributes(ALBUM_SCHEMA_PATH, m_albumNodeId,
                null, null, m_schemaRegistry, m_modelNodeDSM);
        populateValuesInModelNode(albumModelNode1);
        updateModelNode(albumModelNode1);

        when(m_modelNodeDSM.findNode(any(SchemaPath.class), any(ModelNodeKey.class), any(ModelNodeId.class))).
                thenReturn(albumModelNode).thenReturn(albumModelNode1);

        childLeafListHelper.addChild(albumModelNode, new GenericConfigAttribute("singer3"));
        verify(childLeafListHelper).getValue(albumModelNode);

        LinkedHashSet<ConfigLeafAttribute> values = new LinkedHashSet<>();
        values.add(new GenericConfigAttribute("singer1"));
        values.add(new GenericConfigAttribute("singer2"));
        values.add(new GenericConfigAttribute("singer3"));

        verify(m_modelNodeDSM, times(1)).updateNode(albumModelNode, m_artistNodeId, null, Collections.singletonMap
                (SINGER_QNAME, values), false);

        childLeafListHelper.addChild(albumModelNode, new GenericConfigAttribute("singer4"));
        verify(childLeafListHelper, times(2)).getValue(albumModelNode1);
        values.add(new GenericConfigAttribute("singer4"));

        verify(m_modelNodeDSM, times(1)).updateNode(albumModelNode, m_artistNodeId, null, Collections.singletonMap
                (SINGER_QNAME, values), false);
    }

    private void populateValuesInModelNode(ModelNodeWithAttributes modelNode) {
        Map<QName, ConfigLeafAttribute> attributes = new HashMap<>();
        attributes.put(NAME_QNAME, new GenericConfigAttribute("album"));
        modelNode.setAttributes(attributes);

        Map<QName, LinkedHashSet<ConfigLeafAttribute>> leafListAttrs = new HashMap<>();
        LinkedHashSet<ConfigLeafAttribute> singerLeafList = new LinkedHashSet<>();
        singerLeafList.add(new GenericConfigAttribute("singer1"));
        singerLeafList.add(new GenericConfigAttribute("singer2"));
        leafListAttrs.put(SINGER_QNAME, singerLeafList);
        modelNode.setLeafLists(leafListAttrs);
        modelNode.setModelNodeId(new ModelNodeId(m_albumNodeId));
    }

    private void updateModelNode(ModelNodeWithAttributes modelNode) {
        Map<QName, LinkedHashSet<ConfigLeafAttribute>> leafListAttrs = modelNode.getLeafLists();
        LinkedHashSet<ConfigLeafAttribute> singerLeafList = (LinkedHashSet<ConfigLeafAttribute>)
                modelNode.getLeafList(SINGER_QNAME);

        singerLeafList.add(new GenericConfigAttribute("singer3"));
        leafListAttrs.put(SINGER_QNAME, singerLeafList);
        modelNode.setLeafLists(leafListAttrs);
    }
}