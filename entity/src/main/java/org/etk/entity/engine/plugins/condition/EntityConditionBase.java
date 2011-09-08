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
package org.etk.entity.engine.plugins.condition;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.jdbc.SqlJdbcUtil;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Field;
import org.etk.entity.engine.plugins.model.xml.ViewEntity;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * Represents the conditions to be used to constrain a query
 * <br/>An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionBase implements Serializable {

    public static final List<?> emptyList = Collections.unmodifiableList(FastList.newInstance());
    public static final Map<?,?> _emptyMap = Collections.unmodifiableMap(FastMap.newInstance());
    public static final Map<String, String> emptyAliases = Collections.unmodifiableMap(FastMap.<String, String>newInstance());

    protected Field getField(Entity modelEntity, String fieldName) {
        Field modelField = null;
        if (modelEntity != null) {
            modelField = modelEntity.getField(fieldName);
        }
        return modelField;
    }

    protected String getColName(Map<String, String> tableAliases, Entity modelEntity, String fieldName, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        if (modelEntity == null) return fieldName;
        return getColName(tableAliases, modelEntity, getField(modelEntity, fieldName), fieldName, includeTableNamePrefix, datasourceInfo);
    }

    protected String getColName(Field modelField, String fieldName) {
        String colName = null;
        if (modelField != null) {
            colName = modelField.getColValue();
        } else {
            colName = fieldName;
        }
        return colName;
    }

    protected String getColName(Map<String, String> tableAliases, Entity modelEntity, Field modelField, String fieldName, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        if (modelEntity == null || modelField == null) return fieldName;

        // if this is a view entity and we are configured to alias the views, use the alias here instead of the composite (ie table.column) field name
        if (datasourceInfo != null && datasourceInfo.aliasViews && modelEntity instanceof ViewEntity) {
            ViewEntity modelViewEntity = (ViewEntity) modelEntity;
            ModelAlias modelAlias = modelViewEntity.getAlias(fieldName);
            if (modelAlias != null) {
                return modelAlias.getColAlias();
            }
        }

        String colName = getColName(modelField, fieldName);
        if (includeTableNamePrefix && datasourceInfo != null) {
            String tableName = modelEntity.getTableName(datasourceInfo);
            if (tableAliases.containsKey(tableName)) {
                tableName = tableAliases.get(tableName);
            }
            colName = tableName + "." + colName;
        }
        return colName;
    }

    protected void addValue(StringBuilder buffer, Field field, Object value, List<EntityConditionParam> params) {
        SqlJdbcUtil.addValue(buffer, params == null ? null : field, value, params);
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("equals:" + getClass().getName());
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("hashCode: " + getClass().getName());
    }

    protected static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    protected static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    public static Boolean castBoolean(boolean result) {
        return result ? Boolean.TRUE : Boolean.FALSE;
    }
}
