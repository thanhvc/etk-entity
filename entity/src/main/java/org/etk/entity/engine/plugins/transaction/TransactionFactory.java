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
package org.etk.entity.engine.plugins.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.etk.common.logging.Logger;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.config.EntityConfigUtil;
import org.etk.entity.engine.plugins.datasource.GenericHelperInfo;
import org.etk.entity.engine.plugins.jdbc.CursorConnection;

/**
 * TransactionFactory - central source for JTA objects
 */
public class TransactionFactory {

  private static final Logger logger = Logger.getLogger(TransactionFactory.class);

  public static TransactionFactoryInterface transactionFactory = null;

  public static TransactionFactoryInterface getTransactionFactory() {
    if (transactionFactory == null) { // don't want to block here
      synchronized (TransactionFactory.class) {
        // must check if null again as one of the blocked threads can still
        // enter
        if (transactionFactory == null) {
          try {
            String className = EntityConfigUtil.getTxFactoryClass();

            if (className == null) {
              throw new IllegalStateException("Could not find transaction factory class name definition");
            }
            Class<?> tfClass = null;

            if (UtilValidate.isNotEmpty(className)) {
              try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                tfClass = loader.loadClass(className);
              } catch (ClassNotFoundException e) {
                logger.warn(e.getMessage(), e);
                throw new IllegalStateException("Error loading TransactionFactory class \""
                    + className + "\": " + e.getMessage());
              }
            }

            try {
              transactionFactory = (TransactionFactoryInterface) tfClass.newInstance();
            } catch (IllegalAccessException e) {
              logger.warn(e.getMessage(), e);
              throw new IllegalStateException("Error loading TransactionFactory class \""
                  + className + "\": " + e.getMessage());
            } catch (InstantiationException e) {
              logger.error(e.getMessage(), e);
              throw new IllegalStateException("Error loading TransactionFactory class \""
                  + className + "\": " + e.getMessage());
            }
          } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
            throw new IllegalStateException("Error loading TransactionFactory class: "
                + e.getMessage());
          }
        }
      }
    }
    return transactionFactory;
  }

  public static TransactionManager getTransactionManager() {
    return getTransactionFactory().getTransactionManager();
  }

  public static UserTransaction getUserTransaction() {
    return getTransactionFactory().getUserTransaction();
  }

  public static String getTxMgrName() {
    return getTransactionFactory().getTxMgrName();
  }

  public static Connection getConnection(GenericHelperInfo helperInfo) throws SQLException,
                                                                      GenericEntityException {
    return getTransactionFactory().getConnection(helperInfo);
  }

  public static void shutdown() {
    getTransactionFactory().shutdown();
  }

  public static Connection getCursorConnection(GenericHelperInfo helperInfo, Connection con) {
    DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperInfo.getHelperBaseName());
    if (datasourceInfo == null) {
      logger.warn("Could not find configuration for " + helperInfo.getHelperBaseName() + " datasource.");
      return con;
    } else if (datasourceInfo.useProxyCursor) {
      try {
        if (datasourceInfo.resultFetchSize > 1)
          con = CursorConnection.newCursorConnection(con, datasourceInfo.cursorName, datasourceInfo.resultFetchSize);
      } catch (Exception ex) {
        logger.warn("Error creating the cursor connection proxy " + helperInfo.getHelperBaseName() + " datasource.", ex
                         );
      }
    }
    return con;
  }
}
