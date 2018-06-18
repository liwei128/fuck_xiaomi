package com.fuck.xiaomi.manage;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fuck.xiaomi.annotation.Controller;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Service;
import com.fuck.xiaomi.interceptor.TaskInterceptor;
import com.google.common.collect.Maps;
import net.sf.cglib.proxy.Enhancer;

/**
 * 任务工厂
 * 可实现依赖注入
 * @author LW
 * @time 2017年12月22日 下午10:01:48
 */
public class ServiceFactory {	
	
	private static  Logger logger = LoggerFactory.getLogger(ServiceFactory.class);
	
	private static String packageName = "com.fuck.xiaomi";
	
	private static Map<String ,Object> serviceMap;

	private static void initService() {
		serviceMap = Maps.newHashMap();
		Reflections reflections = new Reflections(packageName);
		Set<Class<?>> serviceClasss = reflections.getTypesAnnotatedWith(Service.class);
		Set<Class<?>> controllerClasss = reflections.getTypesAnnotatedWith(Controller.class);
		serviceClasss.addAll(controllerClasss);
		for(Class<?> clazz:serviceClasss){
			createInstance(clazz); 
		}
	}


	private static void createInstance(Class<?> clazz) {
		if(clazz.getAnnotation(Service.class)==null&&clazz.getAnnotation(Controller.class)==null){
			return ;
		}
		if(serviceMap.get(clazz.getName())!=null){
			return ;
		}
		try{
			Enhancer enhancer=new Enhancer();  
			enhancer.setSuperclass(clazz);  
			enhancer.setCallback(new TaskInterceptor(clazz));  
			Object create = enhancer.create();
			Field[] fields = clazz.getDeclaredFields();
			for(Field field :fields){
				if(field.getAnnotation(Resource.class) != null){
					field.setAccessible(true);
					if(serviceMap.get(field.getType().getName())==null){
						createInstance(field.getType());
					}
					Object object = serviceMap.get(field.getType().getName());
					field.set(create, object);
				}
			}
			serviceMap.put(clazz.getName(), create);
			logger.info("{} 创建成功",clazz.getName());
		}catch(Exception e){
			logger.error("{} 创建失败",clazz.getName(),e);
		}
	}


	@SuppressWarnings("unchecked")
	public static <T> T getService(Class<T> clazz) {
		synchronized (packageName) {
			if(serviceMap == null){
				initService();
			}
		}
		return (T) serviceMap.get(clazz.getName());
	}
	
	

}
