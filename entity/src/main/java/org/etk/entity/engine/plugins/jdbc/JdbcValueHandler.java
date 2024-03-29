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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;

import org.etk.common.logging.Logger;

import javolution.util.FastMap;

/**
 * An object that handles getting/setting column values in JDBC
 * <code>PreparedStatement</code> and <code>ResultSet</code> objects.
 */
public abstract class JdbcValueHandler<T> {
  private static final Logger                            logger              = Logger.getLogger(JdbcValueHandler.class);

  private static final Map<String, JdbcValueHandler<?>> JdbcValueHandlerMap = createJdbcValueHandlerMap();

  private static final Map<String, Integer>             SqlTypeMap          = createSqlTypeMap();

  private static Map<String, JdbcValueHandler<?>> createJdbcValueHandlerMap() {
    /*
     * This Map is used to select the correct JdbcValueHandler for the specified
     * Java type. The JdbcValueHandler instances are initialized with the SQL
     * type recommended by Sun/Oracle.
     */
    Map<String, JdbcValueHandler<?>> result = FastMap.newInstance();
    // JDBC 1
    result.put("byte[]", new ByteArrayJdbcValueHandler(Types.LONGVARBINARY));
    result.put("java.lang.Boolean", new BooleanJdbcValueHandler(Types.BOOLEAN));
    result.put("Boolean", new BooleanJdbcValueHandler(Types.BOOLEAN));
    result.put("java.lang.Double", new DoubleJdbcValueHandler(Types.DOUBLE));
    result.put("Double", new DoubleJdbcValueHandler(Types.DOUBLE));
    result.put("java.lang.Float", new FloatJdbcValueHandler(Types.FLOAT));
    result.put("Float", new FloatJdbcValueHandler(Types.FLOAT));
    result.put("java.lang.Integer", new IntegerJdbcValueHandler(Types.INTEGER));
    result.put("Integer", new IntegerJdbcValueHandler(Types.INTEGER));
    result.put("java.lang.Long", new LongJdbcValueHandler(Types.BIGINT));
    result.put("Long", new LongJdbcValueHandler(Types.BIGINT));
    result.put("java.lang.Short", new ShortJdbcValueHandler(Types.SMALLINT));
    result.put("Short", new ShortJdbcValueHandler(Types.SMALLINT));
    result.put("java.lang.String", new StringJdbcValueHandler(Types.CHAR));
    result.put("String", new StringJdbcValueHandler(Types.CHAR));
    result.put("java.sql.Date", new DateJdbcValueHandler(Types.DATE));
    result.put("Date", new DateJdbcValueHandler(Types.DATE));
    result.put("java.sql.Time", new TimeJdbcValueHandler(Types.TIME));
    result.put("Time", new TimeJdbcValueHandler(Types.TIME));
    result.put("java.sql.Timestamp", new TimestampJdbcValueHandler(Types.TIMESTAMP));
    result.put("Timestamp", new TimestampJdbcValueHandler(Types.TIMESTAMP));
    // JDBC 2
    result.put("java.math.BigDecimal", new BigDecimalJdbcValueHandler(Types.DECIMAL));
    result.put("BigDecimal", new BigDecimalJdbcValueHandler(Types.DECIMAL));
    result.put("java.sql.Blob", new BlobJdbcValueHandler(Types.BLOB));
    result.put("Blob", new BlobJdbcValueHandler(Types.BLOB));
    result.put("java.sql.Clob", new ClobJdbcValueHandler(Types.CLOB));
    result.put("Clob", new ClobJdbcValueHandler(Types.CLOB));
    // Non-JDBC Types
    result.put("java.lang.Object", new ObjectJdbcValueHandler(Types.BLOB));
    result.put("Object", new ObjectJdbcValueHandler(Types.BLOB));
    return result;
  }

  private static Map<String, Integer> createSqlTypeMap() {
    /*
     * This Map is used to select the correct SQL data type for the
     * PreparedStatement.setNull method. The setNull method must be called with
     * the correct type, or an exception will be thrown.
     */
    Map<String, Integer> result = FastMap.newInstance();
    // SQL 99 Data Types
    result.put("BIT", Types.BIT);
    result.put("BLOB", Types.BLOB);
    result.put("BOOLEAN", Types.BOOLEAN);
    result.put("CHAR", Types.CHAR);
    result.put("CHARACTER", Types.CHAR);
    result.put("CLOB", Types.CLOB);
    result.put("DATE", Types.DATE);
    result.put("DEC", Types.DECIMAL);
    result.put("DECIMAL", Types.DECIMAL);
    result.put("DOUBLE", Types.DOUBLE);
    result.put("DOUBLE PRECISION", Types.DOUBLE);
    result.put("FLOAT", Types.FLOAT);
    result.put("INT", Types.INTEGER);
    result.put("INTEGER", Types.INTEGER);
    result.put("NUMERIC", Types.NUMERIC);
    result.put("REAL", Types.REAL);
    result.put("SMALLINT", Types.SMALLINT);
    result.put("TIME", Types.TIME);
    result.put("TIMESTAMP", Types.TIMESTAMP);
    result.put("VARCHAR", Types.VARCHAR);
    result.put("CHAR VARYING", Types.VARCHAR);
    result.put("CHARACTER VARYING", Types.VARCHAR);
    // DB2, MS SQL Data Types
    // Note: Do NOT map the DATETIME SQL data type, the
    // java-type will be used to select the correct data type
    result.put("LONGVARCHAR", Types.LONGVARCHAR);
    result.put("LONG VARCHAR", Types.LONGVARCHAR);
    result.put("BIGINT", Types.BIGINT);
    result.put("TEXT", Types.LONGVARCHAR);
    result.put("IMAGE", Types.BLOB);
    result.put("BINARY", Types.BINARY);
    result.put("VARBINARY", Types.VARBINARY);
    result.put("LONGVARBINARY", Types.LONGVARBINARY);
    result.put("LONG VARBINARY", Types.LONGVARBINARY);
    // PostgreSQL SQL Data Types
    result.put("BYTEA", Types.BINARY);
    return result;
  }

  /**
   * Returns the <code>JdbcValueHandler</code> that corresponds to a field type.
   * 
   * @param javaType The Java type specified in fieldtype*.xml
   * @param sqlType The SQL type specified in fieldtype*.xml
   * @return A <code>JdbcValueHandler</code> instance
   */
  public static JdbcValueHandler<?> getInstance(String javaType, String sqlType) {
    JdbcValueHandler<?> handler = JdbcValueHandlerMap.get(javaType);
    if (handler != null) {
      String key = parseSqlType(sqlType);
      Integer sqlTypeInt = SqlTypeMap.get(key);
      if (sqlTypeInt != null) {
        handler = handler.create(sqlTypeInt);
      }
    }
    return handler;
  }

  protected static String parseSqlType(String sqlType) {
    String result = sqlType.toUpperCase();
    int pos = result.indexOf("(");
    if (pos != -1) {
      result = result.substring(0, pos);
    }
    return result;
  }

  protected static byte[] serializeObject(Object obj) throws SQLException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(os);
      oos.writeObject(obj);
      os.close();
    } catch (IOException e) {
      throw new SQLException(e);
    } finally {
      if (oos != null) {
        try {
          oos.close();
        } catch (IOException e) {
        }
      }
    }
    return os.toByteArray();
  }

  protected static byte[] toByteArray(java.sql.Blob blob) throws SQLException {
    InputStream inStream = null;
    try {
      inStream = blob.getBinaryStream();
      int blobLength = (int) blob.length();
      byte[] byteBuffer = new byte[blobLength];
      int offset = 0;
      int bytesRead = inStream.read(byteBuffer, offset, blobLength);
      while (bytesRead > 0) {
        offset += bytesRead;
        bytesRead = inStream.read(byteBuffer, offset, blobLength);
      }
      return byteBuffer;
    } catch (Exception e) {
      throw new SQLException(e);
    } finally {
      if (inStream != null) {
        try {
          inStream.close();
        } catch (IOException e) {
        }
      }
    }
  }

  // The target database SQL type to be used for the
  // PreparedStatement.setNull method.
  private final int sqlType;

  protected JdbcValueHandler(int sqlType) {
    this.sqlType = sqlType;
  }

  /**
   * Sets a value in a <code>PreparedStatement</code>. The <code>obj</code>
   * argument is converted to the correct data type. Subclasses override this
   * method to cast <code>obj</code> to the correct data type and call the
   * appropriate <code>PreparedStatement.setXxx</code> method.
   * 
   * @param ps
   * @param parameterIndex
   * @param obj
   * @throws SQLException
   */
  protected abstract void castAndSetValue(PreparedStatement ps, int parameterIndex, T obj) throws SQLException;

  protected JdbcValueHandler<T> create(int sqlType) {
    if (sqlType == this.getSqlType()) {
      return this;
    }
    return newInstance(sqlType);
  }

  /**
   * Returns the SQL type for this handler.
   * 
   * @return return the SQL type
   * @see <code>java.sql.Types</code>
   */
  public int getSqlType() {
    return this.sqlType;
  }

  /**
   * Returns a value from a <code>ResultSet</code>. The returned object is
   * converted to the Java data type specified in the fieldtype file.
   * 
   * @param rs the ResultSet object
   * @param columnIndex the column index
   * @return get value from result set
   * @throws SQLException
   */
  public abstract T getValue(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Returns a new instance of the object - initialized with the specified SQL
   * type.
   * 
   * @param sqlType the sql type
   * @return returns a new instance
   */
  protected abstract JdbcValueHandler<T> newInstance(int sqlType);

  public abstract Class<T> getJavaClass();

  /**
   * Sets a value in a <code>PreparedStatement</code>. The <code>obj</code>
   * argument is converted to the correct data type.
   * 
   * @param ps
   * @param parameterIndex
   * @param obj
   * @throws SQLException
   */
  public void setValue(PreparedStatement ps, int parameterIndex, T obj) throws SQLException {
    if (obj == null) {
      ps.setNull(parameterIndex, this.getSqlType());
      return;
    }
    this.castAndSetValue(ps, parameterIndex, obj);
  }

  /**
   * A <code>java.math.BigDecimal</code> JDBC value handler.
   */
  protected static class BigDecimalJdbcValueHandler extends JdbcValueHandler<java.math.BigDecimal> {
    protected BigDecimalJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<java.math.BigDecimal> getJavaClass() {
      return java.math.BigDecimal.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps,
                                   int parameterIndex,
                                   java.math.BigDecimal obj) throws SQLException {
      ps.setBigDecimal(parameterIndex, obj);
    }

    @Override
    public java.math.BigDecimal getValue(ResultSet rs, int columnIndex) throws SQLException {
      return rs.getBigDecimal(columnIndex);
    }

    @Override
    protected JdbcValueHandler<java.math.BigDecimal> newInstance(int sqlType) {
      return new BigDecimalJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.sql.Blob</code> JDBC value handler.
   */
  protected static class BlobJdbcValueHandler extends JdbcValueHandler<Object> {
    protected BlobJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Object> getJavaClass() {
      return Object.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
      try {
        // FIXME: This is here for backwards compatibility. Client code
        // that uses a Blob java-type for a byte array should use a
        // byte[] java-type instead.
        byte[] bytes = (byte[]) obj;
        logger.warn("Blob java-type used for byte array. Use byte[] java-type instead.");
        ps.setBytes(parameterIndex, bytes);
        return;
      } catch (ClassCastException e) {
      }
      ps.setBlob(parameterIndex, (Blob) obj);
    }

    @Override
    public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
      // FIXME: This code is here for backwards compatibility. Client code
      // that uses a Blob java-type for non-Blob types should be updated
      // to use the correct type.
      Object originalObject;
      byte[] fieldBytes;
      try {
        Blob theBlob = rs.getBlob(columnIndex);
        fieldBytes = toByteArray(theBlob);
        originalObject = theBlob;
      } catch (SQLException e) {
        // for backward compatibility if getBlob didn't work try getBytes
        fieldBytes = rs.getBytes(columnIndex);
        originalObject = fieldBytes;
      }
      if (originalObject != null) {
        // for backward compatibility, check to see if there is a serialized
        // object and if so return that
        Object blobObject = null;
        ObjectInputStream in = null;
        try {
          in = new ObjectInputStream(new ByteArrayInputStream(fieldBytes));
          blobObject = in.readObject();
        } catch (IOException e) {
          if (logger.isDebugEnabled())
            logger.debug("Unable to read BLOB data from input stream", e);
        } catch (ClassNotFoundException e) {
          if (logger.isDebugEnabled())
            logger.debug("Class not found: Unable to cast BLOB data to an Java object while getting value, most likely because it is a straight byte[], so just using the raw bytes", e);
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
            }
          }
        }
        if (blobObject != null) {
          logger.debug("Blob java-type used for java.lang.Object. Use java.lang.Object java-type instead.");
          return blobObject;
        } else {
          if (originalObject instanceof Blob) {
            // NOTE using SerialBlob here instead of the Blob from the database
            // to make sure we can pass it around, serialize it, etc
            return new SerialBlob((Blob) originalObject);
          } else {
            logger.warn("Blob java-type used for byte array. Use byte[] java-type instead.");
            return originalObject;
          }
        }
      }
      return null;
    }

    @Override
    protected JdbcValueHandler<Object> newInstance(int sqlType) {
      return new BlobJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Boolean</code> JDBC value handler.
   */
  protected static class BooleanJdbcValueHandler extends JdbcValueHandler<Boolean> {
    protected BooleanJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Boolean> getJavaClass() {
      return Boolean.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Boolean obj) throws SQLException {
      ps.setBoolean(parameterIndex, obj);
    }

    @Override
    public Boolean getValue(ResultSet rs, int columnIndex) throws SQLException {
      boolean value = rs.getBoolean(columnIndex);
      return rs.wasNull() ? null : Boolean.valueOf(value);
    }

    @Override
    protected JdbcValueHandler<Boolean> newInstance(int sqlType) {
      return new BooleanJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>byte[]</code> JDBC value handler.
   */
  protected static class ByteArrayJdbcValueHandler extends JdbcValueHandler<byte[]> {
    protected ByteArrayJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<byte[]> getJavaClass() {
      return byte[].class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, byte[] obj) throws SQLException {
      ps.setBytes(parameterIndex, obj);
    }

    @Override
    public byte[] getValue(ResultSet rs, int columnIndex) throws SQLException {
      byte[] value = rs.getBytes(columnIndex);
      return rs.wasNull() ? null : value;
    }

    @Override
    protected JdbcValueHandler<byte[]> newInstance(int sqlType) {
      return new ByteArrayJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.sql.Clob</code> JDBC value handler.
   */
  protected static class ClobJdbcValueHandler extends JdbcValueHandler<Object> {
    protected ClobJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Object> getJavaClass() {
      return Object.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
      try {
        // FIXME: This is here for backwards compatibility. Client code
        // that uses a Clob java-type for a java.lang.String should use a
        // java.lang.String java-type instead.
        String str = (String) obj;
        logger.warn("Clob java-type used for java.lang.String. Use java.lang.String java-type instead.");
        ps.setString(parameterIndex, str);
        return;
      } catch (ClassCastException e) {
      }
      ps.setClob(parameterIndex, (java.sql.Clob) obj);
      return;
    }

    @Override
    public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
      java.sql.Clob clob = rs.getClob(columnIndex);
      if (clob == null || clob.length() == 0) {
        return null;
      }
      Reader clobReader = null;
      try {
        clobReader = clob.getCharacterStream();
        int clobLength = (int) clob.length();
        char[] charBuffer = new char[clobLength];
        int offset = 0;
        int charsRead = clobReader.read(charBuffer, offset, clobLength);
        while (charsRead > 0) {
          offset += charsRead;
          charsRead = clobReader.read(charBuffer, offset, clobLength);
        }
        // FIXME: This is here for backwards compatibility. Client code
        // that uses a Clob java-type for a java.lang.String should use a
        // java.lang.String java-type instead.
        return new String(charBuffer);
      } catch (IOException e) {
        throw new SQLException(e);
      } finally {
        if (clobReader != null) {
          try {
            clobReader.close();
          } catch (IOException e) {
          }
        }
      }
    }

    @Override
    protected JdbcValueHandler<Object> newInstance(int sqlType) {
      return new ClobJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.sql.Date</code> JDBC value handler.
   */
  protected static class DateJdbcValueHandler extends JdbcValueHandler<java.sql.Date> {
    protected DateJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<java.sql.Date> getJavaClass() {
      return java.sql.Date.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, java.sql.Date obj) throws SQLException {
      ps.setDate(parameterIndex, obj);
    }

    @Override
    public java.sql.Date getValue(ResultSet rs, int columnIndex) throws SQLException {
      return rs.getDate(columnIndex);
    }

    @Override
    protected JdbcValueHandler<java.sql.Date> newInstance(int sqlType) {
      return new DateJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Double</code> JDBC value handler.
   */
  protected static class DoubleJdbcValueHandler extends JdbcValueHandler<Double> {
    protected DoubleJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Double> getJavaClass() {
      return Double.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Double obj) throws SQLException {
      ps.setDouble(parameterIndex, obj);
    }

    @Override
    public Double getValue(ResultSet rs, int columnIndex) throws SQLException {
      double value = rs.getDouble(columnIndex);
      return rs.wasNull() ? null : Double.valueOf(value);
    }

    @Override
    protected JdbcValueHandler<Double> newInstance(int sqlType) {
      return new DoubleJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Float</code> JDBC value handler.
   */
  protected static class FloatJdbcValueHandler extends JdbcValueHandler<Float> {
    protected FloatJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Float> getJavaClass() {
      return Float.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Float obj) throws SQLException {
      ps.setFloat(parameterIndex, obj);
    }

    @Override
    public Float getValue(ResultSet rs, int columnIndex) throws SQLException {
      float value = rs.getFloat(columnIndex);
      return rs.wasNull() ? null : Float.valueOf(value);
    }

    @Override
    protected JdbcValueHandler<Float> newInstance(int sqlType) {
      return new FloatJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Integer</code> JDBC value handler.
   */
  protected static class IntegerJdbcValueHandler extends JdbcValueHandler<Integer> {
    protected IntegerJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Integer> getJavaClass() {
      return Integer.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Integer obj) throws SQLException {
      ps.setInt(parameterIndex, obj);
    }

    @Override
    public Integer getValue(ResultSet rs, int columnIndex) throws SQLException {
      int value = rs.getInt(columnIndex);
      return rs.wasNull() ? null : Integer.valueOf(value);
    }

    @Override
    protected JdbcValueHandler<Integer> newInstance(int sqlType) {
      return new IntegerJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Long</code> JDBC value handler.
   */
  protected static class LongJdbcValueHandler extends JdbcValueHandler<Long> {
    protected LongJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Long> getJavaClass() {
      return Long.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Long obj) throws SQLException {
      ps.setLong(parameterIndex, obj);
    }

    @Override
    public Long getValue(ResultSet rs, int columnIndex) throws SQLException {
      long value = rs.getLong(columnIndex);
      return rs.wasNull() ? null : Long.valueOf(value);
    }

    @Override
    protected JdbcValueHandler<Long> newInstance(int sqlType) {
      return new LongJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Object</code> JDBC value handler.
   */
  protected static class ObjectJdbcValueHandler extends JdbcValueHandler<Object> {
    protected ObjectJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Object> getJavaClass() {
      return Object.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
      ps.setBytes(parameterIndex, serializeObject(obj));
    }

    @Override
    public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
      ObjectInputStream in = null;
      InputStream bis = null;
      try {
        bis = rs.getBinaryStream(columnIndex);
        if (bis == null) {
          return null;
        }
        in = new ObjectInputStream(bis);
        return in.readObject();
      } catch (Exception e) {
        throw new SQLException(e);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
          }
        }
        if (bis != null) {
          try {
            bis.close();
          } catch (IOException e) {
          }
        }
      }
    }

    @Override
    protected JdbcValueHandler<Object> newInstance(int sqlType) {
      return new ObjectJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.Short</code> JDBC value handler.
   */
  protected static class ShortJdbcValueHandler extends JdbcValueHandler<Short> {
    protected ShortJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<Short> getJavaClass() {
      return Short.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Short obj) throws SQLException {
      ps.setShort(parameterIndex, obj);
    }

    @Override
    public Short getValue(ResultSet rs, int columnIndex) throws SQLException {
      short value = rs.getShort(columnIndex);
      return rs.wasNull() ? null : Short.valueOf(value);
    }

    @Override
    protected JdbcValueHandler<Short> newInstance(int sqlType) {
      return new ShortJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.lang.String</code> JDBC value handler.
   */
  protected static class StringJdbcValueHandler extends JdbcValueHandler<String> {
    protected StringJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<String> getJavaClass() {
      return String.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, String obj) throws SQLException {
      ps.setString(parameterIndex, obj);
    }

    @Override
    public String getValue(ResultSet rs, int columnIndex) throws SQLException {
      return rs.getString(columnIndex);
    }

    @Override
    protected JdbcValueHandler<String> newInstance(int sqlType) {
      return new StringJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.sql.Time</code> JDBC value handler.
   */
  protected static class TimeJdbcValueHandler extends JdbcValueHandler<java.sql.Time> {
    protected TimeJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<java.sql.Time> getJavaClass() {
      return java.sql.Time.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, java.sql.Time obj) throws SQLException {
      ps.setTime(parameterIndex, obj);
    }

    @Override
    public java.sql.Time getValue(ResultSet rs, int columnIndex) throws SQLException {
      return rs.getTime(columnIndex);
    }

    @Override
    protected JdbcValueHandler<java.sql.Time> newInstance(int sqlType) {
      return new TimeJdbcValueHandler(sqlType);
    }
  }

  /**
   * A <code>java.sql.Timestamp</code> JDBC value handler.
   * <p>
   * This <code>JdbcValueHandler</code> accommodates databases that don't
   * support sub-second precision. If the date-time field type is a
   * <code>CHAR(30)</code> SQL type, <code>java.sql.Timestamp</code>s will be
   * stored as JDBC timestamp escape format strings (
   * <code>yyyy-mm-dd hh:mm:ss.fffffffff</code>), referenced to UTC.
   * </p>
   */
  protected static class TimestampJdbcValueHandler extends JdbcValueHandler<java.sql.Timestamp> {
    protected TimestampJdbcValueHandler(int jdbcType) {
      super(jdbcType);
    }

    @Override
    public Class<java.sql.Timestamp> getJavaClass() {
      return java.sql.Timestamp.class;
    }

    @Override
    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, java.sql.Timestamp obj) throws SQLException {
      ps.setTimestamp(parameterIndex, obj);
    }

    @Override
    public java.sql.Timestamp getValue(ResultSet rs, int columnIndex) throws SQLException {
      return rs.getTimestamp(columnIndex);
    }

    @Override
    protected JdbcValueHandler<java.sql.Timestamp> newInstance(int sqlType) {
      if (sqlType == Types.CHAR) {
        return new TimestampJdbcValueHandler(sqlType) {
          @Override
          protected void castAndSetValue(PreparedStatement ps,
                                         int parameterIndex,
                                         java.sql.Timestamp obj) throws SQLException {
            ps.setString(parameterIndex, obj.toString());
          }

          @Override
          public java.sql.Timestamp getValue(ResultSet rs, int columnIndex) throws SQLException {
            String str = rs.getString(columnIndex);
            if (str == null) {
              return null;
            }
            return Timestamp.valueOf(str);
          }
        };
      } else {
        return new TimestampJdbcValueHandler(sqlType);
      }
    }
  }
}
