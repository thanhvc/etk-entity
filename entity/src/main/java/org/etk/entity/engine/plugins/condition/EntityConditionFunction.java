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

import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.plugins.model.xml.Entity;

/**
 * Base class for entity condition functions.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionFunction extends EntityCondition {

    public static final int ID_NOT = 1;

    public static class NOT extends EntityConditionFunction {
        public NOT(EntityCondition nested) { super(ID_NOT, "NOT", nested); }
        @Override
        public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
            return !condition.mapMatches(delegator, map);
        }
        @Override
        public EntityCondition freeze() {
            return new NOT(condition.freeze());
        }
        @Override
        public void encryptConditionFields(Entity modelEntity, Delegator delegator) {
            // nothing to do here...
        }
    }

    protected Integer idInt = null;
    protected String codeString = null;
    protected EntityCondition condition = null;

    protected EntityConditionFunction(int id, String code, EntityCondition condition) {
        init(id, code, condition);
    }

    public void init(int id, String code, EntityCondition condition) {
        idInt = id;
        codeString = code;
        this.condition = condition;
    }

    public void reset() {
        idInt = null;
        codeString = null;
        this.condition = null;
    }

    public String getCode() {
        if (codeString == null)
            return "null";
        else
            return codeString;
    }

    public int getId() {
        return idInt;
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityConditionFunction(this, condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionFunction)) return false;
        EntityConditionFunction otherFunc = (EntityConditionFunction) obj;
        return this.idInt == otherFunc.idInt && (this.condition != null ? condition.equals(otherFunc.condition) : otherFunc.condition != null);
    }

    @Override
    public int hashCode() {
        return idInt.hashCode() ^ condition.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String makeWhereString(Entity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(codeString).append('(');
        sb.append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
        sb.append(')');
        return sb.toString();
    }

    @Override
    public void checkCondition(Entity modelEntity) throws GenericModelException {
        condition.checkCondition(modelEntity);
    }
}