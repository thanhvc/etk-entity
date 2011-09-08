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

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;

import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.plugins.datasource.GenericHelperInfo;

/**
 * TransactionFactory - central source for JTA objects
 */
public interface TransactionFactoryInterface {

    public TransactionManager getTransactionManager();

    public UserTransaction getUserTransaction();

    public String getTxMgrName();

    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException;

    public void shutdown();
}