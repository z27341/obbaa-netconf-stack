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

package org.broadband_forum.obbaa.netconf.mn.fwk.schema;

import java.util.Collection;
import java.util.List;

import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import org.broadband_forum.obbaa.netconf.stack.logging.AdvancedLogger;
import org.broadband_forum.obbaa.netconf.stack.logging.LoggerFactory;

/**
 * Traverses the schema registry and calls visitors for each node encountered.
 * Created by keshava on 12/9/15.
 */
public class SchemaRegistryTraverser {
    private final SchemaRegistry m_schemaRegistry;
    private final List<SchemaRegistryVisitor> m_visitors;
    private final Module m_module;
    private static final AdvancedLogger LOGGER = LoggerFactory.getLogger(SchemaRegistryTraverser.class,
            "netconf-server-datastore", "DEBUG", "GLOBAL");
    private final String m_componentId;

    /**
     * Create a traverser that traverses the SchemaRegistry from the root nodes of the given module
     * and also from the given schemaPaths.
     *
     * @param visitor        - visitor instance which will be called when visiting a node.
     * @param schemaRegistry
     * @param module         - can be null, supply a module if you want to traverse all nodes in the module.
     */
    public SchemaRegistryTraverser(String componentId, List<SchemaRegistryVisitor> visitors, SchemaRegistry
            schemaRegistry, Module module) {
        m_visitors = visitors;
        m_schemaRegistry = schemaRegistry;
        m_module = module;
        m_componentId = componentId;
    }


    public void traverse() {
        traverseModule();
    }

    private void traverseModule() {
        if (m_module != null) {
            for (DataSchemaNode moduleRoot : m_module.getChildNodes()) {
                for (SchemaRegistryVisitor visitor : m_visitors) {
                    visitAndTraverse(null, moduleRoot, visitor);
                }
            }
            for (AugmentationSchema augmentationSchema : m_module.getAugmentations()) {
                SchemaPath targetPath = augmentationSchema.getTargetPath();
                if (m_schemaRegistry != null) {
                    DataSchemaNode augmentedNode = m_schemaRegistry.getDataSchemaNode(targetPath);
                    Collection<DataSchemaNode> children = m_schemaRegistry.getChildren(targetPath);
                    for (DataSchemaNode child : children) {
                        for (DataSchemaNode augmentingNode : augmentationSchema.getChildNodes()) {
                            if (child.getQName().equals(augmentingNode.getQName())) {
                                for (SchemaRegistryVisitor visitor : m_visitors) {
                                    visitAndTraverse(augmentedNode.getPath(), child, visitor);
                                }
                            }
                        }
                    }

                }

            }
            for (IdentitySchemaNode identitySchemaNode : m_module.getIdentities()) {
                for (SchemaRegistryVisitor visitor : m_visitors) {
                    visitor.visitIdentityNode(m_componentId, identitySchemaNode);
                }
            }
        }
    }

    private void visitAndTraverse(SchemaPath parentPath, DataSchemaNode subtreeRoot, SchemaRegistryVisitor visitor) {
        SchemaPath subtreeRootPath = subtreeRoot.getPath();
        visitor.visitEnter(m_componentId, parentPath, subtreeRootPath);
        if (subtreeRoot instanceof ContainerSchemaNode) {
            visitor.visitContainerNode(m_componentId, parentPath, (ContainerSchemaNode) subtreeRoot);
        } else if (subtreeRoot instanceof ListSchemaNode) {
            visitor.visitListNode(m_componentId, parentPath, (ListSchemaNode) subtreeRoot);
        } else if (subtreeRoot instanceof AnyXmlSchemaNode) {
            visitor.visitAnyXmlNode(m_componentId, parentPath, (AnyXmlSchemaNode) subtreeRoot);
        } else if (subtreeRoot instanceof ChoiceSchemaNode) {
            visitor.visitChoiceNode(m_componentId, parentPath, (ChoiceSchemaNode) subtreeRoot);
        } else if (subtreeRoot instanceof ChoiceCaseNode) {
            visitor.visitChoiceCaseNode(m_componentId, parentPath, (ChoiceCaseNode) subtreeRoot);
        } else if (subtreeRoot instanceof LeafSchemaNode) {
            visitor.visitLeafNode(m_componentId, parentPath, (LeafSchemaNode) subtreeRoot);
        } else if (subtreeRoot instanceof LeafListSchemaNode) {
            visitor.visitLeafListNode(m_componentId, parentPath, (LeafListSchemaNode) subtreeRoot);
        } else {
            LOGGER.error("Unknown subtree root node:{}", subtreeRoot.getPath());
        }
        if (subtreeRoot instanceof DataNodeContainer) {
            traverse(subtreeRootPath, (DataNodeContainer) subtreeRoot, visitor);
        } else if (subtreeRoot instanceof ChoiceSchemaNode) {
            for (ChoiceCaseNode caseNode : ((ChoiceSchemaNode) subtreeRoot).getCases()) {
                //traverse the case contents as if they are children of the choice container
                traverse(parentPath, caseNode, visitor);
            }
        }
        visitor.visitLeave(m_componentId, parentPath, subtreeRootPath);
    }


    private void traverse(SchemaPath parentPath, DataNodeContainer dataNodeContainer, SchemaRegistryVisitor visitor) {
        for (DataSchemaNode childNode : dataNodeContainer.getChildNodes()) {
            visitAndTraverse(parentPath, childNode, visitor);
        }
    }


}
