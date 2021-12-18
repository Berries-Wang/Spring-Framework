package com.imooc.services.impl;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TransactionalServiceImpl {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public String testTrans() throws Exception {
		Map<String, Object> queryForMap = jdbcTemplate.queryForMap("select * from people;");
		if (null == queryForMap || queryForMap.size() == 0) {
			System.out.println("暂无数据");
			return "";
		}
		for (Map.Entry<String, Object> entry : queryForMap.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
		// 事务嵌套，测试事务的传播行为
		((TransactionalServiceImpl) AopContext.currentProxy()).testTransRollBack();
		return "testTrans";
	}

	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public String testTransRollBack() throws Exception {
		jdbcTemplate.execute("insert into people(name,sex) values('RollBack',1);");

		List<Map<String, Object>> queryList = jdbcTemplate.queryForList("select * from people;");
		if (null == queryList || queryList.size() == 0) {
			System.out.println("暂无数据");
			return "";
		}
		for (Map<String, Object> goal : queryList) {
			for (Map.Entry<String, Object> entry : goal.entrySet()) {
				System.out.println(entry.getKey() + " <:> " + entry.getValue());
			}
		}


		throw new Exception("测试事务回滚");
	}

}
