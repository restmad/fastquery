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

package org.fastquery.core;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;
import org.fastquery.filter.FilterChainHandler;
import org.fastquery.mapper.QueryPool;
import org.fastquery.page.Page;
import org.fastquery.util.FastQueryJSONObject;
import org.fastquery.util.TypeUtil;

/**
 * 
 * @author xixifeng (fastquery@126.com)
 */
public class Prepared {
	
	private static final Logger LOG = Logger.getLogger(Prepared.class);
	
	private static ThreadLocal<ClassLoader> clsloadThread = new ThreadLocal<ClassLoader>(){
		@Override
        public ClassLoader initialValue() {  
            return this.getClass().getClassLoader();
        }  
    }; 
    
	private Prepared(){}
  
	/**
	 * 执行方法
	 * @param interfaceClazz 接口clazz
	 * @param methodName 方法名称
	 * @param methodDescriptor 方法完整描述(asm)
	 * @param args 方法参数 注意: 此处参数列表的成员,永远都是包装类型(已经验证)
	 * @param target 目标 Repository
	 * @return 执行之后的值
	 */
	public static Object excute(String interfaceClazz, String methodName,String methodDescriptor,Object[] args,Repository target) {
		try {
			final Class<? extends Repository> iclazz = getInterfaceClass(interfaceClazz);
			final Method method = TypeUtil.getMethod(iclazz, methodName,methodDescriptor);
			// return businessProcess(iclazz,method, args)
			
	        // 如果是调试模式
	        if(FastQueryJSONObject.getDebug()){
	        	QueryPool.reset(iclazz.getName());
	        }
	        
			// 在businessProcess的先后加拦截器 ==================
			// 注入BeforeFilter
	        Object object = FilterChainHandler.bindBeforeFilterChain(iclazz,target,method,args);
	        if(object!=void.class){
	                return object;
	        }

	        // QueryContext 生命开始
	        QueryContext.getQueryContext().setMethod(method);
	        LOG.info("准备执行方法:" + method);
	        // 取出当前线程中method和args(BeforeFilter 有可能中途修换其他method, 因为过滤器有个功能this.change(..,...) )
	        object = businessProcess(iclazz,method, args);

	        // 注入AfterFilter
	        object = FilterChainHandler.bindAfterFilterChain(iclazz,target,method,args,object); // 注意,这个方法的method,必须是原始的!!!
	        // 在businessProcess的先后加拦截器 ================== End
	        
	        return object;	
		} catch (Exception e) {
			
			QueryContext context = QueryContext.getQueryContext();
			
			StringBuilder sb = new StringBuilder();
			String msg = e.getMessage();
			if(msg!=null) {
				sb.append(msg);
			}
			
			sb.append('\n');
			sb.append("发生方法:" + context.getMethod());
			sb.append('\n');
			sb.append("执行过的sql:");
			
			List<String> sqls = context.getSqls();
			sqls.forEach(sql -> {
				sb.append(sql);
				sb.append('\n');
			});
			LOG.error(sb.toString(),e);
			throw new RepositoryException(e);
		} finally {
	        // QueryContext 生命终止
	        QueryContext.getQueryContext().clear();
		}
	}
	
	private static Object businessProcess(Class<? extends Repository> iclazz,Method method,Object...args) {
		
		Class<?> returnType = method.getReturnType();
		//String packageName = iclazz.getPackage().getName()
		String packageName = iclazz.getName();
		// 目前只有一种可能:Query Interface
		// 在这里是一个分水岭
		if(QueryRepository.class.isAssignableFrom(iclazz)){ // 判断iclazz 是否就是QueryRepository.class,或是其子类
			// QueryRepository 中的方法可分成4类
			// 1. 同时包含有@Query和@Modify
			// 2. 只包含@Query
			// 3. 只包含@Modify 这是不允许的, 该检测已放在生成类之前做了.
			// 4. 没有Query,也没有@Modify
			Query[] querys = method.getAnnotationsByType(Query.class);
			Modifying modifying = method.getAnnotation(Modifying.class);
			QueryByNamed queryById = method.getAnnotation(QueryByNamed.class);
			if( (querys.length>0 || queryById!=null) && modifying !=null) {  // ->进入Modify
				return QueryProcess.getInstance().modifying(method,returnType, TypeUtil.getQuerySQL(method, querys, args), packageName,args);
			} else if(querys.length>0  || queryById!=null) {
				
				if(returnType == Page.class && queryById != null) {  // ->进入Page
					String query = QueryPool.render(iclazz.getName(), method,true,args);
					String countQuery = QueryPool.render(iclazz.getName(), method,false,args);
					return QueryProcess.getInstance().queryPage(method, query, countQuery,packageName, args);
				}
				
				
				if(returnType == Page.class) {  // ->进入Page
					return QueryProcess.getInstance().queryPage(method,querys,packageName, args);
				}
				
				// -> 进入query
				// 获取sql
				String sql = TypeUtil.getQuerySQL(method, querys, args).get(0);
				// 获取数据源的名称
				String sourceName = TypeUtil.findSource(method.getParameters(), args);
				return QueryProcess.getInstance().query(method,returnType, sql,sourceName, packageName,null,args); // new Object[0] 这个null暂时不能省
			} 
			 else {
				 return QueryProcess.getInstance().methodQuery(method,packageName, args);
			}
			
		} else {
			throw new RepositoryException("不能识别的Repository");
		}
	}
	

	/**
	 * 获得clazz
	 * @param interfaceClazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Class<? extends Repository> getInterfaceClass(String interfaceClazz){
			Class<? extends Repository> clazz = null;
			ClassLoader classLoader = clsloadThread.get(); // 有默认值,因此 classLoader 不为null
			try {
				/**
				if(classLoader==null) {
					clazz = (Class<? extends Repository>) Class.forName(interfaceClazz);	
				} else {
					clazz = (Class<? extends Repository>) classLoader.loadClass(interfaceClazz);
				}*/
				clazz = (Class<? extends Repository>) classLoader.loadClass(interfaceClazz);
			} catch (ClassNotFoundException e) {
				throw new RepositoryException(e.getMessage(),e);
			}
			return clazz;
	}
	
	public static ClassLoader getClassLoader(){
		return clsloadThread.get();
	}
	
	public static void setClassLoader(ClassLoader classLoader){
		clsloadThread.set(classLoader);
	}
	
}
