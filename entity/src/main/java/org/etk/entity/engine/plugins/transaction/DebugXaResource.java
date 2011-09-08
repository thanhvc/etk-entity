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

import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;

import org.etk.common.logging.Logger;

public class DebugXaResource extends GenericXaResource {

    private static final Logger logger = Logger.getLogger(DebugXaResource.class);
    public Exception ex = null;

    public DebugXaResource(String info) {
        this.ex = new Exception(info);
    }

    public DebugXaResource() {
        this.ex = new Exception();
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionUtil.debugResMap.remove(xid);
        if (logger.isDebugEnabled()) logger.debug("Xid : " + xid.toString() + " cleared [commit]");
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        TransactionUtil.debugResMap.remove(xid);
        if (logger.isDebugEnabled()) logger.debug("Xid : " + xid.toString() + " cleared [rollback]");
    }

    @Override
    public void enlist() throws XAException {
        super.enlist();
        TransactionUtil.debugResMap.put(xid, this);
    }

    public void log() {
        logger.info("Xid : " + xid);
        logger.info(ex.getMessage(), ex);
    }
}