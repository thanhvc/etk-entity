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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionListBase<T extends EntityCondition> extends EntityCondition {
    public static final String module = EntityConditionListBase.class.getName();

    protected List<T> conditionList = null;
    protected EntityJoinOperator operator = null;

    protected EntityConditionListBase() {}

    public EntityConditionListBase(EntityJoinOperator operator, T... conditionList) {
        this.init(operator, conditionList);
    }

    public EntityConditionListBase(List<T> conditionList, EntityJoinOperator operator) {
        this.init(conditionList, operator);
    }

    public void init(EntityJoinOperator operator, T... conditionList) {
        this.conditionList = Arrays.asList(conditionList);
        this.operator = operator;
    }

    public void init(List<T> conditionList, EntityJoinOperator operator) {
        this.conditionList = conditionList;
        this.operator = operator;
    }

    public void reset() {
        this.conditionList = null;
        this.operator = null;
    }

    public EntityJoinOperator getOperator() {
        return this.operator;
    }

    public T getCondition(int index) {
        return this.conditionList.get(index);
    }

    protected int getConditionListSize() {
        return this.conditionList.size();
    }

    protected Iterator<T> getConditionIterator() {
        return this.conditionList.iterator();
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityJoinOperator(operator, conditionList);
    }

    @Override
    public boolean isEmpty() {
        return operator.isEmpty(conditionList);
    }

    @Override
    public String makeWhereString(Entity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName(), module);
        StringBuilder sql = new StringBuilder();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, conditionList, datasourceInfo);
        return sql.toString();
    }

    @Override
    public void checkCondition(Entity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName(), module);
        operator.validateSql(modelEntity, conditionList);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        return operator.mapMatches(delegator, map, conditionList);
    }

    @Override
    public EntityCondition freeze() {
        return operator.freeze(conditionList);
    }

    @Override
    public void encryptConditionFields(Entity modelEntity, Delegator delegator) {
        for (T cond: this.conditionList) {
            cond.encryptConditionFields(modelEntity, delegator);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionListBase<?>)) return false;
        EntityConditionListBase<?> other = UtilGenerics.cast(obj);

        boolean isEqual = conditionList.equals(other.conditionList) && operator.equals(other.operator);
        //if (!isEqual) {
        //    Debug.logWarning("EntityConditionListBase.equals is false:\n this.operator=" + this.operator + "; other.operator=" + other.operator +
        //            "\nthis.conditionList=" + this.conditionList +
        //            "\nother.conditionList=" + other.conditionList, module);
        //}
        return isEqual;
    }

    @Override
    public int hashCode() {
        return conditionList.hashCode() + operator.hashCode();
    }
}
