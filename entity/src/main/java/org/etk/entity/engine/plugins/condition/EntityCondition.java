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

import javolution.lang.Reusable;
import javolution.util.FastList;

import org.etk.entity.base.lang.IsEmpty;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;

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
public abstract class EntityCondition extends EntityConditionBase implements IsEmpty, Reusable {

    public static <L,R,LL,RR> EntityExpr makeCondition(L lhs, EntityComparisonOperator<LL,RR> operator, R rhs) {
        EntityExpr expr = EntityExpr.entityExprFactory.object();
        expr.init(lhs, operator, rhs);
        return expr;
    }

    public static <R> EntityExpr makeCondition(String fieldName, R value) {
        EntityExpr expr = EntityExpr.entityExprFactory.object();
        expr.init(fieldName, EntityOperator.EQUALS, value);
        return expr;
    }

    public static EntityExpr makeCondition(EntityCondition lhs, EntityJoinOperator operator, EntityCondition rhs) {
        EntityExpr expr = EntityExpr.entityExprFactory.object();
        expr.init(lhs, operator, rhs);
        return expr;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(EntityJoinOperator operator, T... conditionList) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(operator, conditionList);
        return ecl;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(T... conditionList) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(EntityOperator.AND, conditionList);
        return ecl;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<T> conditionList, EntityJoinOperator operator) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(conditionList, operator);
        return ecl;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<T> conditionList) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(conditionList, EntityOperator.AND);
        return ecl;
    }

    public static <L,R> EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(fieldMap, compOp, joinOp);
        return efm;
    }

    public static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityJoinOperator joinOp) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(fieldMap, EntityOperator.EQUALS, joinOp);
        return efm;
    }

    public static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(fieldMap, EntityOperator.EQUALS, EntityOperator.AND);
        return efm;
    }

    public static <L,R> EntityFieldMap makeCondition(EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp, Object... keysValues) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(compOp, joinOp, keysValues);
        return efm;
    }

    public static EntityFieldMap makeCondition(EntityJoinOperator joinOp, Object... keysValues) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(EntityOperator.EQUALS, joinOp, keysValues);
        return efm;
    }

    public static EntityFieldMap makeConditionMap(Object... keysValues) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(EntityOperator.EQUALS, EntityOperator.AND, keysValues);
        return efm;
    }

    public static EntityDateFilterCondition makeConditionDate(String fromDateName, String thruDateName) {
        EntityDateFilterCondition edfc = EntityDateFilterCondition.entityDateFilterConditionFactory.object();
        edfc.init(fromDateName, thruDateName);
        return edfc;
    }

    public static EntityWhereString makeConditionWhere(String sqlString) {
        EntityWhereString ews = EntityWhereString.entityWhereStringFactory.object();
        ews.init(sqlString);
        return ews;
    }

    @Override
    public String toString() {
        return makeWhereString(null, FastList.<EntityConditionParam>newInstance(), null);
    }

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".accept not implemented");
    }

    abstract public String makeWhereString(Entity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo);

    abstract public void checkCondition(Entity modelEntity) throws GenericModelException;

    public boolean entityMatches(GenericEntity entity) {
        return mapMatches(entity.getDelegator(), entity);
    }

    public Boolean eval(GenericEntity entity) {
        return eval(entity.getDelegator(), entity);
    }

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map) {
        return mapMatches(delegator, map) ? Boolean.TRUE : Boolean.FALSE;
    }

    abstract public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map);

    abstract public EntityCondition freeze();

    abstract public void encryptConditionFields(Entity modelEntity, Delegator delegator);

    public void visit(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".visit not implemented");
    }
}
