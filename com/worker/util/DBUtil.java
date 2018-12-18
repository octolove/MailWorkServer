package com.worker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;

/**
 * 数据库操作工具类
 * 
 * @author JC
 * 
 */
public class DBUtil {

	private static DataSource ds = null;

	private static File file = new File("");

	static {
		try {
			InputStream in = new FileInputStream(file.getAbsolutePath() + File.separator + "mailworker.properties");
			Properties propertites = new Properties();
			propertites.load(in);

			ds = BasicDataSourceFactory.createDataSource(propertites);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	/**
	 * @param SQL
	 * @param objs
	 * @return
	 */
	public static int updateByPre(String SQL, Object[] objs) {
		int count = 0;
		PreparedStatement pst = null;
		Connection conn = null;
		try {
			conn = getConnection();
			pst = conn.prepareStatement(SQL);

			if ((objs != null) && (objs.length > 0)) {
				for (int i = 0; i < objs.length; i++) {
					pst.setObject(i + 1, getValue(objs[i]));
				}
			}
			count = pst.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(null, pst, conn);
		}

		return count;
	}

	/**
	 * 帶批处理更新
	 * 
	 * @param SQL
	 * @param objs
	 * @return
	 */
	public static int[] updateByPreBatch(String SQL, List<Object[]> list) {
		int[] count = null;
		PreparedStatement pst = null;
		Connection conn = null;
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			pst = conn.prepareStatement(SQL);

			if ((list != null) && (list.size() > 0)) {
				for (int i = 0; i < list.size(); i++) {
					Object[] objs = list.get(i);
					if (objs != null && objs.length > 0) {
						for (int j = 0; j < objs.length; j++) {
							pst.setObject(j + 1, getValue(objs[j]));
						}
						pst.addBatch();
					}
				}
			}
			count = pst.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			close(null, pst, conn);
		}

		return count;
	}

	public static int insertToKey(String SQL, Object[] objs) {
		int key = 0;
		PreparedStatement pst = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pst = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);

			if ((objs != null) && (objs.length > 0)) {
				for (int i = 0; i < objs.length; i++) {
					pst.setObject(i + 1, getValue(objs[i]));
				}
			}
			pst.executeUpdate();
			rs = pst.getGeneratedKeys();
			if (rs.next())
				key = rs.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pst, conn);
		}

		return key;
	}

	public static List<Map<String, String>> queryByPre(String SQL, String[] columnName, Object[] objs) {
		PreparedStatement pst = null;
		Connection conn = null;
		ResultSet rs = null;
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		try {
			conn = getConnection();
			pst = conn.prepareStatement(SQL);

			if ((objs != null) && (objs.length > 0)) {
				for (int i = 0; i < objs.length; i++) {
					pst.setObject(i + 1, getValue(objs[i]));
				}
			}
			rs = pst.executeQuery();

			while (rs.next()) {
				Map<String, String> map = new HashMap<String, String>();
				for (String str : columnName) {
					map.put(str, rs.getString(str));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pst, conn);
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getValue(T t) {
		if ((t instanceof String))
			return (T) ((t == null) || ("".equals(t)) ? "" : String.valueOf(t));
		if ((t instanceof Integer))
			return (T) (t == null ? Integer.valueOf(0) : t);
		if ((t instanceof Date)) {
			return (T) ((t == null) || ("".equals(t)) ? "" : formatDate2Str((Date) t, null));
		}
		return (T) ((t == null) || ("".equals(t)) ? "" : String.valueOf(t));
	}

	public static String formatDate2Str(Date date, String format) {
		if (date == null) {
			return null;
		}

		if ((format == null) || (format.equals(""))) {
			format = "yyyy-MM-dd HH:mm:ss";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}

	public static void close(ResultSet rs, Statement stmt, Connection conn) {
		try {
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			e.printStackTrace();

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e1) {
					e1.printStackTrace();

					if (conn == null)
						return;
					try {
						conn.close();
					} catch (SQLException e11) {
						e11.printStackTrace();
					}
				} finally {
					if (conn != null)
						try {
							conn.close();
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
				}
				try {
					conn.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();

					if (conn != null)
						try {
							conn.close();
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
				} finally {
					if (conn != null)
						try {
							conn.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
				}
		}
	}

	public static int update(String SQL) {
		int count = 0;
		Statement stmt = null;
		Connection conn = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			count = stmt.executeUpdate(SQL);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(null, stmt, conn);
		}
		return count;
	}

	public static String querySimple(String SQL) {
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		String msg = "";
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next())
				msg = rs.getString(1);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, stmt, conn);
		}
		return msg;
	}

	public static List<String> queryList(String SQL) {
		List<String> list = new ArrayList<String>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next())
				list.add(rs.getString(1));
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, stmt, conn);
		}
		return list;
	}

	public static List<Map<String, String>> queryParam(String SQL, String[] columnName) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next()) {
				Map<String, String> map = new HashMap<String, String>();
				for (String str : columnName) {
					map.put(str, rs.getString(str));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, stmt, conn);
		}
		return list;
	}

	public static List<Map<String, String>> queryKeyValue(String SQL) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next()) {
				Map<String, String> map = new HashMap<String, String>();
				map.put(rs.getString(1), rs.getString(2));
				list.add(map);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, stmt, conn);
		}
		return list;
	}

	public static int queryCount(String SQL) {
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		int count = -1;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next())
				count = rs.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, stmt, conn);
		}
		return count;
	}

	public static int getColumnCount(String tableName) {
		int count = 0;
		Connection conn = null;
		try {
			conn = getConnection();
			Statement sm = conn.createStatement();
			ResultSet rs = sm.executeQuery("select * from " + tableName);
			ResultSetMetaData metaData = rs.getMetaData();
			count = metaData.getColumnCount();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			close(null, null, conn);
		}

		return count;
	}

	public static List<String> getColumnName(String tableName) {
		Connection conn = null;
		Statement sm = null;
		ResultSet rs = null;
		int count = getColumnCount(tableName);
		List<String> cloumName = new ArrayList<String>();
		try {
			conn = getConnection();
			sm = conn.createStatement();
			rs = sm.executeQuery("select * from " + tableName);
			ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 1; i <= count; i++)
				cloumName.add(metaData.getColumnName(i));
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			close(null, sm, conn);
		}

		return cloumName;
	}
}