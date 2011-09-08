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
package org.etk.entity.engine.plugins.util;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.transaction.Transaction;

import org.etk.common.logging.Logger;
import org.etk.entity.base.crypto.DesCrypt;
import org.etk.entity.base.crypto.HashCrypt;
import org.etk.entity.base.utils.GeneralException;
import org.etk.entity.base.utils.StringUtil;
import org.etk.entity.base.utils.UtilObject;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.EntityCryptoException;
import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.core.GenericValue;
import org.etk.entity.engine.plugins.transaction.GenericTransactionException;
import org.etk.entity.engine.plugins.transaction.TransactionUtil;

public class EntityCrypto {

  private static final Logger logger = Logger.getLogger(EntityCrypto.class);

  protected Delegator delegator = null;
  protected Map<String, SecretKey> keyMap = null;

  protected EntityCrypto() { }
  public EntityCrypto(Delegator delegator) {
      this.delegator = delegator;
      this.keyMap = new HashMap<String, SecretKey>();

      // check the key table and make sure there
      // make sure there are some dummy keys
      synchronized(EntityCrypto.class) {
          try {
              long size = delegator.findCountByCondition("EntityKeyStore", null, null, null);
              if (size == 0) {
                  for (int i = 0; i < 20; i++) {
                      String randomName = this.getRandomString();
                      this.getKeyFromStore(randomName, false);
                  }
              }
          } catch (GenericEntityException e) {
            logger.error(e.getMessage(), e);
          }
      }
  }

  /** Encrypts an Object into an encrypted hex encoded String */
  public String encrypt(String keyName, Object obj) throws EntityCryptoException {
      try {
          byte[] encryptedBytes = DesCrypt.encrypt(this.getKey(keyName, false), UtilObject.getBytes(obj));
          String hexString = StringUtil.toHexString(encryptedBytes);
          return hexString;
      } catch (GeneralException e) {
          throw new EntityCryptoException(e);
      }
  }

  /** Decrypts a hex encoded String into an Object */
  public Object decrypt(String keyName, String encryptedString) throws EntityCryptoException {
      Object decryptedObj = null;
      byte[] encryptedBytes = StringUtil.fromHexString(encryptedString);
      try {
          SecretKey decryptKey = this.getKey(keyName, false);
          byte[] decryptedBytes = DesCrypt.decrypt(decryptKey, encryptedBytes);

          decryptedObj = UtilObject.getObject(decryptedBytes);
      } catch (GeneralException e) {
          try {
              // try using the old/bad hex encoding approach; this is another path the code may take, ie if there is an exception thrown in decrypt
              logger.info("Decrypt with DES key from standard key name hash failed, trying old/funny variety of key name hash");
              SecretKey decryptKey = this.getKey(keyName, true);
              byte[] decryptedBytes = DesCrypt.decrypt(decryptKey, encryptedBytes);
              decryptedObj = UtilObject.getObject(decryptedBytes);
              //Debug.logInfo("Old/funny variety succeeded: Decrypted value [" + encryptedString + "]", module);
          } catch (GeneralException e1) {
              // NOTE: this throws the original exception back, not the new one if it fails using the other approach
              throw new EntityCryptoException(e);
          }
      }

      // NOTE: this is definitely for debugging purposes only, do not uncomment in production server for security reasons: Debug.logInfo("Decrypted value [" + encryptedString + "] to result: " + decryptedObj, module);
      return decryptedObj;
  }

  protected SecretKey getKey(String name, boolean useOldFunnyKeyHash) throws EntityCryptoException {
      String keyMapName = name + useOldFunnyKeyHash;
      SecretKey key = keyMap.get(keyMapName);
      if (key == null) {
          synchronized(this) {
              key = this.getKeyFromStore(name, useOldFunnyKeyHash);
              keyMap.put(keyMapName, key);
          }
      }
      return key;
  }

  protected SecretKey getKeyFromStore(String originalKeyName, boolean useOldFunnyKeyHash) throws EntityCryptoException {
      String hashedKeyName = useOldFunnyKeyHash? HashCrypt.getDigestHashOldFunnyHexEncode(originalKeyName, null) : HashCrypt.getDigestHash(originalKeyName);

      GenericValue keyValue = null;
      try {
          keyValue = delegator.findOne("EntityKeyStore", false, "keyName", hashedKeyName);
      } catch (GenericEntityException e) {
          throw new EntityCryptoException(e);
      }
      if (keyValue == null || keyValue.get("keyText") == null) {
          SecretKey key = null;
          try {
              key = DesCrypt.generateKey();
          } catch (NoSuchAlgorithmException e) {
              throw new EntityCryptoException(e);
          }
          GenericValue newValue = delegator.makeValue("EntityKeyStore");
          newValue.set("keyText", StringUtil.toHexString(key.getEncoded()));
          newValue.set("keyName", hashedKeyName);

          Transaction parentTransaction = null;
          boolean beganTrans = false;
          try {
              beganTrans = TransactionUtil.begin();
          } catch (GenericTransactionException e) {
              throw new EntityCryptoException(e);
          }

          if (!beganTrans) {
              try {
                  parentTransaction = TransactionUtil.suspend();
              } catch (GenericTransactionException e) {
                  throw new EntityCryptoException(e);
              }

              // now start a new transaction
              try {
                  beganTrans = TransactionUtil.begin();
              } catch (GenericTransactionException e) {
                  throw new EntityCryptoException(e);
              }
          }

          try {
              delegator.create(newValue);
          } catch (GenericEntityException e) {
              try {
                  TransactionUtil.rollback(beganTrans, "Error creating encrypted value", e);
              } catch (GenericTransactionException e1) {
                  logger.error("Could not rollback transaction", e1);
              }
              throw new EntityCryptoException(e);
          } finally {
              try {
                  TransactionUtil.commit(beganTrans);
              } catch (GenericTransactionException e) {
                  throw new EntityCryptoException(e);
              }
              // resume the parent transaction
              if (parentTransaction != null) {
                  try {
                      TransactionUtil.resume(parentTransaction);
                  } catch (GenericTransactionException e) {
                      throw new EntityCryptoException(e);
                  }
              }
          }


          return key;
      } else {
          byte[] keyBytes = StringUtil.fromHexString(keyValue.getString("keyText"));
          try {
              return DesCrypt.getDesKey(keyBytes);
          } catch (GeneralException e) {
              throw new EntityCryptoException(e);
          }
      }
  }

  protected String getRandomString() {
      Random rand = new Random();
      byte[] randomBytes = new byte[24];
      rand.nextBytes(randomBytes);
      return StringUtil.toHexString(randomBytes);
  }
}
