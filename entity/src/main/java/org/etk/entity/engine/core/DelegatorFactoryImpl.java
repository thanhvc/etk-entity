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
package org.etk.entity.engine.core;

import org.etk.common.logging.Logger;
import org.etk.entity.engine.api.Delegator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
public class DelegatorFactoryImpl extends DelegatorFactory {

  private static Logger logger = Logger.getLogger(DelegatorFactoryImpl.class);
  public static final String module = DelegatorFactoryImpl.class.getName();

  public Delegator getInstance(String delegatorName) {

    try {
      return new GenericDelegator(delegatorName);
    } catch (GenericEntityException e) {
      logger.error("Error creating delegator", e, module);
      return null;
    }
  }
}
