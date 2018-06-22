package com.fuck.xiaomi.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fuck.xiaomi.annotation.Controller;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Singleton;
import com.fuck.xiaomi.db.LogStorage;
import com.fuck.xiaomi.manage.FilePathManage;
import com.fuck.xiaomi.manage.StatusManage;
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
	public void init() {
		logService.readLogs();
	}

	public String loadLog() {
		return LogStorage.getLog();
	}
}
