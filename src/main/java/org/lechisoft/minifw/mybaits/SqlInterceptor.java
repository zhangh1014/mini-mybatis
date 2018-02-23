package org.lechisoft.minifw.mybaits;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.lechisoft.minifw.log.MiniLog;

//@Intercepts({
//        @Signature(method = "query", type = Executor.class, args = { MappedStatement.class, Object.class,
//                RowBounds.class, ResultHandler.class }),
//        @Signature(method = "query", type = Executor.class, args = { MappedStatement.class, Object.class,
//                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class }),
//        @Signature(method = "update", type = Executor.class, args = { MappedStatement.class, Object.class }) })
@Intercepts({
		@Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }) })
public class SqlInterceptor implements Interceptor {

	public enum DataBaseType {
		ORACLE, SQLSERVER, MYSQL
	}

	DataBaseType dataBaseType = DataBaseType.ORACLE;

	public SqlInterceptor(DataBaseType type) {
		this.dataBaseType = type;
	}

	public Object intercept(Invocation invocation) throws Throwable {

		if (invocation.getTarget() instanceof RoutingStatementHandler) {
			RoutingStatementHandler routingStatementHandler = (RoutingStatementHandler) invocation.getTarget();
			StatementHandler statementHandler = (StatementHandler) ReflectUtil.getFieldValue(routingStatementHandler,
					"delegate");

			// 获取Paging对象
			Paging paging = getPaging(statementHandler.getBoundSql());

			if (null != paging && paging.getEnabled()) {

				// 获取并设置总记录数、总页数
				Connection connection = (Connection) invocation.getArgs()[0];
				int totalRecord = this.getTotalRecord(statementHandler, connection, paging);
				paging.setTotalRecord(totalRecord);

				int pageSize = paging.getPageSize();
				int totalPage = totalRecord % pageSize == 0 ? totalRecord / pageSize : totalRecord / pageSize + 1;
				paging.setTotalPage(totalPage);

				int gotoPage = paging.getGotoPage();
				gotoPage = gotoPage < 1 ? 1 : gotoPage;
				gotoPage = gotoPage > totalPage ? totalPage : gotoPage;
				paging.setGotoPage(gotoPage);

				// 处理分页
				this.paging(statementHandler, paging);
			}
		}

		return invocation.proceed();
	}

	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	public void setProperties(Properties arg0) {
	}

	/**
	 * 从ParameterObject中获取Paging对象
	 * 
	 * @param boundSql
	 *            BoundSql对象
	 * @return Paging对象
	 */
	private Paging getPaging(BoundSql boundSql) {
		Object paramObject = boundSql.getParameterObject();

		Paging paging = null;
		if (null != paramObject) {
			if (this.isBaseType(paramObject)) {

			} else if (paramObject instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> paramObjectMap = (Map<String, Object>) paramObject;
				for (Map.Entry<String, Object> entry : paramObjectMap.entrySet()) {
					if (entry.getValue() instanceof Paging) {
						paging = (Paging) entry.getValue();
						break;
					}
				}
			} else {
				Field[] fields = null;
				if (paramObject.getClass().getSuperclass() != null
						&& paramObject.getClass().getSuperclass().equals(Pageable.class)) {
					fields = paramObject.getClass().getSuperclass().getDeclaredFields();
				} else {
					fields = paramObject.getClass().getDeclaredFields();
				}

				for (Field itemField : fields) {
					if (itemField.getType().equals(Paging.class)) {
						itemField.setAccessible(true);

						try {
							paging = (Paging) itemField.get(paramObject);
						} catch (IllegalArgumentException e) {
							MiniLog.error("", e);
						} catch (IllegalAccessException e) {
							MiniLog.error("", e);
						}
						break;
					}
				}
			}
		}
		return paging;
	}

	/**
	 * 获取总记录数
	 * 
	 * @param statementHandler
	 * @param connection
	 * @param paging
	 * @return
	 * @throws Throwable
	 */
	private int getTotalRecord(StatementHandler statementHandler, Connection connection, Paging paging)
			throws Throwable {
		int num = 0;

		BoundSql boundSql = statementHandler.getBoundSql();
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		String sql = boundSql.getSql().toLowerCase();

		int fromIdx = -1;
		if (-1 == fromIdx) {
			fromIdx = sql.indexOf("\nfrom\n");
		}
		if (-1 == fromIdx) {
			fromIdx = sql.indexOf(" from ");
		}
		if (-1 == fromIdx) {
			fromIdx = sql.indexOf("\nfrom ");
		}
		if (-1 == fromIdx) {
			fromIdx = sql.indexOf(" from\n");
		}
		if (-1 != fromIdx) {
			sql = "select count(1) " + sql.substring(fromIdx);

			PreparedStatement pstmt = null;
			pstmt = connection.prepareStatement(sql);

			// 通过反射获取delegate父类BaseStatementHandler的mappedStatement属性
			MappedStatement mappedStatement = (MappedStatement) ReflectUtil.getFieldValue(statementHandler,
					"mappedStatement");
			Object paramObject = boundSql.getParameterObject();
			BoundSql countBoundSql = new BoundSql(mappedStatement.getConfiguration(), sql, parameterMappings,
					paramObject);
			ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, paramObject,
					countBoundSql);
			parameterHandler.setParameters(pstmt);

			// 记录SQL
			MiniLog.debug("total record sql:\n" + this.getSql(countBoundSql));

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				num = rs.getInt(1);
				rs.close();
			}
		}
		return num;
	}

	private void paging(StatementHandler statementHandler, Paging paging) throws Throwable {

		BoundSql boundSql = statementHandler.getBoundSql();
		String sql = boundSql.getSql();

		if (paging != null) {

			int gotoPage = paging.getGotoPage();
			int pageSize = paging.getPageSize();

			StringBuffer stbSql = new StringBuffer();
			// ORACLE情况下
			if (this.dataBaseType == DataBaseType.ORACLE) {
				int num1 = (gotoPage - 1) * pageSize + 1;
				int num2 = gotoPage * pageSize;

				stbSql.append("SELECT * FROM (SELECT TEMPTABLE000.*, ROWNUM NO000 FROM (\n");
				stbSql.append(sql);
				stbSql.append("\n) TEMPTABLE000 WHERE ROWNUM <= " + num2 + ") WHERE NO000 >= " + num1);
			}
			// SQLSERVER
			if (this.dataBaseType == DataBaseType.SQLSERVER) {
				int num1 = (gotoPage - 1) * pageSize + 1;
				int num2 = gotoPage * pageSize;

				// 要求sql自身带有ROW_NUMBER()函数，并将该列命名为ROWNUM。
				// 如：ROW_NUMBER() OVER(Order by [CREATE_DATE]
				// DESC,[CREATE_TIME] DESC ) AS ROWNUM
				stbSql.append("SELECT * FROM (\n");
				stbSql.append(sql);
				stbSql.append("\n) RESULT_WITH_ROWNUM WHERE ROWNUM BETWEEN " + num1 + " AND " + num2);
			}
			// MYSQL
			else if (this.dataBaseType == DataBaseType.MYSQL) {
				int num1 = (gotoPage - 1) * pageSize;
				int num2 = pageSize;

				stbSql.append("SELECT * FROM (\n");
				stbSql.append(sql);
				stbSql.append("\n) TEMPTABLE000 LIMIT " + num1 + "," + num2);
			}

			Field sqlField = boundSql.getClass().getDeclaredField("sql");
			sqlField.setAccessible(true);
			sqlField.set(boundSql, stbSql.toString());

			// 记录SQL
			MiniLog.debug("paging sql:\n" + this.getSql(boundSql));
		}
	}

	/**
	 * 获取SQL
	 * 
	 * @param boundSql
	 *            BoundSql对象
	 * @return SQL
	 * @throws Throwable
	 */
	@SuppressWarnings("rawtypes")
	private String getSql(BoundSql boundSql) throws Throwable {
		String sql = boundSql.getSql();
		Object paramObject = boundSql.getParameterObject();

		// 遍历SQL参数
		List<ParameterMapping> pms = boundSql.getParameterMappings();
		for (ParameterMapping pm : pms) {

			String paramName = pm.getProperty(); // SQL参数名称
			Class<?> paramJavaType = pm.getJavaType(); // SQL参数Java类型

			// 从参ParameterObject中获取参数的值
			Object paramValue = null;
			if (null != paramObject) {
				if (this.isBaseType(paramObject)) {
					paramValue = paramObject;
				} else if (paramObject instanceof Map) {
					paramValue = ((Map) paramObject).get(paramName);
				} else {
					Field field = paramObject.getClass().getDeclaredField(paramName);
					field.setAccessible(true);
					paramValue = field.get(paramObject);
				}
			}

			if (null == paramValue) {
				MiniLog.error("the value of sql parameter[" + paramName + "] does not exist.");
			}

			// 替换SQL中参数的占位符
			paramValue = null == paramValue ? "" : paramValue;
			paramValue = paramJavaType.equals(String.class) ? "'" + paramValue + "'" : paramValue;
			sql = sql.replaceFirst("\\?", paramValue.toString());
		}
		return sql;
	}

	/**
	 * 判断是否基本类型
	 * 
	 * @param val
	 *            对象
	 * @return 是否基本类型
	 */
	private boolean isBaseType(Object val) {

		if (val instanceof String || val instanceof Integer || val instanceof Byte || val instanceof Long
				|| val instanceof Double || val instanceof Float || val instanceof Character || val instanceof Short
				|| val instanceof BigDecimal || val instanceof BigInteger || val instanceof Boolean
				|| val instanceof Date) {
			return true;
		}
		return false;
	}
}
