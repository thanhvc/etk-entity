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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class CursorConnection extends AbstractCursorHandler {

  public static Connection newCursorConnection(Connection con, String cursorName, int pageSize) throws Exception {
      return newHandler(new CursorConnection(con, cursorName, pageSize), Connection.class);
  }

  protected Connection con;

  protected CursorConnection(Connection con, String cursorName, int fetchSize) {
      super(cursorName, fetchSize);
      this.con = con;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("prepareStatement")) {
          System.err.println("prepareStatement");
          args[0] = "DECLARE " + cursorName + " CURSOR FOR " + args[0];
          PreparedStatement pstmt = (PreparedStatement) method.invoke(con, args);
          return CursorStatement.newCursorPreparedStatement(pstmt, cursorName, fetchSize);
      } else if (method.getName().equals("createStatement")) {
          System.err.println("createStatement");
          Statement stmt = (Statement) method.invoke(con, args);
          return CursorStatement.newCursorStatement(stmt, cursorName, fetchSize);
      }
      return super.invoke(con, proxy, method, args);
  }
}