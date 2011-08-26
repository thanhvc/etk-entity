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
package org.etk.entity.engine.plugins.entity.metadata;

import java.io.Serializable;

/**
 * Author : ThanhVuCong
 *          thanhvucong.78@gmail.com
 * Aug 26, 2011  
 */
public abstract class EntityChildMeta implements Serializable {

  protected EntityMeta parentEntityMeta;
  protected String desciption = "";
  
  protected EntityChildMeta() { }
  protected EntityChildMeta(EntityMeta parentEntityMeta) {
    this.parentEntityMeta = parentEntityMeta;
  }
  public EntityMeta getParentEntityMeta() {
    return parentEntityMeta;
  }
  public void setParentEntityMeta(EntityMeta parentEntityMeta) {
    this.parentEntityMeta = parentEntityMeta;
  }
  /**
   * The description for documentation purposes.
   * @return
   */
  public String getDesciption() {
    return desciption;
  }
  public void setDesciption(String desciption) {
    this.desciption = desciption;
  }
  
  
}
