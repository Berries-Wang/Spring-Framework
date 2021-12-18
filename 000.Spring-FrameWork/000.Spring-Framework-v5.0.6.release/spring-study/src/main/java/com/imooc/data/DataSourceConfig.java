package com.imooc.data;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
public class DataSourceConfig {
	/**
	 * 初始化一个数据源并注入到IOC中
	 */
	@Bean(name = "dataSource")
	public DataSource createDataSource()  {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/spring?characterEncoding=UTF-8");
		dataSource.setUsername("root");
		dataSource.setPassword("123456");
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setDbType("com.alibaba.druid.pool.DruidDataSource");
		dataSource.setInitialSize(5);
		dataSource.setMinIdle(5);
		dataSource.setMaxActive(500);
		dataSource.setMaxWait(60000);
		dataSource.setTimeBetweenEvictionRunsMillis(60000);
		dataSource.setMinEvictableIdleTimeMillis(300000);
		dataSource.setValidationQuery("SELECT version()");
		dataSource.setTestWhileIdle(true);
		dataSource.setTestOnBorrow(true);
		dataSource.setTestOnReturn(true);
		dataSource.setPoolPreparedStatements(true);
		// dataSource.setFilters("stat,wall,log4j");
		dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
		dataSource.setUseGlobalDataSourceStat(true);
		Properties commProperties = new Properties();

		commProperties.setProperty("druid.stat.mergeSq", "true");
		commProperties.setProperty("druid.stat.slowSqlMillis", "500");
		dataSource.setConnectProperties(commProperties);

		return dataSource;
	}

	/**
	 * 创建事务管理器
	 */
	@Bean(name = "transactionManager")
	@Autowired
	public DataSourceTransactionManager createTransactionManager(DataSource dataSource) {
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		transactionManager.setDataSource(dataSource);
		return transactionManager;
	}

	/**
	 * 创建JDBCTemplate，操作数据库
	 */
	@Bean(name = "jdbcTemplate")
	@Autowired
	public JdbcTemplate createJDBCTemplate(DataSource dataSource) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return jdbcTemplate;
	}
}
