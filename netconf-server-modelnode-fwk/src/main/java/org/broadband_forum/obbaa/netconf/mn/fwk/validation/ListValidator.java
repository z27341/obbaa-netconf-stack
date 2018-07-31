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

package org.broadband_forum.obbaa.netconf.mn.fwk.validation;

import static org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.MandatoryTypeConstraintParser
        .checkMandatoryElementExists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.AbstractSchemaNodeConstraintParser;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.type.builtin.TypeValidatorFactory;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.typevalidators.TypeValidator;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.typevalidators.ValidationException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ChildListHelper;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeHelperRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.util.NetconfRpcErrorUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UniqueConstraint;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.broadband_forum.obbaa.netconf.api.messages.EditConfigOperations;
import org.broadband_forum.obbaa.netconf.api.messages.InsertOperation;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcError;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcErrorInfo;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcErrorTag;
import org.broadband_forum.obbaa.netconf.api.messages.NetconfRpcErrorType;
import org.broadband_forum.obbaa.netconf.api.util.NetconfResources;
import org.broadband_forum.obbaa.netconf.api.util.Pair;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.SchemaRegistry;
import org.broadband_forum.obbaa.netconf.mn.fwk.schema.constraints.payloadparsing.util.SchemaRegistryUtil;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNode;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ConfigLeafAttribute;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.ModelNodeGetException;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.DataStoreConstraintValidator;

import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DSExpressionValidator;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DataStoreValidationUtil;

import org.broadband_forum.obbaa.netconf.server.rpc.RequestType;
import org.broadband_forum.obbaa.netconf.stack.logging.AdvancedLogger;
import org.broadband_forum.obbaa.netconf.stack.logging.LoggerFactory;

/**
 * Single validation class, which performs validation of a List
 * at different phases
 * <p>
 * validate(Element) -> phase 1 validation
 * validate(ModelNode) -> phase 2 validation
 */

public class ListValidator extends AbstractSchemaNodeConstraintParser implements DataStoreConstraintValidator {
    private final SchemaRegistry m_schemaRegistry;
    private final ModelNodeHelperRegistry m_modelNodeHelperRegistry;
    private final ListSchemaNode m_listSchemaNode;
    private static final AdvancedLogger LOGGER = LoggerFactory.getLogger(ListValidator.class, "netconf-stack",
            "DEBUG", "GLOBAL");

    private void validateKeys(Element dataNode) throws ValidationException {
        List<QName> keys = m_listSchemaNode.getKeyDefinition();
        NodeList childNodes = dataNode.getChildNodes();
        List<QName> missingKeys = new ArrayList<>(keys);
        List<QName> misplacedKeys = new ArrayList<>(keys);
        List<Node> keyNodes = new ArrayList<>();
        int keyNr = 0;
        // the first N children need to be the N keys
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                QName qName = SchemaRegistryUtil.getChildQname(child, m_listSchemaNode, m_schemaRegistry);
                if (missingKeys.contains(qName)) {
                    missingKeys.remove(qName);
                }
                if (keys.get(keyNr).equals(qName)) {
                    misplacedKeys.remove(qName);
                    keyNodes.add(child);
                }
                keyNr++;
                if (keyNr >= keys.size()) {
                    break;
                }
            }
        }
        if (missingKeys.isEmpty() && misplacedKeys.isEmpty()) {
            validateKeyNodesOperationAndEmpty(keyNodes);//keyNodes = list keys
            return;
        } else if (!missingKeys.isEmpty()) {
            List<String> missingKeyNames = new ArrayList<>();
            for (QName qname : missingKeys) {
                missingKeyNames.add(qname.getLocalName());
            }

            NetconfRpcError rpcError = NetconfRpcError.getMissingKeyError(missingKeyNames, NetconfRpcErrorType
                    .Application);
            throwRPCValidationException(dataNode, rpcError);
        } else {
            List<String> misplacedKeyNames = new ArrayList<>();
            for (QName qname : misplacedKeys) {
                misplacedKeyNames.add(qname.getLocalName());
            }

            NetconfRpcError rpcError = NetconfRpcError.getMisplacedKeyError(misplacedKeyNames, NetconfRpcErrorType
                    .Application);
            throwRPCValidationException(dataNode, rpcError);
        }
    }

    private void throwRPCValidationException(Element dataNode, NetconfRpcError rpcError) {
        Pair<String, Map<String, String>> errorPathPair = SchemaRegistryUtil.getErrorPath(dataNode, m_listSchemaNode,
                m_schemaRegistry, (Element) null);
        rpcError.setErrorPath(errorPathPair.getFirst(), errorPathPair.getSecond());
        throw new ValidationException(rpcError);
    }

    /*
     * Don't allow to delete on key leaf
     */
    private void validateKeyNodesOperationAndEmpty(List<Node> keyNodes) throws ValidationException {
        for (Node node : keyNodes) {
            Node operation = node.getAttributes().getNamedItemNS(NetconfResources.NETCONF_RPC_NS_1_0,
                    NetconfResources.OPERATION);
            if (operation != null && EditConfigOperations.DELETE.equals(operation.getNodeValue())) {
                NetconfRpcError rpcError = NetconfRpcErrorUtil.getApplicationError(NetconfRpcErrorTag
                        .OPERATION_FAILED, "Can not delete the key attribute '" + node.getNodeName() + "'");
                throw new ValidationException(rpcError);
            }
        }
    }

    private void validateMandatoryElement(Element dataNode) throws ValidationException {
        Collection<DataSchemaNode> childNodes = m_listSchemaNode.getChildNodes();
        checkMandatoryElementExists(dataNode, childNodes, m_listSchemaNode, m_schemaRegistry);
    }

    private void validateInsertAttributes(Element dataNode) throws ValidationException {
        String insert = getInsertAttributes(dataNode, NetconfResources.INSERT);
        String key = getInsertAttributes(dataNode, NetconfResources.KEY);

        if (InsertOperation.AFTER.equals(insert) || InsertOperation.BEFORE.equals(insert)) { //must have a 'value'
            // attribute
            if (m_listSchemaNode.isUserOrdered()) { //if ordered-by user , need to valid to value attribute is valid
                // type
                if (key == null) {
                    throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                            "Key attribute can't be null or empty"));
                }
                // Validation for the keys in "key" attribute
                Map<QName, String> keyPairs = validateKeyPredicates(dataNode, key);
                // Validation for the key by the type of list
                validateTypeKeyAttributes(dataNode, keyPairs);

            } else { // if ordered-by system, the attributes "before" and "after" can't present
                //Throw an "unknown-attribute" tag in the rpc-error
                throw new ValidationException(getUnknownAttributesError(String.format("There is an unknown-attribute " +
                                "'%s' in element '%s'",
                        insert, dataNode.getLocalName())));

            }
        }

    }

    private static String getInsertAttributes(Element element, String localName) {
        String attribute = element.getAttributeNS(NetconfResources.NETCONF_YANG_1, localName);
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute;
    }

    private boolean isKeyPredicate(String keyPredicate) {
        String regex = "((\\w|\\W)+:(\\w|\\W)+='(\\w|\\W)+')|((\\w|\\W)+='(\\w|\\W)+')";
        return keyPredicate.matches(regex);
    }

    private boolean isStringKeyPredicate(String str) {
        return str.startsWith("[") && str.endsWith("]");
    }

    private Map<QName, String> validateKeyPredicates(Element dataNode, String keyAttribute) throws ValidationException {
        Map<QName, String> keyPairs = new HashMap<QName, String>();
        String listNamespace = dataNode.getNamespaceURI();
        String regex = "\\]\\["; // split keys
        if (!isStringKeyPredicate(keyAttribute)) {
            throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                    String.format("Key '%s' attribute is not the key predicates format.", keyAttribute)));
        }
        String keyAttributeFix = keyAttribute.substring(1, keyAttribute.length() - 1); // remove '[', ']'
        List<String> strKeys = Arrays.asList(keyAttributeFix.split(regex));
        Set<QName> addedKeys = new HashSet<QName>(); // check duplicate keys
        List<QName> keys = m_listSchemaNode.getKeyDefinition(); // key must present in key attribute ;
        for (String strKey : strKeys) {
            //validate for strKey
            if (!isKeyPredicate(strKey)) {
                throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                        String.format("'%s' is not a key predicate format.", "[" + strKey + "]")));
            }

            String key = "";
            String value = "";
            if (strKey.indexOf(":") >= 0) {// contains prefix
                String prefix = strKey.substring(0, strKey.indexOf(":"));
                String prefixNamespcace = getNamespaceFromPrefix(dataNode, prefix);
                key = strKey.substring(strKey.indexOf(":") + 1, strKey.indexOf("=")).trim();
                // validate the namespace
                if (prefixNamespcace == null) {

                    throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                            String.format("There is an unknown prefix '%s' in key '%s' attribute", prefix,
                                    keyAttribute)));
                } else if (!prefixNamespcace.equals(listNamespace)) {
                    throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                            String.format("There is an unknown key '%s' in key '%s' attribute", prefix + ":" + key,
                                    keyAttribute)));
                }

            } else {
                key = strKey.substring(0, strKey.indexOf("="));
            }
            value = strKey.substring(strKey.indexOf("=") + 1, strKey.length()).trim();
            value = value.substring(value.indexOf("'") + 1, value.lastIndexOf("'"));
            //validate the key
            QName qNameKey = validateKeyName(keys, addedKeys, key, keyAttribute);
            keyPairs.put(qNameKey, value);
        }

        //Validate the missing key
        List<QName> missingKeys = new ArrayList<QName>();
        for (QName qNameKey : keys) {
            if (!addedKeys.contains(qNameKey)) {
                missingKeys.add(qNameKey);
            }
        }
        if (missingKeys.size() > 0) {
            throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                    String.format("Missing key '%s' in key '%s' attribute", getStringKeysMissing(missingKeys),
                            keyAttribute)));
        }

        return keyPairs; // must return a map of full key/value within the list
    }

    private QName validateKeyName(List<QName> keyDefinition, Set<QName> addedKeys, String keyName, String
            keyAttribute) throws ValidationException {
        QName keyDefined = null;
        for (QName key : keyDefinition) {
            if (key.getLocalName().equals(keyName)) {
                keyDefined = key;
                break;
            }
        }
        if (keyDefined == null) {
            // key not existed
            throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                    String.format("There is an unknown key '%s' in key '%s' attribute", keyName, keyAttribute)));
        }
        if (!addedKeys.add(keyDefined)) {
            // key duplicated
            throw new ValidationException(getBadInsertAttributesError(NetconfResources.KEY,
                    String.format("There is a duplicated key '%s' in key '%s' attribute", keyName, keyAttribute)));
        }
        return keyDefined;
    }

    private void validateTypeKeyAttributes(Element dataNode, Map<QName, String> keyPairs) throws ValidationException {
        for (QName key : keyPairs.keySet()) {
            DataSchemaNode dataSchemaNode = m_listSchemaNode.getDataChildByName(key);
            if (dataSchemaNode instanceof LeafSchemaNode) {
                TypeValidator validator = TypeValidatorFactory.getInstance().getValidator(((LeafSchemaNode)
                        dataSchemaNode).getType());
                try {
                    validator.validate(dataNode, true, keyPairs.get(key));
                } catch (ValidationException e) {
                    NetconfRpcError rpcError = e.getRpcError();
                    DataSchemaNode parentSchemaNode = m_schemaRegistry.getNonChoiceParent(m_listSchemaNode.getPath());
                    //String errorPath = (parentSchemaNode == null) ? "/" : SchemaRegistryUtil.getErrorPath(dataNode
                    // .getParentNode(), parentSchemaNode,
                    //	m_schemaRegistry, dataNode.getLocalName());

                    Pair<String, Map<String, String>> errorPathPair = (parentSchemaNode == null) ? new Pair<String,
                            Map<String, String>>("/", Collections.EMPTY_MAP) : SchemaRegistryUtil.getErrorPath
                            (dataNode.getParentNode(), parentSchemaNode,
                            m_schemaRegistry, dataNode);
                    rpcError = NetconfRpcErrorUtil.getApplicationError(NetconfRpcErrorTag.BAD_ATTRIBUTE, e
                            .getRpcError().getErrorMessage());
                    rpcError.setErrorPath(errorPathPair.getFirst(), errorPathPair.getSecond());
                    rpcError.addErrorInfoElement(NetconfRpcErrorInfo.BadAttribute, NetconfResources.KEY);
                    throw new ValidationException(rpcError);
                }
            } else {
                throw new ValidationException(String.format("Key %s is not a leaf type", key.getLocalName()));
            }
        }
    }

    private String getNamespaceFromPrefix(Element element, String prefix) {
        String namespaceAttribute = "xmlns" + ":" + prefix;
        String namespaceValue = element.getAttribute(namespaceAttribute);
        if (namespaceValue != null && !namespaceValue.isEmpty()) {
            return namespaceValue;
        }

        Node parentNode = element.getParentNode();
        while (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
            Node namedItem = parentNode.getAttributes().getNamedItem(namespaceAttribute);
            if (namedItem != null) {
                return namedItem.getTextContent();
            }

            parentNode = parentNode.getParentNode();
        }

        return null;
    }

    private String getStringKeysMissing(List<QName> missingKeys) {
        StringBuilder strBuilder = new StringBuilder();
        for (QName key : missingKeys) {
            strBuilder.append(key.getLocalName()).append(", ");
        }
        if (strBuilder.length() > 0) {
            strBuilder.deleteCharAt(strBuilder.length() - 1);
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }
        return strBuilder.toString();
    }

    private NetconfRpcError getBadInsertAttributesError(String badAttributeName, String errorMessage) {
        NetconfRpcError rpcError = NetconfRpcError.getBadAttributeError(badAttributeName,
                NetconfRpcErrorType.Application, errorMessage);
        rpcError.setErrorAppTag("missing-instance");
        return rpcError;
    }

    private NetconfRpcError getUnknownAttributesError(String errorMessage) {
        NetconfRpcError rpcError = NetconfRpcErrorUtil.getApplicationError(NetconfRpcErrorTag.UNKNOWN_ATTRIBUTE,
                errorMessage);
        return rpcError;
    }

    private void validateSizeRange(ModelNode modelNode) throws ValidationException {
        if (m_listSchemaNode.getConstraints().getMinElements() != null || m_listSchemaNode.getConstraints()
                .getMaxElements() != null) {
            Collection<ModelNode> childModelNodes = getChildListModelNodeFromParentNode(modelNode);
            validateSizeRange(m_listSchemaNode.getConstraints(), modelNode, childModelNodes.size(),
                    m_listSchemaNode.getQName().getLocalName(), m_listSchemaNode.getQName().getNamespace().toString());
        }
    }

    private void validateUniqueConstraint(ModelNode modelNode) throws ValidationException {
        Collection<UniqueConstraint> uniqueConstraints = m_listSchemaNode.getUniqueConstraints();
        if (uniqueConstraints != null && !uniqueConstraints.isEmpty()) {
            Collection<ModelNode> childModelNodes = getChildListModelNodeFromParentNode(modelNode);
            validateUniqueConstraint(m_listSchemaNode.getUniqueConstraints(), modelNode, childModelNodes,
                    m_listSchemaNode.getQName().getLocalName(), m_listSchemaNode.getQName().getNamespace().toString());
        }
    }

    private Collection<ModelNode> getChildListModelNodeFromParentNode(ModelNode parentNode) {
        Collection<ModelNode> childModelNodes = new ArrayList<>();
        ChildListHelper childListHelper = m_modelNodeHelperRegistry.getChildListHelper(parentNode
                .getModelNodeSchemaPath(), m_listSchemaNode.getQName());
        if (childListHelper != null) {
            try {
                childModelNodes = childListHelper.getValue(parentNode, Collections.<QName, ConfigLeafAttribute>emptyMap());
            } catch (ModelNodeGetException e) {
                LOGGER.error("Error when getting child ModelNodes ChildListHelper.getValue(ModelNode, Map)", e);
            }
        }
        return childModelNodes;
    }


    public ListValidator(SchemaRegistry schemaRegistry, ModelNodeHelperRegistry modelNodeHelperRegistry, ListSchemaNode schemaNode,
                         DSExpressionValidator expValidator) {
        super(null, schemaNode, schemaRegistry, expValidator);
        m_schemaRegistry = schemaRegistry;
        m_modelNodeHelperRegistry = modelNodeHelperRegistry;
        m_listSchemaNode = schemaNode;
    }

    @Override
    public DataSchemaNode getDataSchemaNode() {
        return m_listSchemaNode;
    }

    /* (non-Javadoc)
     * @see SchemaNodeConstraintParser#validate(org.w3c.dom.Element)
     */
    @Override
    public void validate(Element dataNode, RequestType requestType) throws ValidationException {
        validateInsertAttributes(dataNode);
        validateKeys(dataNode);
        if (DataStoreValidationUtil.needsFurtherValidation(dataNode, requestType)) {
            validateMandatoryElement(dataNode);
        }
    }

    /* (non-Javadoc)
     * @see DataStoreConstraintValidator#validate(ModelNode)
     */
    @Override
    public void validate(ModelNode modelNode) throws ValidationException {
        validateChoiceCase(modelNode, m_listSchemaNode);
        validateSizeRange(modelNode);
        validateUniqueConstraint(modelNode);
    }

    @Override
    protected AdvancedLogger getLogger() {
        return LOGGER;
    }

    @Override
    protected boolean isDataAvailable(ModelNode modelNode) {
        Collection<ModelNode> childModelNodes = getChildListModelNodeFromParentNode(modelNode);
        return childModelNodes != null && childModelNodes.size() > 0;
    }


}
