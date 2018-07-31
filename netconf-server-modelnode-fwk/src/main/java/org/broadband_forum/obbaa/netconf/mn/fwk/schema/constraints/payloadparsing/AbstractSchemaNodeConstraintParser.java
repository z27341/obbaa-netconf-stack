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

package org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.type.builtin.TypeValidatorFactory;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.typevalidators.TypeValidator;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.typevalidators.ValidationException;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.util.ChoiceCaseNodeUtil;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.util.SchemaRegistryUtil;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.IdentityRefConfigAttribute;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeWithAttributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UniqueConstraint;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Relative;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcError;
import org.broadband_forum.obbaa.netconf.api.messages.PojoToDocumentTransformer;
import org.broadband_forum.obbaa.netconf.api.util.NetconfResources;
import org.broadband_forum.obbaa.netconf.api.util.Pair;

import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNode;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNodeId;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNodeRdn;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ConfigLeafAttribute;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DSExpressionValidator;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DataStoreValidationErrors;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DataStoreValidationUtil;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.UniqueConstraintCheck;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.jxpath.JXPathUtils;
import org.broadband_forum.obbaa.netconf.server.rpc.RequestType;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.broadband_forum.obbaa.netconf.stack.logging.AdvancedLogger;

/**
 * Created by keshava on 11/24/15.
 */
public abstract class AbstractSchemaNodeConstraintParser implements SchemaNodeConstraintParser {
    protected final TypeDefinition<?> m_typeDefinition;
    protected final SchemaRegistry m_schemaRegistry;
    protected final DataSchemaNode m_schemaNode;
    protected final DSExpressionValidator m_expValidator;

    public AbstractSchemaNodeConstraintParser(TypeDefinition<?> typeDefinition, DataSchemaNode schemaNode,
                                              SchemaRegistry schemaRegistry,
                                              DSExpressionValidator expValidator) {
        m_typeDefinition = typeDefinition;
        m_schemaNode = schemaNode;
        m_schemaRegistry = schemaRegistry;
        m_expValidator = expValidator;
    }

    @Override
    public void validate(Element dataNode, RequestType requestType) throws ValidationException {
        TypeValidator typeTypeValidator = TypeValidatorFactory.getInstance().getValidator(m_typeDefinition);
        String operationAttribute = dataNode.getAttributeNS(NetconfResources.NETCONF_RPC_NS_1_0, NetconfResources
                .OPERATION);
        if (typeTypeValidator != null && needsTypeValidation(operationAttribute)) {
            try {
                typeTypeValidator.validate(dataNode, false, null);
            } catch (ValidationException e) {
                NetconfRpcError rpcError = e.getRpcError();
                DataSchemaNode parentSchemaNode = m_schemaRegistry.getNonChoiceParent(m_schemaNode.getPath());
                Pair<String, Map<String, String>> errorPathPair = null;
                if (dataNode.getParentNode() != null) {
                    /**
                     * dataNode parent will be null, when the element is constructed back
                     * from DS during type validation in phase 3. In those case, the error path will be
                     * taken care by the caller who generated the element. 
                     */
                    errorPathPair = (parentSchemaNode == null) ? new Pair<String, Map<String, String>>("/",
                            Collections.EMPTY_MAP) : SchemaRegistryUtil.getErrorPath(dataNode.getParentNode(),
                            parentSchemaNode,
                            m_schemaRegistry, dataNode);
                    rpcError.setErrorPath(errorPathPair.getFirst(), errorPathPair.getSecond());
                } else {
                    rpcError.setErrorPath(null, Collections.emptyMap());
                }
                throw e;
            }
        }
    }

    protected void validateType(ConfigLeafAttribute attribute, ModelNode parentModelNode) {
        QName qname = m_schemaNode.getQName();
        Element attributeElement = DataStoreValidationUtil.getValidationDocument().createElementNS(qname.getNamespace
                ().toString(), qname.getLocalName());
        if (attribute.getStringValue() != null) {
            attributeElement.setTextContent(attribute.getStringValue());
        }

        if (attribute instanceof IdentityRefConfigAttribute) {
            /**
             * Only in the case of IdentityRef, the existing set of attributes available in the DOM is recreated. The
             * assumption is that the existing
             * DOM element inside the IdentityRefAttribute has the proper set of attributes saved. This is because,
             * when saving to the data
             * store, the identity ref is stored with the proper NS (even if it is default). Since we are at phase 3,
             * the identity ref
             * constructed back from DS should have the right NS and prefix appended.
             **/
            IdentityRefConfigAttribute refAttribute = (IdentityRefConfigAttribute) attribute;
            Element domElement = refAttribute.getDOMValue();
            NamedNodeMap nodeMap = domElement.getAttributes();
            for (int i = 0; i < nodeMap.getLength(); i++) {
                Node node = nodeMap.item(i);
                if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                    attributeElement.setAttributeNS(node.getNamespaceURI(), PojoToDocumentTransformer.XMLNS + node
                                    .getNodeName().split(":")[1],
                            node.getNodeValue());
                }
            }
        }
        try {
            validate(attributeElement, null);
        } catch (ValidationException e) {
            NetconfRpcError error = e.getRpcError();
            ModelNodeId errorId = new ModelNodeId(parentModelNode.getModelNodeId());
            errorId.addRdn(new ModelNodeRdn(ModelNodeRdn.CONTAINER, qname.getNamespace().toString(), qname
                    .getLocalName()));
            error.setErrorPath(errorId.xPathString(m_schemaRegistry), errorId.xPathStringNsByPrefix(m_schemaRegistry));
            throw e;
        }
    }

    protected boolean needsTypeValidation(String operationAttribute) {
        return true;
    }

    protected void validateUniqueConstraint(Collection<UniqueConstraint> constraints, ModelNode parentModelNode,
                                            Collection<ModelNode> childModelNodes, String childName, String
                                                    namespace) throws ValidationException {
        Set<QName> uniqueQNames = new HashSet<>();
        SchemaRegistry registry = parentModelNode.getSchemaRegistry();
        for (UniqueConstraint constraint : constraints) {
            for (Relative relative : constraint.getTag()) {
                uniqueQNames.add(relative.getLastComponent());
            }
        }

        BiMap<ModelNode, UniqueConstraintCheck> modelNodeToAllUniqueAttr = HashBiMap.create();
        for (ModelNode childModelNode : childModelNodes) {
            ModelNodeWithAttributes attr = (ModelNodeWithAttributes) childModelNode;
            Map<QName, ConfigLeafAttribute> attributes = attr.getAttributes();
            if (attributes.keySet().containsAll(uniqueQNames)) {
                UniqueConstraintCheck unique = new UniqueConstraintCheck();
                for (QName qName : uniqueQNames) {
                    unique.m_attributes.put(qName, attributes.get(qName).getStringValue());
                }
                try {
                    modelNodeToAllUniqueAttr.put(childModelNode, unique);
                } catch (IllegalArgumentException e) {
                    ValidationException violateUniqueException = DataStoreValidationErrors
                            .getUniqueConstraintException(e.getMessage());
                    ModelNodeId id = buildModelNodeId(parentModelNode, childName, namespace);
                    violateUniqueException.getRpcError().setErrorPath(id.xPathString(registry), id
                            .xPathStringNsByPrefix(registry));
                    throw violateUniqueException;
                }
            }
        }
    }

    protected void validateSizeRange(ConstraintDefinition constraints, ModelNode parentModelNode, int currentSize,
                                     String childName, String namespace)
            throws ValidationException {
        SchemaRegistry registry = parentModelNode.getSchemaRegistry();
        Integer minElements = constraints.getMinElements();
        if (minElements != null && currentSize < minElements) {
            ValidationException violateMinException = DataStoreValidationErrors.getViolateMinElementException
                    (childName, minElements);
            ModelNodeId id = buildModelNodeId(parentModelNode, childName, namespace);
            violateMinException.getRpcError().setErrorPath(id.xPathString(registry), id.xPathStringNsByPrefix
                    (registry));
            throw violateMinException;
        }

        Integer maxElements = constraints.getMaxElements();
        if (maxElements != null && currentSize > maxElements) {
            ValidationException violateMaxException = DataStoreValidationErrors.getViolateMaxElementException
                    (childName, maxElements);
            ModelNodeId id = buildModelNodeId(parentModelNode, childName, namespace);
            violateMaxException.getRpcError().setErrorPath(id.xPathString(registry), id.xPathStringNsByPrefix
                    (registry));
            throw violateMaxException;
        }
    }

    protected ModelNodeId buildModelNodeId(ModelNode modeNode, String childName, String namespace) {
        ModelNodeId id = new ModelNodeId(modeNode.getModelNodeId());
        id.addRdn(new ModelNodeRdn(ModelNodeRdn.CONTAINER, namespace, childName));
        return id;
    }

    protected void validateChoiceCase(ModelNode modelNode, DataSchemaNode schemaNode) {
        SchemaPath schemaPath = schemaNode.getPath().getParent();
        if (schemaPath != null) {
            ChoiceSchemaNode choiceSchemaNode = ChoiceCaseNodeUtil.getChoiceSchemaNodeFromCaseNodeSchemaPath
                    (m_schemaRegistry, schemaPath);
            if (choiceSchemaNode != null) {
                DataStoreValidationUtil.getValidationContext().setChildOfChoiceCase(schemaNode);
                if (choiceSchemaNode.getConstraints().getWhenCondition() != null) {
                    RevisionAwareXPath awareXPath = choiceSchemaNode.getConstraints().getWhenCondition();
                    if (isDataAvailable(modelNode) && awareXPath != null) {
                        m_expValidator.validateWhen(JXPathUtils.getExpression(awareXPath.toString()),
                                choiceSchemaNode, modelNode);
                    }
                }
                DataSchemaNode choiceCaseNode = m_schemaRegistry.getDataSchemaNode(schemaPath);
                if (choiceCaseNode != null && choiceCaseNode.getConstraints().getWhenCondition() != null) {
                    RevisionAwareXPath awareXPath = choiceCaseNode.getConstraints().getWhenCondition();
                    if (isDataAvailable(modelNode) && awareXPath != null) {
                        m_expValidator.validateWhen(JXPathUtils.getExpression(awareXPath.toString()), choiceCaseNode,
                                modelNode);
                    }
                }
            }
        }
    }


    protected abstract AdvancedLogger getLogger();

    protected abstract boolean isDataAvailable(ModelNode parentNode);
}
