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

import java.util.List;
import java.util.Map;

import javolution.context.ObjectFactory;

import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
@SuppressWarnings("serial")
public class EntityWhereString extends EntityCondition {

    protected static final ObjectFactory<EntityWhereString> entityWhereStringFactory = new ObjectFactory<EntityWhereString>() {
        @Override
        protected EntityWhereString create() {
            return new EntityWhereString();
        }
    };

    protected String sqlString;

    protected EntityWhereString() {}

    public void init(String sqlString) {
        this.sqlString = sqlString;
    }

    public void reset() {
        this.sqlString = null;
    }

    @Override
    public boolean isEmpty() {
        return UtilValidate.isEmpty(sqlString);
    }

    @Override
    public String makeWhereString(Entity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        return sqlString;
    }

    @Override
    public void checkCondition(Entity modelEntity) throws GenericModelException {// no nothing, this is always assumed to be fine... could do funky SQL syntax checking, but hey this is a HACK anyway
    }

    @Override
    public boolean entityMatches(Entity entity) {
        throw new UnsupportedOperationException("Cannot do entityMatches on a WhereString, ie no SQL evaluation in EE; Where String is: " + sqlString);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        throw new UnsupportedOperationException("Cannot do mapMatches on a WhereString, ie no SQL evaluation in EE; Where String is: " + sqlString);
    }

    public String getWhereString() {
        return sqlString;
    }

    @Override
    public EntityCondition freeze() {
        return this;
    }

    @Override
    public void encryptConditionFields(Entity modelEntity, Delegator delegator) {
        // nothing to do here...
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityWhereString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityWhereString)) return false;
        EntityWhereString other = (EntityWhereString) obj;
        return equals(sqlString, other.sqlString);
    }

    @Override
    public int hashCode() {
        return hashCode(sqlString);
    }
}