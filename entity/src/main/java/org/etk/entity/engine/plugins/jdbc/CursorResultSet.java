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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class CursorResultSet extends AbstractCursorHandler {

    protected ResultSet rs;
    protected Statement stmt;
    protected String query;

    protected CursorResultSet(Statement stmt, String cursorName, int fetchSize) throws SQLException {
        super(cursorName, fetchSize);
        this.stmt = stmt;
        query = "FETCH FORWARD " + fetchSize + " IN " + cursorName;
        System.err.println("executing page fetch(1)");
        rs = stmt.executeQuery(query);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            close();
            return null;
        } else if ("next".equals(method.getName())) {
            return next() ? Boolean.TRUE : Boolean.FALSE;
        }
        return super.invoke(rs, proxy, method, args);
    }

    protected boolean next() throws SQLException {
        if (rs.next()) return true;
        System.err.println("executing page fetch(2)");
        rs = stmt.executeQuery(query);
        return rs.next();
    }

    protected void close() throws SQLException {
        stmt.executeUpdate("CLOSE " + cursorName);
        rs.close();
    }

    public static ResultSet newCursorResultSet(Statement stmt, String cursorName, int fetchSize) throws SQLException, Exception {
        return newHandler(new CursorResultSet(stmt, cursorName, fetchSize), ResultSet.class);
    }
}