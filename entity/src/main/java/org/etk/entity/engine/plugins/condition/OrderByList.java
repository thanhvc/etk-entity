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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;

public class OrderByList implements Comparator<GenericEntity> {
  protected List<OrderByItem> orderByList = new ArrayList<OrderByItem>();

  public OrderByList() {
  }

  public OrderByList(String... orderByList) {
      addOrderBy(orderByList);
  }

  public OrderByList(Collection<String> orderByList) {
      addOrderBy(orderByList);
  }

  public void addOrderBy(String... orderByList) {
      for (String orderByItem: orderByList) {
          addOrderBy(orderByItem);
      }
  }

  public void addOrderBy(Collection<String> orderByList) {
      for (String orderByItem: orderByList) {
          addOrderBy(orderByItem);
      }
  }

  public void addOrderBy(String text) {
      addOrderBy(OrderByItem.parse(text));
  }

  public void addOrderBy(EntityConditionValue value) {
      addOrderBy(value, false);
  }

  public void addOrderBy(EntityConditionValue value, boolean descending) {
      addOrderBy(new OrderByItem(value, descending));
  }

  public void addOrderBy(OrderByItem orderByItem) {
      orderByList.add(orderByItem);
  }

  public void checkOrderBy(Entity modelEntity) throws GenericModelException {
      for (OrderByItem orderByItem: orderByList) {
          orderByItem.checkOrderBy(modelEntity);
      }
  }

  public String makeOrderByString(Entity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
      StringBuilder sb = new StringBuilder();
      makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
      return sb.toString();
  }

  public void makeOrderByString(StringBuilder sb, Entity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
      if (!orderByList.isEmpty()) {
          sb.append(" ORDER BY ");
      }
      for (int i = 0; i < orderByList.size(); i++) {
          if (i != 0) sb.append(", ");
          OrderByItem orderByItem = orderByList.get(i);
          orderByItem.makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
      }
  }

  public int compare(GenericEntity entity1, GenericEntity entity2) {
      int result = 0;
      for (OrderByItem orderByItem: orderByList) {
          result = orderByItem.compare(entity1, entity2);
          if (result != 0) break;
      }
      return result;
  }

  @Override
  public boolean equals(java.lang.Object obj) {
      if (!(obj instanceof OrderByList)) return false;
      OrderByList that = (OrderByList) obj;
      return orderByList.equals(that.orderByList);
  }

  @Override
  public String toString() {
      return makeOrderByString(null, false, null);
  }
}