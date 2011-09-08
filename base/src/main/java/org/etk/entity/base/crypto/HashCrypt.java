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
package org.etk.entity.base.crypto;


import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.etk.common.logging.Logger;
import org.etk.entity.base.utils.GeneralRuntimeException;
import org.etk.entity.base.utils.StringUtil;
import org.etk.entity.base.utils.UtilValidate;

/**
 * Utility class for doing SHA-1/MD5 One-Way Hash Encryption
 *
 */
public class HashCrypt {

    private static final Logger logger = Logger.getLogger(HashCrypt.class);

    public static String getDigestHash(String str) {
        return getDigestHash(str, "SHA");
    }

    public static String getDigestHash(String str, String hashType) {
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte[] strBytes = str.getBytes();

            messagedigest.update(strBytes);
            byte[] digestBytes = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);

            StringBuilder sb = new StringBuilder();
            sb.append("{").append(hashType).append("}");
            sb.append(digestChars, 0, digestChars.length);
            return sb.toString();
        } catch (Exception e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    public static String getDigestHash(String str, String code, String hashType) {
        if (str == null) return null;
        try {
            byte codeBytes[] = null;

            if (code == null) codeBytes = str.getBytes();
            else codeBytes = str.getBytes(code);
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);

            messagedigest.update(codeBytes);
            byte digestBytes[] = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);
            StringBuilder sb = new StringBuilder();
            sb.append("{").append(hashType).append("}");
            sb.append(digestChars, 0, digestChars.length);
            return sb.toString();
        } catch (Exception e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    public static String getHashTypeFromPrefix(String hashString) {
        if (UtilValidate.isEmpty(hashString) || hashString.charAt(0) != '{') {
            return null;
        }

        return hashString.substring(1, hashString.indexOf('}'));
    }

    public static String removeHashTypePrefix(String hashString) {
        if (UtilValidate.isEmpty(hashString) || hashString.charAt(0) != '{') {
            return hashString;
        }

        return hashString.substring(hashString.indexOf('}') + 1);
    }

    public static String getDigestHashOldFunnyHexEncode(String str, String hashType) {
        if (UtilValidate.isEmpty(hashType)) hashType = "SHA";
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte strBytes[] = str.getBytes();

            messagedigest.update(strBytes);
            byte digestBytes[] = messagedigest.digest();
            int k = 0;
            char digestChars[] = new char[digestBytes.length * 2];

            for (int l = 0; l < digestBytes.length; l++) {
                int i1 = digestBytes[l];

                if (i1 < 0) {
                    i1 = 127 + i1 * -1;
                }
                StringUtil.encodeInt(i1, k, digestChars);
                k += 2;
            }

            return new String(digestChars, 0, digestChars.length);
        } catch (Exception e) {
            logger.error("Error while computing hash of type " + hashType, e);
        }
        return str;
    }
}