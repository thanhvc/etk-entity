/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.etk.entity.engine.plugins.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.etk.common.logging.Logger;
import org.etk.entity.engine.core.GenericEntityException;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public class EntityConfigUtil {

    private static final Logger logger = Logger.getLogger(EntityConfigUtil.class);
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";

    private static volatile AtomicReference<EntityConfigUtil> configRef = new AtomicReference<EntityConfigUtil>();

    // ========== engine info fields ==========
    private final String txFactoryClass;
    private final String txFactoryUserTxJndiName;
    private final String txFactoryUserTxJndiServerName;
    private final String txFactoryTxMgrJndiName;
    private final String txFactoryTxMgrJndiServerName;
    private final String connFactoryClass;

    private final Map<String, ResourceLoaderInfo> resourceLoaderInfos = new HashMap<String, ResourceLoaderInfo>();
    private final Map<String, DelegatorInfo> delegatorInfos = new HashMap<String, DelegatorInfo>();
    private final Map<String, EntityModelReaderInfo> entityModelReaderInfos = new HashMap<String, EntityModelReaderInfo>();
    private final Map<String, EntityGroupReaderInfo> entityGroupReaderInfos = new HashMap<String, EntityGroupReaderInfo>();
    private final Map<String, EntityEcaReaderInfo> entityEcaReaderInfos = new HashMap<String, EntityEcaReaderInfo>();
    private final Map<String, EntityDataReaderInfo> entityDataReaderInfos = new HashMap<String, EntityDataReaderInfo>();
    private final Map<String, FieldTypeInfo> fieldTypeInfos = new HashMap<String, FieldTypeInfo>();
    private final Map<String, DatasourceInfo> datasourceInfos = new HashMap<String, DatasourceInfo>();

    private static Element getXmlRootElement() throws GenericEntityConfException {
        try {
            return ResourceLoader.getXmlRootElement(ENTITY_ENGINE_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericEntityConfException("Could not get entity engine XML root element", e);
        }
    }

    static {
        try {
            initialize(getXmlRootElement());
        } catch (Exception e) {
            logger.error(e, "Error loading entity config XML file " + ENTITY_ENGINE_XML_FILENAME);
        }
    }

    public static void reinitialize() throws GenericEntityException {
        try {
            ResourceLoader.invalidateDocument(ENTITY_ENGINE_XML_FILENAME);
            initialize(getXmlRootElement());
        } catch (Exception e) {
            throw new GenericEntityException("Error reloading entity config XML file " + ENTITY_ENGINE_XML_FILENAME, e);
        }
    }

    public static void initialize(Element rootElement) throws GenericEntityException {
        configRef.set(new EntityConfigUtil(rootElement));
    }

    private EntityConfigUtil(Element rootElement) throws GenericEntityException {
        // load the transaction factory
        Element transactionFactoryElement = UtilXml.firstChildElement(rootElement, "transaction-factory");
        if (transactionFactoryElement == null) {
            throw new GenericEntityConfException("ERROR: no transaction-factory definition was found in " + ENTITY_ENGINE_XML_FILENAME);
        }

        txFactoryClass = transactionFactoryElement.getAttribute("class");

        Element userTxJndiElement = UtilXml.firstChildElement(transactionFactoryElement, "user-transaction-jndi");
        if (userTxJndiElement != null) {
            txFactoryUserTxJndiName = userTxJndiElement.getAttribute("jndi-name");
            txFactoryUserTxJndiServerName = userTxJndiElement.getAttribute("jndi-server-name");
        } else {
            txFactoryUserTxJndiName = null;
            txFactoryUserTxJndiServerName = null;
        }

        Element txMgrJndiElement = UtilXml.firstChildElement(transactionFactoryElement, "transaction-manager-jndi");
        if (txMgrJndiElement != null) {
            txFactoryTxMgrJndiName = txMgrJndiElement.getAttribute("jndi-name");
            txFactoryTxMgrJndiServerName = txMgrJndiElement.getAttribute("jndi-server-name");
        } else {
            txFactoryTxMgrJndiName = null;
            txFactoryTxMgrJndiServerName = null;
        }

        // load the connection factory
        Element connectionFactoryElement = UtilXml.firstChildElement(rootElement, "connection-factory");
        if (connectionFactoryElement == null) {
            throw new GenericEntityConfException("ERROR: no connection-factory definition was found in " + ENTITY_ENGINE_XML_FILENAME);
        }

        connFactoryClass = connectionFactoryElement.getAttribute("class");

        // not load all of the maps...

        // resource-loader - resourceLoaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "resource-loader")) {
            ResourceLoaderInfo resourceLoaderInfo = new ResourceLoaderInfo(curElement);
            resourceLoaderInfos.put(resourceLoaderInfo.name, resourceLoaderInfo);
        }

        // delegator - delegatorInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "delegator")) {
            DelegatorInfo delegatorInfo = new DelegatorInfo(curElement);
            delegatorInfos.put(delegatorInfo.name, delegatorInfo);
        }

        // entity-model-reader - entityModelReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-model-reader")) {
            EntityModelReaderInfo entityModelReaderInfo = new EntityModelReaderInfo(curElement);
            entityModelReaderInfos.put(entityModelReaderInfo.name, entityModelReaderInfo);
        }

        // entity-group-reader - entityGroupReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-group-reader")) {
            EntityGroupReaderInfo entityGroupReaderInfo = new EntityGroupReaderInfo(curElement);
            entityGroupReaderInfos.put(entityGroupReaderInfo.name, entityGroupReaderInfo);
        }

        // entity-eca-reader - entityEcaReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-eca-reader")) {
            EntityEcaReaderInfo entityEcaReaderInfo = new EntityEcaReaderInfo(curElement);
            entityEcaReaderInfos.put(entityEcaReaderInfo.name, entityEcaReaderInfo);
        }

        // entity-data-reader - entityDataReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-data-reader")) {
            EntityDataReaderInfo entityDataReaderInfo = new EntityDataReaderInfo(curElement);
            entityDataReaderInfos.put(entityDataReaderInfo.name, entityDataReaderInfo);
        }

        // field-type - fieldTypeInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "field-type")) {
            FieldTypeInfo fieldTypeInfo = new FieldTypeInfo(curElement);
            fieldTypeInfos.put(fieldTypeInfo.name, fieldTypeInfo);
        }

        // datasource - datasourceInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "datasource")) {
            DatasourceInfo datasourceInfo = new DatasourceInfo(curElement);
            datasourceInfos.put(datasourceInfo.name, datasourceInfo);
        }
    }

    public static String getTxFactoryClass() {
        return configRef.get().txFactoryClass;
    }

    public static String getTxFactoryUserTxJndiName() {
        return configRef.get().txFactoryUserTxJndiName;
    }

    public static String getTxFactoryUserTxJndiServerName() {
        return configRef.get().txFactoryUserTxJndiServerName;
    }

    public static String getTxFactoryTxMgrJndiName() {
        return configRef.get().txFactoryTxMgrJndiName;
    }

    public static String getTxFactoryTxMgrJndiServerName() {
        return configRef.get().txFactoryTxMgrJndiServerName;
    }

    public static String getConnectionFactoryClass() {
        return configRef.get().connFactoryClass;
    }

    public static ResourceLoaderInfo getResourceLoaderInfo(String name) {
        return configRef.get().resourceLoaderInfos.get(name);
    }

    public static DelegatorInfo getDelegatorInfo(String name) {
        return configRef.get().delegatorInfos.get(name);
    }

    public static EntityModelReaderInfo getEntityModelReaderInfo(String name) {
        return configRef.get().entityModelReaderInfos.get(name);
    }

    public static EntityGroupReaderInfo getEntityGroupReaderInfo(String name) {
        return configRef.get().entityGroupReaderInfos.get(name);
    }

    public static EntityEcaReaderInfo getEntityEcaReaderInfo(String name) {
        return configRef.get().entityEcaReaderInfos.get(name);
    }

    public static EntityDataReaderInfo getEntityDataReaderInfo(String name) {
        return configRef.get().entityDataReaderInfos.get(name);
    }

    public static FieldTypeInfo getFieldTypeInfo(String name) {
        return configRef.get().fieldTypeInfos.get(name);
    }

    public static DatasourceInfo getDatasourceInfo(String name) {
        return configRef.get().datasourceInfos.get(name);
    }

    public static Map<String, DatasourceInfo> getDatasourceInfos() {
        return configRef.get().datasourceInfos;
    }
}
