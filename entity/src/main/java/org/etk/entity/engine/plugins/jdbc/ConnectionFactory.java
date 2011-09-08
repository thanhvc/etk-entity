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
package org.etk.entity.engine.plugins.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.etk.common.logging.Logger;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.plugins.config.EntityConfigUtil;
import org.etk.entity.engine.plugins.connection.ConnectionFactoryInterface;
import org.etk.entity.engine.plugins.datasource.GenericHelperInfo;
import org.etk.entity.engine.plugins.transaction.TransactionFactory;

/**
 * ConnectionFactory - central source for JDBC connections
 *
 */
public class ConnectionFactory {
    // Debug module name
    private static final Logger logger = Logger.getLogger(ConnectionFactory.class);
    private static ConnectionFactoryInterface _factory = null;

    public static Connection getConnection(String driverName, String connectionUrl, Properties props, String userName, String password) throws SQLException {
        // first register the JDBC driver with the DriverManager
        if (driverName != null) {
            ConnectionFactory.loadDriver(driverName);
        }

        try {
            if (UtilValidate.isNotEmpty(userName))
                return DriverManager.getConnection(connectionUrl, userName, password);
            else if (props != null)
                return DriverManager.getConnection(connectionUrl, props);
            else
                return DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            logger.error("SQL Error obtaining JDBC connection", e);
            throw e;
        }
    }

    public static Connection getConnection(String connectionUrl, String userName, String password) throws SQLException {
        return getConnection(null, connectionUrl, null, userName, password);
    }

    public static Connection getConnection(String connectionUrl, Properties props) throws SQLException {
        return getConnection(null, connectionUrl, props, null, null);
    }

    public static Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        // Debug.logVerbose("Getting a connection", module);

        Connection con = TransactionFactory.getConnection(new GenericHelperInfo(null, helperName));
        if (con == null) {
            logger.error("******* ERROR: No database connection found for helperName \"" + helperName + "\"");
        }
        return con;
    }

    public static Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        // Debug.logVerbose("Getting a connection", module);

        Connection con = TransactionFactory.getConnection(helperInfo);
        if (con == null) {
            logger.error("******* ERROR: No database connection found for helperName \"" + helperInfo.getHelperFullName() + "\"");
        }
        return con;
    }

    public static ConnectionFactoryInterface getManagedConnectionFactory() {
        if (_factory == null) { // don't want to block here
            synchronized (TransactionFactory.class) {
                // must check if null again as one of the blocked threads can still enter
                if (_factory == null) {
                    try {
                        String className = EntityConfigUtil.getConnectionFactoryClass();

                        if (className == null) {
                            throw new IllegalStateException("Could not find connection factory class name definition");
                        }
                        Class<?> cfClass = null;

                        if (UtilValidate.isNotEmpty(className)) {
                            try {
                                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                                cfClass = loader.loadClass(className);
                            } catch (ClassNotFoundException e) {
                                logger.warn(e.getMessage(), e);
                                throw new IllegalStateException("Error loading ConnectionFactoryInterface class \"" + className + "\": " + e.getMessage());
                            }
                        }

                        try {
                            _factory = (ConnectionFactoryInterface) cfClass.newInstance();
                        } catch (IllegalAccessException e) {
                            logger.warn(e.getMessage(), e);
                            throw new IllegalStateException("Error loading ConnectionFactoryInterface class \"" + className + "\": " + e.getMessage());
                        } catch (InstantiationException e) {
                            logger.warn(e.getMessage(), e);
                            throw new IllegalStateException("Error loading ConnectionFactoryInterface class \"" + className + "\": " + e.getMessage());
                        }
                    } catch (SecurityException e) {
                        logger.error(e.getMessage(), e);
                        throw new IllegalStateException("Error loading ConnectionFactoryInterface class: " + e.getMessage());
                    }
                }
            }
        }
        return _factory;
    }

    public static Connection getManagedConnection(GenericHelperInfo helperInfo, Element inlineJdbcElement) throws SQLException, GenericEntityException {
        return getManagedConnectionFactory().getConnection(helperInfo, inlineJdbcElement);
    }

    public static void closeAllManagedConnections() {
        getManagedConnectionFactory().closeAll();
    }

    public static void loadDriver(String driverName) throws SQLException {
        if (DriverManager.getDriver(driverName) == null) {
            try {
                Driver driver = (Driver) Class.forName(driverName, true, Thread.currentThread().getContextClassLoader()).newInstance();
                DriverManager.registerDriver(driver);
            } catch (ClassNotFoundException e) {
                logger.warn("Unable to load driver [" + driverName + "]");
            } catch (InstantiationException e) {
              logger.warn("Unable to instantiate driver [" + driverName + "]", e);
            } catch (IllegalAccessException e) {
              logger.warn("Illegal access exception [" + driverName + "]", e);
            }
        }
    }

    public static void unloadDriver(String driverName) throws SQLException {
        Driver driver = DriverManager.getDriver(driverName);
        if (driver != null) {
            DriverManager.deregisterDriver(driver);
        }
    }
}
