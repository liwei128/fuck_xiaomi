package com.fuck.xiaomi.manage;

import java.util.Map;

import com.fuck.xiaomi.pojo.CustomRule;
import com.fuck.xiaomi.pojo.GoodsConfig;
import com.fuck.xiaomi.pojo.GoodsInfo;
import com.fuck.xiaomi.pojo.User;
import com.google.common.collect.Maps;

public class Config {
	
	public static User user;
	
	public static GoodsInfo goodsInfo;
	
	public static CustomRule customRule ;
	
	public static GoodsConfig goodsConfig;
	
	public static Map<String, GoodsConfig> goodsConfigs = Maps.newHashMap();

}
