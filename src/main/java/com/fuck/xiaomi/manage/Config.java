package com.fuck.xiaomi.manage;

import java.util.concurrent.atomic.AtomicInteger;

import com.fuck.xiaomi.pojo.CustomRule;
import com.fuck.xiaomi.pojo.GoodsInfo;
import com.fuck.xiaomi.pojo.User;

public class Config {
	
	public static User user;
	
	public static GoodsInfo goodsInfo;
	
	public static CustomRule customRule ;
	
	public static AtomicInteger submitCount = new AtomicInteger(0);

}
