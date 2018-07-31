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

package org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.opendaylight.yangtools.yang.common.QName;

import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.ModelNode;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.constraints.validation.util.DataStoreValidationUtil;
import org.broadband_forum.obbaa.netconf.mn.fwk.server.model.support.emn.DsmNotRegisteredException;
import org.broadband_forum.obbaa.netconf.stack.logging.AdvancedLogger;
import org.broadband_forum.obbaa.netconf.stack.logging.LoggerFactory;

public class ModelNodeDynaBean extends BasicDynaBean {

    private static final long serialVersionUID = 1L;

    private Set<String> m_refreshList;

    private Set<String> m_attributeList;

    private Boolean m_isLogDebugEnabled;

    private final static AdvancedLogger LOGGER = LoggerFactory.getLogger(ModelNodeDynaBean.class,
            DataStoreValidationUtil.NC_DS_VALIDATION, "DEBUG", "GLOBAL");

    private void logDebug(String message, Object... objects) {
        if (m_isLogDebugEnabled == null) {
            m_isLogDebugEnabled = LOGGER.isDebugEnabled();
        }

        if (m_isLogDebugEnabled) {
            LOGGER.debug(message, objects);
        }
    }

    public ModelNodeDynaBean(DynaClass dynaClass) {
        super(dynaClass);
    }

    @Override
    public boolean contains(String name, String key) {
        doLazyLoad(name);
        return super.contains(name, key);
    }

    public boolean contains(String name) {
        if (PropertyUtils.isReadable(this, name)) {
            return true;
        }

        return ModelNodeDynaClass.containsProperty(this, name);
    }

    @Override
    public Object get(String name) {
        logDebug("fetch for name {} in bean {}", name, this);
        doLazyLoad(name);
        return super.get(name);
    }

    @Override
    public Object get(String name, int index) {
        logDebug("fetch for name {} index {} in bean {}", name, index, this);
        doLazyLoad(name);
        return super.get(name, index);
    }

    @Override
    public Object get(String name, String key) {
        logDebug("fetch for name {} key {} in bean {}", name, key, this);
        doLazyLoad(name);
        return super.get(name, key);
    }

    @SuppressWarnings("unchecked")
    private void doLazyLoad(String name) {
        Set<String> refreshList = getRefreshList();
        if (refreshList.contains(name)) {
            return;
        }
        logDebug("dolazyload for name {} in bean {}", name, this);
        if (values.get(name) == null && !refreshList.contains(name)) {
            if (ModelNodeWithAttributes.PARENT.equals(name)) {
                loadParent();
            } else if (((Collection<String>) values.get(ModelNodeWithAttributes.CHILD_LISTS)).contains(name)) {
                loadChildList(name);
            } else if (((Collection<String>) values.get(ModelNodeWithAttributes.CHILD_CONTAINERS)).contains(name)) {
                loadChildContainer(name);
            }
        }
    }

    private void loadParent() {
        ModelNodeWithAttributes modelNode = (ModelNodeWithAttributes) values.get(ModelNodeWithAttributes.MODEL_NODE);
        Boolean addModelNode = (Boolean) values.get(ModelNodeWithAttributes.ADD_MODEL_NODE);
        ModelNode parentNode = modelNode.getParent();
        if (parentNode != null) {
            if (addModelNode) {
                set(ModelNodeWithAttributes.PARENT, parentNode);
            } else {
                Object dyna = ModelNodeDynaBeanFactory.getDynaBean(parentNode);
                set(ModelNodeWithAttributes.PARENT, dyna);
                logDebug("loading parent {} for {}", dyna, this);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadChildList(String inName) {
        try {
            String name = ModelNodeDynaBeanFactory.getModelNodeAttributeName(inName);
            ModelNodeWithAttributes modelNode = (ModelNodeWithAttributes) values.get(ModelNodeWithAttributes.MODEL_NODE);
            Boolean addModelNode = (Boolean) values.get(ModelNodeWithAttributes.ADD_MODEL_NODE);
            Map<QName, ChildListHelper> childLists = modelNode.getModelNodeHelperRegistry().getChildListHelpers
                    (modelNode.getModelNodeSchemaPath());
            for (Entry<QName, ChildListHelper> childList : childLists.entrySet()) {
                // TODO: FNMS-10121 we should use the QNames as key !!
                if (childList.getKey().getLocalName().equals(name)) {
                    ChildListHelper helper = childList.getValue();
                    Collection<ModelNode> listNodes = helper.getValue(modelNode, Collections.<QName,
                            ConfigLeafAttribute>emptyMap());
                    List childEntries = null;
                    for (ModelNode list : listNodes) {
                        if (addModelNode) {
                            if (childEntries == null) {
                                childEntries = new ArrayList<ModelNode>();
                            }
                            childEntries.add(list);
                        } else {
                            if (childEntries == null) {
                                childEntries = new ArrayList<Object>();
                            }
                            Object dyna = ModelNodeDynaBeanFactory.getDynaBean(list, modelNode);
                            logDebug("loading child list {} dyna for {}", dyna, this);
                            childEntries.add(dyna);
                        }
                    }
                    if (childEntries != null) {
                        set(inName, childEntries);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadChildContainer(String inName) {
        try {
            String name = ModelNodeDynaBeanFactory.getModelNodeAttributeName(inName);
            ModelNodeWithAttributes modelNode = (ModelNodeWithAttributes) values.get(ModelNodeWithAttributes.MODEL_NODE);
            Boolean addModelNode = (Boolean) values.get(ModelNodeWithAttributes.ADD_MODEL_NODE);
            Map<QName, ChildContainerHelper> childContainers = modelNode.getModelNodeHelperRegistry()
                    .getChildContainerHelpers(modelNode.getModelNodeSchemaPath());
            for (Entry<QName, ChildContainerHelper> childContainer : childContainers.entrySet()) {
                // TODO: FNMS-10121 we should use the QNames as key !!
                if (childContainer.getKey().getLocalName().equals(name)) {
                    ChildContainerHelper containerHelper = childContainer.getValue();
                    ModelNode containerNode = null;
                    try {
                        containerNode = containerHelper.getValue(modelNode);
                    } catch (Exception e) {
                        if (!(e instanceof DsmNotRegisteredException)) {
                            throw e;
                        } else {
                            //We have picked up a wrong node. since we are comparing only name here.
                            //NS addition is a seperate task to be done to enhance support in ModelNodeDynaBean.
                            // lets continue to scan others, if they also want to throw an exception :)
                            // FNMS-6679
                            continue;
                        }
                    }
                    if (containerNode != null) {
                        if (addModelNode) {
                            set(inName, containerNode);
                        } else {
                            Object dyna = ModelNodeDynaBeanFactory.getDynaBean(containerNode);
                            logDebug("loading child container {} dyna for {}", dyna, this);
                            set(inName, dyna);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void refreshLeafLists(Set<Entry<QName, LinkedHashSet<ConfigLeafAttribute>>> leafListEntry) {
        for (Entry<QName, LinkedHashSet<ConfigLeafAttribute>> item : leafListEntry) {
            String name = ModelNodeDynaBeanFactory.getDynaBeanAttributeName(item.getKey().getLocalName());
            set(name, item.getValue());
        }
    }


    public void refreshAttributes(Set<Entry<QName, ConfigLeafAttribute>> entry) {
        for (Entry<QName, ConfigLeafAttribute> item : entry) {
            String name = ModelNodeDynaBeanFactory.getDynaBeanAttributeName(item.getKey().getLocalName());
            set(name, item.getValue().getStringValue());
        }
    }

    /**
     * added for debuggin
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public String toString() {
        DynaProperty[] properties = this.dynaClass.getDynaProperties();
        StringBuilder builder = new StringBuilder();
        Collection childNames = new HashSet<>((Collection) super.get(ModelNodeWithAttributes.CHILD_LISTS));
        childNames.addAll((Collection) super.get(ModelNodeWithAttributes.CHILD_CONTAINERS));
        builder.append(this.dynaClass.getName()).append("[");
        for (DynaProperty property : properties) {
            if (property.getName().equals(ModelNodeWithAttributes.PARENT)) {
                builder.append(property.getName()).append(":").append(" ").append('\n');
            } else if (!childNames.contains(property.getName())) {
                builder.append(property.getName()).append(":").append(super.get(property.getName())).append(" ")
                        .append('\n');
            }
        }
        builder.append("]");

        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRefreshList() {
        if (m_refreshList == null) {
            m_refreshList = (Set<String>) super.get(ModelNodeDynaBeanFactory.CHILD_REFRESH_LIST);
        }
        return m_refreshList;
    }

    @SuppressWarnings("unchecked")
    public boolean isReadable(String name) {
        if (m_attributeList == null) {
            m_attributeList = (Set<String>) super.get(ModelNodeDynaBeanFactory.ATTRIBUTE_LIST);
        }
        return m_attributeList.contains(name);
    }

}
