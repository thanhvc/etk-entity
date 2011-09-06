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
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.etk.entity.base.utils.UtilDateTime;
import org.etk.entity.engine.api.Delegator;

import javolution.context.ObjectFactory;
import javolution.util.FastList;

/**
 * Date-range condition.
 *
 */
@SuppressWarnings("serial")
public class EntityDateFilterCondition extends EntityCondition {

    protected static final ObjectFactory<EntityDateFilterCondition> entityDateFilterConditionFactory = new ObjectFactory<EntityDateFilterCondition>() {
        @Override
        protected EntityDateFilterCondition create() {
            return new EntityDateFilterCondition();
        }
    };

    protected String fromDateName = null;
    protected String thruDateName = null;

    protected EntityDateFilterCondition() {}

    public void init(String fromDateName, String thruDateName) {
        this.fromDateName = fromDateName;
        this.thruDateName = thruDateName;
    }

    public void reset() {
        this.fromDateName = null;
        this.thruDateName = null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String makeWhereString(Entity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        EntityCondition condition = makeCondition();
        return condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo);
    }

    @Override
    public void checkCondition(Entity modelEntity) throws GenericModelException {
        EntityCondition condition = makeCondition();
        condition.checkCondition(modelEntity);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        EntityCondition condition = makeCondition();
        return condition.mapMatches(delegator, map);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityDateFilterCondition)) return false;
        EntityDateFilterCondition other = (EntityDateFilterCondition) obj;
        return equals(fromDateName, other.fromDateName) && equals(thruDateName, other.thruDateName);
    }

    @Override
    public int hashCode() {
        return hashCode(fromDateName) ^ hashCode(thruDateName);
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    @Override
    public EntityCondition freeze() {
        return this;
    }

    @Override
    public void encryptConditionFields(Entity modelEntity, Delegator delegator) {
        // nothing to do here...
    }

    protected EntityCondition makeCondition() {
        return makeCondition(UtilDateTime.nowTimestamp(), fromDateName, thruDateName);
    }

    public static EntityExpr makeCondition(Timestamp moment, String fromDateName, String thruDateName) {
        return EntityCondition.makeCondition(
            EntityCondition.makeCondition(
                EntityCondition.makeCondition(thruDateName, EntityOperator.EQUALS, null),
                EntityOperator.OR,
                EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN, moment)
           ),
            EntityOperator.AND,
            EntityCondition.makeCondition(
                EntityCondition.makeCondition(fromDateName, EntityOperator.EQUALS, null),
                EntityOperator.OR,
                EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN_EQUAL_TO, moment)
           )
      );
    }

    /**
     * Creates an EntityCondition representing a date range filter query to be used against
     * entities that themselves represent a date range.  When used the resulting entities
     * will meet at least one of the following criteria:
     * - fromDate is equal to or after rangeStart but before rangeEnd
     * - thruDate is equal to or after rangeStart but before rangeEnd
     * - fromDate is null and thruDate is equal to or after rangeStart
     * - thruDate is null and fromDate is before rangeEnd
     * - fromDate is null and thruDate is null
     *
     * @param rangeStart    The start of the range to filter against
     * @param rangeEnd      The end of the range to filter against
     * @param fromDateName  The name of the field containing the entity's "fromDate"
     * @param thruDateName  The name of the field containing the entity's "thruDate"
     * @return EntityCondition representing the date range filter
     */
    public static EntityCondition makeRangeCondition(Timestamp rangeStart, Timestamp rangeEnd, String fromDateName, String thruDateName) {
        List<EntityCondition> criteria = FastList.newInstance();
        // fromDate is equal to or after rangeStart but before rangeEnd
        criteria.add(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(fromDateName, EntityOperator.GREATER_THAN_EQUAL_TO, rangeStart),
                        EntityOperator.AND,
                        EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN, rangeEnd)
                )
        );
        // thruDate is equal to or after rangeStart but before rangeEnd
        criteria.add(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN_EQUAL_TO, rangeStart),
                        EntityOperator.AND,
                        EntityCondition.makeCondition(thruDateName, EntityOperator.LESS_THAN, rangeEnd)
                )
        );
        // fromDate is null and thruDate is equal to or after rangeStart
        criteria.add(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(fromDateName, EntityOperator.EQUALS, null),
                        EntityOperator.AND,
                        EntityCondition.makeCondition(thruDateName, EntityOperator.GREATER_THAN_EQUAL_TO, rangeStart)
                )
        );
        // thruDate is null and fromDate is before rangeEnd
        criteria.add(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(thruDateName, EntityOperator.EQUALS, null),
                        EntityOperator.AND,
                        EntityCondition.makeCondition(fromDateName, EntityOperator.LESS_THAN, rangeEnd)
                )
        );
        // fromDate is null and thruDate is null
        criteria.add(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(thruDateName, EntityOperator.EQUALS, null),
                        EntityOperator.AND,
                        EntityCondition.makeCondition(fromDateName, EntityOperator.EQUALS, null)
                )
        );
        // require at least one of the above to be true
        return EntityCondition.makeCondition(criteria, EntityOperator.OR);
    }
}
