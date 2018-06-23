package com.fuck.xiaomi.controller;


import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fuck.xiaomi.annotation.Async;
import com.fuck.xiaomi.annotation.Controller;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Singleton;
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
		Config.goodsConfigs = JSON.parseObject(string,new TypeReference<Map<String,GoodsConfig>>(){});
		
	}

	public String loadLog() {
		return LogStorage.getLog();
	}

	public void stop(String msg) {
		xiaomiService.stop(msg);
		
	}

	public void searchGoods(String name) {
		GoodsConfig goodsConfig = Config.goodsConfigs.get(name);
		if(goodsConfig==null){
			Set<String> keySet = Config.goodsConfigs.keySet();
			for(String key : keySet){
				if(key.contains(name)||name.contains(key)){
					goodsConfig = Config.goodsConfigs.get(key);
					break;
				}
			}
		}
		Config.goodsConfig = goodsConfig;
		
	}
}
