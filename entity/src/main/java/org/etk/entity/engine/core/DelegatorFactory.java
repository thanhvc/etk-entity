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

import java.util.concurrent.ConcurrentHashMap;

import org.etk.common.logging.Logger;
import org.etk.entity.base.lang.Factory;
import org.etk.entity.base.utils.UtilObject;
import org.etk.entity.engine.api.Delegator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 26, 2011  
 */
public abstract class DelegatorFactory implements Factory<Delegator, String> {
  public static final String module = DelegatorFactoryImpl.class.getName();
  private static final ConcurrentHashMap<String, Delegator> delegatorCache = new ConcurrentHashMap<String, Delegator>();

  private static Logger logger = Logger.getLogger(DelegatorFactory.class);
  public static Delegator getDelegator(String delegatorName) {
      if (delegatorName == null) {
          delegatorName = "default";
          logger.warn("Got a getGenericDelegator call with a null delegatorName, assuming default for the name.", new Exception( "Location where getting delegator with null name"));
      }
      do {
          Delegator delegator = delegatorCache.get(delegatorName);

          if (delegator != null) {
              // setup the Entity ECA Handler
              delegator.initEntityEcaHandler();
              //Debug.logInfo("got delegator(" + delegatorName + ") from cache", module);

              // setup the distributed CacheClear
              delegator.initDistributedCacheClear();

              return delegator;
          }
          try {
              delegator = UtilObject.getObjectFromFactory(DelegatorFactory.class, delegatorName);
          } catch (ClassNotFoundException e) {
            logger.error(module);
          }
          logger.info("putting delegator(" + delegatorName + ") into cache", module);
          delegatorCache.putIfAbsent(delegatorName, delegator);
      } while (true);
  }
}
