/*
 * Copyright (c) 2016-2017, fastquery.org and/or its affiliates. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * For more information, please see http://www.fastquery.org/.
 * 
 */

package org.fastquery.test;

import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.fastquery.bean.Student;
import org.fastquery.core.Primarykey;
import org.fastquery.core.QueryContext;
import org.fastquery.core.QueryRepository;
import org.fastquery.example.StudentDBService;
import org.fastquery.filter.SkipFilter;
import org.fastquery.service.FQuery;
import org.fastquery.struct.SQLValue;

import static org.hamcrest.Matchers.*;

import org.junit.Rule;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 
 * @author xixifeng (fastquery@126.com)
 */
public class StudentDBServiceTest {

	private static final Logger LOG = LoggerFactory.getLogger(StudentDBServiceTest.class);

	@Rule
	public FastQueryTestRule rule = new FastQueryTestRule();

	private StudentDBService studentDBService = FQuery.getRepository(StudentDBService.class);

	@Test
	public void testNull() {
		assertThat(studentDBService, notNullValue());
		// 断言QueryRepository就是studentDBService的父类
		assertThat(QueryRepository.class.isAssignableFrom(studentDBService.getClass()), is(true));
	}

	// 测试
	// @Query("update student s set s.age=?3,s.name=?2 where s.no=?1")
	// @Modifying
	// int update(String no,String name,int age)
	@Test
	public void update() {
		String no = "9512101";
		String name = "小不点";
		int age = 17;
		int seffot = studentDBService.update(no, name, age);
		assertThat(seffot, is(1));
		assertThat(rule.getSQLValue(), nullValue());
		if (rule.isDebug()) {
			List<SQLValue> sqlValues = rule.getListSQLValue();
			assertThat(sqlValues.size(), is(1));
			SQLValue sqlValue = sqlValues.get(0);
			assertThat(sqlValue.getSql(),
					equalToIgnoringWhiteSpace("update student s set s.age=?,s.name=? where  s.no=?"));
			assertThat(sqlValue.getSql(), equalTo("update student s set s.age=?,s.name=? where  s.no=?"));
			List<Object> values = sqlValue.getValues();
			assertThat(values.size(), is(3));
			assertThat(values.get(0).getClass() == Integer.class && values.get(0).equals(age), is(true));
			assertThat(values.get(1).getClass() == String.class && values.get(1).equals(name), is(true));
			assertThat(values.get(2).getClass() == String.class && values.get(2).equals(no), is(true));
		}
	}

	// @Query("update student s set s.age=?2 where s.no=?1")
	// int update(String no,int age);
	@Test
	public void update2() {
		int i = studentDBService.update("9512101", 17);
		assertThat(i, is(1));
		i = studentDBService.update("9512101XX", 17);
		assertThat(i, is(0));
	}

	// @Query("select no, name, sex from student")
	// JSONArray findAll();
	@Test
	public void findAll() {
		JSONArray jsonArray = studentDBService.findAll();
		assertThat(jsonArray.size(), greaterThan(3));
	}

	@Test
	public void find() {
		Student[] students = studentDBService.find();
		assertThat(students.length, greaterThan(3));
	}

	@Test
	public void findOne() {
		JSONObject student = studentDBService.findOne("9521103");
		assertThat(student.getString("no"), is("9521103"));
		assertThat(student.getString("dept"), is("化学系"));
	}

	@SkipFilter
	@Test
	public void findOne2() {
		QueryContext.setLang("en");
		JSONObject student = studentDBService.findOne("9921103");
		assertThat(student.get("name"), equalTo("Lily"));
		assertThat(student.get("dept"), equalTo("Chinese"));

		QueryContext.setLang("zh");
		student = studentDBService.findOne("9921103");
		assertThat(student.get("name"), equalTo("丽丽"));
		assertThat(student.get("dept"), equalTo("语文"));
	}

	@Test
	public void updateNameAndDept() {
		String name = "{\"zh\":\"丽丽\",\"en\":\"Lily\"}";
		String dept = "{\"zh\":\"语文\",\"en\":\"Chinese\"}";
		String no = "9921103";
		int i = studentDBService.updateNameAndDept(name, dept, no);
		assertThat(i, is(i));
	}

	@Test
	public void findStudent() {
		Student student = studentDBService.findStudent("9521103");
		assertThat(student.getNo(), is("9521103"));
		assertThat(student.getDept(), is("化学系"));
	}

	@Test
	public void findStudent2() {
		Student student = studentDBService.findStudent(null);
		assertThat(student, nullValue());
	}

	@Test
	public void exists() {
		boolean exists = studentDBService.exists("9512101");
		assertThat(exists, is(true));

		exists = studentDBService.exists("9921103xxyy");
		assertThat(exists, is(false));
	}

	@Test
	public void findByNo() {
		boolean exists = studentDBService.exists("1121103");
		if (!exists) {
			studentDBService.add("1121103", "1121103", "女", 82, "无派系");
		}
		Student student = studentDBService.findByNo("1121103");
		assertThat(student.getNo(), is("1121103"));
		assertThat(student.getName(), is("1121103"));
		assertThat(student.getSex(), is("女"));
		assertThat(student.getAge(), is(82));
		assertThat(student.getDept(), is("无派系"));
	}

	@Test
	public void count() {
		long l = studentDBService.count();
		assertThat(l, greaterThan(1l));
	}

	@Test
	public void count2() {
		int i = studentDBService.count2();
		LOG.debug("i=" + i);
		assertThat(i, greaterThan(1));
	}

	@Test
	public void testRows() {
		JSONObject jsonObject = studentDBService.rows();
		LOG.debug(jsonObject.toJSONString());
	}

	// 测试 add 和 delete
	@Test
	public void testAdd() {
		String no = String.valueOf(System.currentTimeMillis()).substring(6);
		// '9513101', '王陵', '男', '23', '计算机系'
		int effect = studentDBService.add(no, "小蚂蚁", "男", 6, "爬行动物");
		assertThat(effect, is(1));

		// 测试删除
		// effect = studentDBService.deleteByNo(no);
		// assertThat(effect, is(1));
	}

	@Test
	public void addStudent() {
		String no = String.valueOf(System.currentTimeMillis()).substring(6);
		Student student = studentDBService.addStudent(no, "蜘蛛", "男", 3, "爬行动物");
		assertThat(student.getNo(), equalTo(no));
		assertThat(student.getName(), equalTo("蜘蛛"));
		assertThat(student.getSex(), equalTo("男"));
		assertThat(student.getAge(), equalTo(3));
		assertThat(student.getDept(), equalTo("爬行动物"));
	}

	@Test
	public void testFindBySex1() {
		Student[] students = studentDBService.findBySex(10, "男");
		int len = students.length;
		if (len >= 3) {
			len = 3;
		}
		// 输出前面三条看看
		for (int i = 0; i < len; i++) {
			LOG.debug(students[i].toString());
		}
	}

	@Test
	public void testFindBySex2() {
		JSONArray students = studentDBService.findBySex("男", 18);
		if (students.size() >= 3) {
			LOG.debug(JSON.toJSONString(students.subList(0, 3), true));
		} else {
			LOG.debug(JSON.toJSONString(students.subList(0, students.size()), true));
		}
		LOG.debug(studentDBService.toString());
	}

	@Test
	public void findBySex3() {
		List<Map<String, Object>> maps = studentDBService.findBySex2("男", 18);
		maps.forEach(m -> {
			assertThat(m.get("sex"), equalTo("男"));
			assertThat(Integer.valueOf(m.get("age").toString()), greaterThan(18));
		});
	}

	@Test
	public void findColumnKey() {
		JSONArray jsonObject = studentDBService.findColumnKey("product", "xk");
		LOG.debug(JSON.toJSONString(jsonObject, true));
		assertThat(jsonObject.size(), is(2));
		// 断言: 第一个主键的列名称是 "pid"
		assertThat(jsonObject.getJSONObject(0).getString("COLUMN_NAME"), equalTo("pid"));
		// 断言: 第二个主键的列名称是 "pid"
		assertThat(jsonObject.getJSONObject(1).getString("COLUMN_NAME"), equalTo("lid"));
	}

	@Test
	public void addUserInfo() {
		Map<String, Object> map = studentDBService.addUserInfo("张三", 36);
		assertThat(map, notNullValue());
		assertThat(map.get("name"), equalTo("张三"));
		assertThat(map.get("age"), equalTo(36));
	}

	@Test
	public void addUserInfo2() {
		Map<String, String> map = studentDBService.addUserInfo2("Lisi", 32);
		assertThat(map, notNullValue());
		assertThat(map.get("name"), equalTo("Lisi"));
		assertThat(map.get("age"), equalTo("32"));
	}

	@Test
	public void saveUserInfo() {
		Primarykey pk = studentDBService.saveUserInfo("李四", 82);
		// 主键
		long id = pk.getPrimarykey();
		// 断言主键大于1
		assertThat(id, greaterThan(1L));
	}

	@Test
	public void saveUserInfo2() {
		JSONObject jsonObject = studentDBService.saveUserInfo2("网五", 31);
		LOG.debug(JSON.toJSONString(jsonObject, true));
		assertThat(jsonObject.getString("name"), equalTo("网五"));
		assertThat(jsonObject.getInteger("age"), equalTo(31));
	}

	@Test
	public void saveUserInfo3() {
		int effect = studentDBService.saveUserInfo3("小丽丽", 8);
		assertThat(effect, is(1));
	}

	@Test
	public void updateUserinfoById() {
		int id = 1;
		Integer age = 16;
		JSONObject jsonObject = studentDBService.updateUserinfoById(16, 1);
		assertThat(jsonObject, notNullValue());
		LOG.debug(JSON.toJSONString(jsonObject, true));
		assertThat(jsonObject.getInteger("id"), is(id));
		assertThat(jsonObject.getInteger("age"), is(age));
	}

	@Test
	public void deleteUserinfoById() {
		boolean b = studentDBService.deleteUserinfoById(0);
		assertThat(b, is(true));
	}

	@Test
	public void findOneCourse() {
		LOG.debug("xxx:" + studentDBService.findOneCourse());
	}

	@Test
	public void findStudentByAge() {
		Integer age = studentDBService.findAgeByStudent();
		LOG.debug("age: " + age);
	}

	@Test
	public void findAllStudent() {
		Student[] students = studentDBService.findAllStudent("9921103", "张", 16, "化学系", "数学系", "无派系", null, null, null);
		// select no, name, sex from Student where no LIKE '9921103' AND name
		// LIKE '张' AND age > 16 OR dept IN ('化学系', '数学系', '无派系') order by age
		// desc
		if (students != null) {
			for (int i = 0; i < students.length; i++) {
				LOG.debug(students[i].toString());
			}
		}
	}

	@Test
	public void findAllStudent2() {
		studentDBService.findAllStudent(null, "张", 16, "化学系", "数学系", "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, 16, "化学系", "数学系", "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, null, "化学系", "数学系", "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, null, null, "数学系", "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, null, null, null, "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, null, null, null, null, null, null, null);
		studentDBService.findAllStudent(null, "张", 16, null, "数学系", "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, 16, "化学系", null, "无派系", null, null, null);
		studentDBService.findAllStudent(null, null, null, "化学系", "数学系", null, null, null, null);
	}

	@Test
	public void findAllStudent3() {
		String name = "小";
		Integer age = 18;
		List<Student> students = studentDBService.findAllStudent("%" + name + "%", age);
		for (Student student : students) {
			assertThat(student.getName(), containsString(name));
			assertThat(student.getAge(), greaterThan(age));
		}

		name = "家";
		age = null;
		students = studentDBService.findAllStudent("%" + name + "%", age);
		for (Student student : students) {
			assertThat(student.getName(), containsString(name));
		}

		age = 30;
		students = studentDBService.findAllStudent(null, age);
		for (Student student : students) {
			assertThat(student.getAge(), greaterThan(age));
		}

		students = studentDBService.findAllStudent(null, null);
		assertThat(students, notNullValue());
		assertThat(students, not(empty()));
		assertThat(students.size(), greaterThan(1));

	}

	@Test
	public void findAllStudent4() {
		List<Student> students = studentDBService.findAllStudent(null, null);
		assertThat(students, not(empty()));
	}

	@Test
	public void findAges() {
		List<Integer> ages = studentDBService.findAges();
		assertThat(ages.size(), greaterThanOrEqualTo(3));
	}

	@Test
	public void findNames() {
		List<String> names = studentDBService.findNames();
		assertThat(names.size(), greaterThanOrEqualTo(3));
		names.forEach(name -> {
			LOG.debug(name);
		});
	}

	@Test
	public void findSomeStudent() {
		List<Student> students = studentDBService.findSomeStudent();
		assertThat(students.size(), is(3));
	}

	@Test
	public void findTop1Student() {
		Map<String, String> map = studentDBService.findTop1Student();
		map.forEach((k, v) -> assertThat(v, is(instanceOf(String.class))));
		JSONObject json = (JSONObject) JSONObject.toJSON(map);
		LOG.debug(json.toJSONString());
	}

}
