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

package org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.yang.validation;

import static org.broadband_forum.obbaa.netconf.server.util.TestUtil.loadAsXml;
import static org.broadband_forum.obbaa.netconf.server.util.TestUtil.transformToElement;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.SubSystem;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.EditConfigException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.NbiNotificationHelper;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.NetConfServerImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ChildContainerHelper;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeFactoryException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeHelperRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeInitException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.RootEntityContainerModelNodeHelper;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.DataStoreValidator;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.DataStoreValidatorImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DSExpressionValidator;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.yang.LocalSubSystem;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeHelperRegistryImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.DataStoreIntegrityService;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.service
        .DataStoreIntegrityServiceImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.util.NoLockService;
import org.junit.Before;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.yang.AbstractValidationTestSetup;

import org.broadband_forum.obbaa.netconf.api.client.NetconfClientInfo;
import org.broadband_forum.obbaa.netconf.api.messages.EditConfigElement;
import org.broadband_forum.obbaa.netconf.api.messages.EditConfigErrorOptions;
import org.broadband_forum.obbaa.netconf.api.messages.EditConfigRequest;
import org.broadband_forum.obbaa.netconf.api.messages.EditConfigTestOptions;
import org.broadband_forum.obbaa.netconf.api.messages.NetConfResponse;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcError;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcErrorSeverity;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcErrorTag;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcErrorType;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcResponse;
import org.broadband_forum.obbaa.netconf.api.messages.StandardDataStores;
import org.broadband_forum.obbaa.netconf.api.util.DocumentUtils;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaBuildException;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaRegistryImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.DataStore;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNode;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.SubSystemRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.SubSystemRegistryImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.AddDefaultDataInterceptor;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.RootModelNodeAggregator;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.RootModelNodeAggregatorImpl;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.inmemory.InMemoryDSM;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.yang.util.YangUtils;
import org.broadband_forum.obbaa.netconf.server.util.TestUtil;

public class AbstractDataStoreValidatorTest extends AbstractValidationTestSetup {
    private static final String YANG_FILE = "/datastorevalidatortest/yangs/datastore-validator-test.yang";
    protected static final QName VALIDATION_QNAME = QName.create("urn:org:bbf:pma:validation", "2015-12-14",
            "validation");
    protected static final QName VALIDATION1_QNAME = QName.create("urn:org:bbf:pma:validation", "2015-12-14",
            "validation1");
    protected static final QName VALIDATION2_QNAME = QName.create("urn:org:bbf:pma:validation", "2015-12-14",
            "validation2");
    protected static final QName VALIDATION3_QNAME = QName.create("urn:org:bbf:pma:validation", "2015-12-14",
            "validation3");
    protected static final SchemaPath VALIDATION_SCHEMA_PATH = SchemaPath.create(true, VALIDATION_QNAME);
    protected static final SchemaPath VALIDATION1_SCHEMA_PATH = SchemaPath.create(true, VALIDATION1_QNAME);
    protected static final SchemaPath VALIDATION2_SCHEMA_PATH = SchemaPath.create(true, VALIDATION2_QNAME);
    protected static final SchemaPath VALIDATION3_SCHEMA_PATH = SchemaPath.create(true, VALIDATION3_QNAME);
    private static final String DEFAULT_XML = "/datastorevalidatortest/yangs/datastore-validator-defaultxml.xml";
    protected static final String NAMESPACE = "urn:org:bbf:pma:validation";
    protected static final String MESSAGE_ID = "1";

    protected RootModelNodeAggregator m_rootModelNodeAggregator;
    protected ModelNodeHelperRegistry m_modelNodeHelperRegistry;
    protected SubSystemRegistry m_subSystemRegistry;
    protected NetConfServerImpl m_server;
    protected DataStoreValidator m_datastoreValidator;
    protected DataStore m_dataStore;
    protected SchemaRegistry m_schemaRegistry;
    protected NetconfClientInfo m_clientInfo;
    protected InMemoryDSM m_modelNodeDsm;
    protected AddDefaultDataInterceptor m_addDefaultDataInterceptor;
    protected ModelNode m_rootModelNode;
    protected DataStoreIntegrityService m_integrityService;

    @Before
    public void setUp() throws ModelNodeInitException, SchemaBuildException {
        m_schemaRegistry = getSchemaRegistry();
        m_modelNodeDsm = new InMemoryDSM(m_schemaRegistry);
        m_modelNodeHelperRegistry = new ModelNodeHelperRegistryImpl(m_schemaRegistry);
        m_subSystemRegistry = new SubSystemRegistryImpl();
        m_server = getNcServer();
        m_expValidator = new DSExpressionValidator(m_schemaRegistry, m_modelNodeHelperRegistry);
        m_integrityService = new DataStoreIntegrityServiceImpl(m_modelNodeHelperRegistry, m_schemaRegistry, m_server);
        m_datastoreValidator = new DataStoreValidatorImpl(m_schemaRegistry, m_modelNodeHelperRegistry,
                m_modelNodeDsm, m_integrityService, m_expValidator);
        m_expValidator = new DSExpressionValidator(m_schemaRegistry, m_modelNodeHelperRegistry);
    }

    protected NetConfServerImpl getNcServer() {
        return new NetConfServerImpl(m_schemaRegistry);
    }

    @Override
    protected SchemaRegistry getSchemaRegistry() throws SchemaBuildException {
        List<YangTextSchemaSource> yangFiles = TestUtil.getByteSources(getYang());
        return new SchemaRegistryImpl(yangFiles, new NoLockService());
    }

    protected List<String> getYang() {
        return Arrays.asList(YANG_FILE);
    }

    protected String getXml() {
        return DEFAULT_XML;
    }

    protected SchemaPath getSchemaPath() {
        return VALIDATION_SCHEMA_PATH;
    }


    protected void getModelNode() throws ModelNodeInitException {
        try {
            YangUtils.deployInMemoryHelpers(getYang(), getSubSystem(), m_modelNodeHelperRegistry,
                    m_subSystemRegistry, m_schemaRegistry, m_modelNodeDsm);
        } catch (ModelNodeFactoryException e) {
            throw new ModelNodeInitException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        m_rootModelNodeAggregator = new RootModelNodeAggregatorImpl(m_schemaRegistry, m_modelNodeHelperRegistry,
                m_modelNodeDsm, m_subSystemRegistry);
        addRootNodeHelpers();
        NbiNotificationHelper nbiNotificationHelper = mock(NbiNotificationHelper.class);
        m_dataStore = new DataStore(StandardDataStores.RUNNING, m_rootModelNodeAggregator, m_subSystemRegistry);
        m_dataStore.setValidator(m_datastoreValidator);
        m_dataStore.setNbiNotificationHelper(nbiNotificationHelper);
        m_server.setRunningDataStore(m_dataStore);
        loadDefaultXml();
        if (!m_rootModelNodeAggregator.getModelServiceRoots().isEmpty()) {
            m_rootModelNode = m_rootModelNodeAggregator.getModelServiceRoots().get(0);
        }
    }

    protected void loadDefaultXml() {
        YangUtils.loadXmlDataIntoServer(m_server, getClass().getResource(getXml()).getPath());
    }

    protected SubSystem getSubSystem() {
        return new LocalSubSystem();
    }

    protected void addRootNodeHelpers() {
        ContainerSchemaNode schemaNode = (ContainerSchemaNode) m_schemaRegistry.getDataSchemaNode(getSchemaPath());
        ChildContainerHelper containerHelper = new RootEntityContainerModelNodeHelper(schemaNode,
                m_modelNodeHelperRegistry, m_subSystemRegistry, m_schemaRegistry,
                m_modelNodeDsm);
        m_rootModelNodeAggregator.addModelServiceRootHelper(getSchemaPath(), containerHelper);

        schemaNode = (ContainerSchemaNode) m_schemaRegistry.getDataSchemaNode(VALIDATION1_SCHEMA_PATH);
        containerHelper = new RootEntityContainerModelNodeHelper(schemaNode,
                m_modelNodeHelperRegistry, m_subSystemRegistry, m_schemaRegistry,
                m_modelNodeDsm);
        m_rootModelNodeAggregator.addModelServiceRootHelper(VALIDATION1_SCHEMA_PATH, containerHelper);

        schemaNode = (ContainerSchemaNode) m_schemaRegistry.getDataSchemaNode(VALIDATION2_SCHEMA_PATH);
        containerHelper = new RootEntityContainerModelNodeHelper(schemaNode,
                m_modelNodeHelperRegistry, m_subSystemRegistry, m_schemaRegistry,
                m_modelNodeDsm);
        m_rootModelNodeAggregator.addModelServiceRootHelper(VALIDATION2_SCHEMA_PATH, containerHelper);

        schemaNode = (ContainerSchemaNode) m_schemaRegistry.getDataSchemaNode(VALIDATION3_SCHEMA_PATH);
        containerHelper = new RootEntityContainerModelNodeHelper(schemaNode,
                m_modelNodeHelperRegistry, m_subSystemRegistry, m_schemaRegistry,
                m_modelNodeDsm);
        m_rootModelNodeAggregator.addModelServiceRootHelper(VALIDATION3_SCHEMA_PATH, containerHelper);
    }

    protected void testPass(String editConfigElement) throws ModelNodeInitException {
        Element configElement = TestUtil.loadAsXml(editConfigElement);
        testPass(configElement);
    }

    protected void testPass(Element configElement) throws ModelNodeInitException {
        getModelNode();
        initialiseInterceptor();
        NetConfResponse response = TestUtil.sendEditConfig(m_server, m_clientInfo, configElement, MESSAGE_ID);
        assertTrue(response.isOk());
    }

    protected void testFail(String editConfigElement, NetconfRpcErrorTag errorTag, NetconfRpcErrorType errorType,
                            NetconfRpcErrorSeverity errorSeverity, String errorAppTag, String errorMessage, String
                                    errorPath) throws
            ModelNodeInitException {
        Element configElement = TestUtil.loadAsXml(editConfigElement);
        testValidate(errorTag, errorType, errorSeverity, errorAppTag, errorMessage, errorPath, configElement);
    }

    @SuppressWarnings("deprecation")
    private void testValidate(NetconfRpcErrorTag errorTag, NetconfRpcErrorType errorType,
                              NetconfRpcErrorSeverity errorSeverity, String errorAppTag, String errorMessage, String
                                          errorPath,
                              Element configElement) throws ModelNodeInitException {
        NetconfRpcError netconfRpcError;
        getModelNode();
        initialiseInterceptor();
        NetConfResponse response = null;
        try {
            response = TestUtil.sendEditConfig(m_server, m_clientInfo, configElement, MESSAGE_ID);
        } catch (Exception e) {
            assertTrue(e instanceof EditConfigException);
            netconfRpcError = ((EditConfigException) e).getRpcError();
            assertNetconfRpcError(errorTag, errorType, errorSeverity, errorAppTag, errorMessage, errorPath,
                    netconfRpcError);
        }
        if (response != null) {
            assertFalse(response.isOk());
            netconfRpcError = response.getErrors().get(0);
            assertNetconfRpcError(errorTag, errorType, errorSeverity, errorAppTag, errorMessage, errorPath,
                    netconfRpcError);
        }
    }

    protected void testFail(Element editConfigElement, NetconfRpcErrorTag errorTag, NetconfRpcErrorType errorType,
                            NetconfRpcErrorSeverity errorSeverity, String errorAppTag, String errorMessage, String
                                    errorPath) throws ModelNodeInitException {
        testValidate(errorTag, errorType, errorSeverity, errorAppTag, errorMessage, errorPath, editConfigElement);
    }

    protected void assertNetconfRpcError(NetconfRpcErrorTag errorTag, NetconfRpcErrorType errorType,
                                         NetconfRpcErrorSeverity errorSeverity,
                                         String errorAppTag, String errorMessage, String errorPath, NetconfRpcError
                                                 netconfRpcError) {
        assertEquals(errorTag, netconfRpcError.getErrorTag());
        assertEquals(errorType, netconfRpcError.getErrorType());
        assertEquals(errorSeverity, netconfRpcError.getErrorSeverity());
        assertEquals(errorAppTag, netconfRpcError.getErrorAppTag());
        assertEquals(errorMessage, netconfRpcError.getErrorMessage());
        assertEquals(errorPath, netconfRpcError.getErrorPath());
    }

    protected void initialiseInterceptor() {
        m_addDefaultDataInterceptor = new AddDefaultDataInterceptor(m_modelNodeHelperRegistry, m_schemaRegistry,
                m_expValidator);
        m_addDefaultDataInterceptor.init();
    }

    protected EditConfigRequest createRequest(String xmlRequest) {
        return new EditConfigRequest().setTargetRunning().setTestOption(EditConfigTestOptions.SET)
                .setErrorOption(EditConfigErrorOptions.STOP_ON_ERROR)
                .setConfigElement(new EditConfigElement().addConfigElementContent(loadAsXml(xmlRequest)));
    }

    public static EditConfigRequest createRequestFromString(String xmlRequest) {
        return new EditConfigRequest().setTargetRunning().setTestOption(EditConfigTestOptions.SET)
                .setErrorOption(EditConfigErrorOptions.STOP_ON_ERROR)
                .setConfigElement(new EditConfigElement().addConfigElementContent(transformToElement(xmlRequest)));
    }


    protected void addOutputElements(Element doc, NetconfRpcResponse response) {
        List<Element> elements = DocumentUtils.getChildElements(doc);
        for (Element element : elements) {
            response.addRpcOutputElement(element);
        }
    }

    protected void verifyGet(String expectedOutput) throws SAXException, IOException {
        super.verifyGet(m_server, m_clientInfo, expectedOutput);
    }

    @Override
    public void setup() throws Exception {

    }

    protected InMemoryDSM getInMemoryDSM() {
        return m_modelNodeDsm;
    }

}
