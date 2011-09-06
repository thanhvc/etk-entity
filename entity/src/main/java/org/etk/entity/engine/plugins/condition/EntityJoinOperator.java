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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;

/**
 * Join operator (AND/OR).
 *
 */
@SuppressWarnings("serial")
public class EntityJoinOperator extends EntityOperator<EntityCondition, EntityCondition, Boolean> {

    protected boolean shortCircuitValue;

    protected EntityJoinOperator(int id, String code, boolean shortCircuitValue) {
        super(id, code);
        this.shortCircuitValue = shortCircuitValue;
    }

    @Override
    public void addSqlValue(StringBuilder sql, Entity modelEntity, List<EntityConditionParam> entityConditionParams, boolean compat, EntityCondition lhs, EntityCondition rhs, DatasourceInfo datasourceInfo) {
        List<EntityCondition> conditions = FastList.newInstance();
        conditions.add(lhs);
        conditions.add(rhs);
        addSqlValue(sql, modelEntity, entityConditionParams, conditions, datasourceInfo);
    }

    public void addSqlValue(StringBuilder sql, Entity modelEntity, List<EntityConditionParam> entityConditionParams, List<? extends EntityCondition> conditionList, DatasourceInfo datasourceInfo) {
        if (UtilValidate.isNotEmpty(conditionList)) {
            boolean hadSomething = false;
            Iterator<? extends EntityCondition> conditionIter = conditionList.iterator();
            while (conditionIter.hasNext()) {
                EntityCondition condition = conditionIter.next();
                if (condition.isEmpty()) {
                    continue;
                }
                if (hadSomething) {
                    sql.append(' ');
                    sql.append(getCode());
                    sql.append(' ');
                } else {
                    hadSomething = true;
                    sql.append('(');
                }
                sql.append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
            }
            if (hadSomething) {
                sql.append(')');
            }
        }
    }

    protected EntityCondition freeze(Object item) {
        return ((EntityCondition) item).freeze();
    }

    @Override
    public EntityCondition freeze(EntityCondition lhs, EntityCondition rhs) {
        return EntityCondition.makeCondition(freeze(lhs), this, freeze(rhs));
    }

    public EntityCondition freeze(List<? extends EntityCondition> conditionList) {
        List<EntityCondition> newList = new ArrayList<EntityCondition>(conditionList.size());
        for (EntityCondition condition: conditionList) {
            newList.add(condition.freeze());
        }
        return EntityCondition.makeCondition(newList, this);
    }

    public void visit(EntityConditionVisitor visitor, List<? extends EntityCondition> conditionList) {
        if (UtilValidate.isNotEmpty(conditionList)) {
            for (EntityCondition condition: conditionList) {
                visitor.visit(condition);
            }
        }
    }

    @Override
    public void visit(EntityConditionVisitor visitor, EntityCondition lhs, EntityCondition rhs) {
        lhs.visit(visitor);
        visitor.visit(rhs);
    }

    public Boolean eval(GenericEntity entity, EntityCondition lhs, EntityCondition rhs) {
        return entityMatches(entity, lhs, rhs) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isEmpty(EntityCondition lhs, EntityCondition rhs) {
        return lhs.isEmpty() && rhs.isEmpty();
    }

    public boolean isEmpty(List<? extends EntityCondition> conditionList) {
        for (EntityCondition condition: conditionList) {
            if (!condition.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean entityMatches(GenericEntity entity, EntityCondition lhs, EntityCondition rhs) {
        if (lhs.entityMatches(entity) == shortCircuitValue) return shortCircuitValue;
        if (rhs.entityMatches(entity) == shortCircuitValue) return shortCircuitValue;
        return !shortCircuitValue;
    }

    public boolean entityMatches(GenericEntity entity, List<? extends EntityCondition> conditionList) {
        return mapMatches(entity.getDelegator(), entity, conditionList);
    }

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map, EntityCondition lhs, EntityCondition rhs) {
        return castBoolean(mapMatches(delegator, map, lhs, rhs));
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, EntityCondition lhs, EntityCondition rhs) {
        if (lhs.mapMatches(delegator, map) == shortCircuitValue) return shortCircuitValue;
        if (rhs.mapMatches(delegator, map) == shortCircuitValue) return shortCircuitValue;
        return !shortCircuitValue;
    }

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map, List<? extends EntityCondition> conditionList) {
        return castBoolean(mapMatches(delegator, map, conditionList));
    }

    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, List<? extends EntityCondition> conditionList) {
        if (UtilValidate.isNotEmpty(conditionList)) {
            for (EntityCondition condition: conditionList) {
                if (condition.mapMatches(delegator, map) == shortCircuitValue) return shortCircuitValue;
            }
        }
        return !shortCircuitValue;
    }

    @Override
    public void validateSql(Entity modelEntity, EntityCondition lhs, EntityCondition rhs) throws GenericModelException {
        lhs.checkCondition(modelEntity);
        rhs.checkCondition(modelEntity);
    }

    public void validateSql(Entity modelEntity, List<? extends EntityCondition> conditionList) throws GenericModelException {
        if (conditionList == null) {
            throw new GenericModelException("Condition list is null");
        }
        for (EntityCondition condition: conditionList) {
            condition.checkCondition(modelEntity);
        }
    }
}