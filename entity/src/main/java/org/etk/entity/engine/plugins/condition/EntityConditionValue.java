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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Field;

/**
 * Base class for condition expression values.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionValue extends EntityConditionBase {

    public abstract Field getModelField(Entity modelEntity);

    public void addSqlValue(StringBuilder sql, Entity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix,
            DatasourceInfo datasourceinfo) {
        addSqlValue(sql, emptyAliases, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
    }

    public abstract void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, Entity modelEntity, List<EntityConditionParam> entityConditionParams,
            boolean includeTableNamePrefix, DatasourceInfo datasourceinfo);

    public abstract void validateSql(Entity modelEntity) throws GenericModelException;

    public Object getValue(GenericEntity entity) {
        if (entity == null) {
            return null;
        }
        return getValue(entity.getDelegator(), entity);
    }

    public abstract Object getValue(Delegator delegator, Map<String, ? extends Object> map);

    public abstract EntityConditionValue freeze();

    public abstract void visit(EntityConditionVisitor visitor);

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException("accept not implemented");
    }

    public void toString(StringBuilder sb) {
        addSqlValue(sb, null, new ArrayList<EntityConditionParam>(), false, null);
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        toString(sql);
        return sql.toString();
    }
}
