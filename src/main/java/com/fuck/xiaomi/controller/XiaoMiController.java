package com.fuck.xiaomi.controller;


import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fuck.xiaomi.annotation.Async;
import com.fuck.xiaomi.annotation.Controller;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Singleton;
import com.fuck.xiaomi.db.GoodsInfoStorage;
import com.fuck.xiaomi.db.LogStorage;
import com.fuck.xiaomi.manage.FilePathManage;
import com.fuck.xiaomi.manage.StatusManage;
import com.fuck.xiaomi.pojo.GoodsConfig;
import com.fuck.xiaomi.manage.Config;
import com.fuck.xiaomi.service.LogService;
import com.fuck.xiaomi.service.XiaoMiService;
import com.fuck.xiaomi.utils.FileUtil;

/**
 * 抢购小米
 * @author liwei
 * @date: 2018年6月11日 上午11:54:32
 *
 */
@Controller
public class XiaoMiController {
	
	@Resource
	private XiaoMiService xiaomiService;
	
	
	@Resource
	private LogService logService;
	
	
	public void start(){
		
		StatusManage.isLogin = false;
		StatusManage.submitCount.set(0);
		StatusManage.cartCount.set(0);
		Config.goodsInfo.getBuyUrls().clear();
		
		FileUtil.checkPath(FilePathManage.configPath);
		FileUtil.writeToFile(JSON.toJSONString(Config.goodsInfo), FilePathManage.goodsInfoConfig);
		xiaomiService.start();
		
		
	}
	
	@Singleton
	@Async
	public void init() {
		logService.readLogs();
		String string = FileUtil.readFileToString(FilePathManage.goodsInfoDb);
		if(string.length()!=0){
			GoodsInfoStorage.putAll(JSON.parseObject(string,new TypeReference<Map<String,GoodsConfig>>(){}));
		}
		
		
	}

	public String loadLog() {
		return LogStorage.getLog();
	}

	public void stop(String msg) {
		xiaomiService.stop(msg);
		
	}

	public String searchGoods(String name) {
		return xiaomiService.searchGoods(name);
		
	}
}
