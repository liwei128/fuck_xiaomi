package com.fuck.xiaomi.manage;

import java.io.File;
/**
 * 配置文件目录
 * @author wei.li
 * @time 2017年7月6日下午5:53:28
 */
public class FilePathManage {
	
	public static String binPath=System.getProperty("user.dir")+File.separator+"bin";
	
	public static String configPath=System.getProperty("user.dir")+File.separator+"config";
	
	
	
	public static String exe = binPath + File.separator  + "phantomjs.exe"; 
	
	public static String userConfig = configPath + File.separator + "user.json";
	
	public static String goodsInfoConfig= configPath + File.separator + "goodsInfo.json";
	
	public static String goodsInfoDb= binPath + File.separator + "goodsInfo.db";
	
	
	
	public static String loginJs = binPath + File.separator + "login.js";
	
	public static String queryBuyCartLinkJs = binPath + File.separator + "queryBuyCartLink.js";
	
	public static String submitOrderJs = binPath + File.separator + "submitOrder.js";
	
	public static String queryHomeGoodsJs = binPath + File.separator + "queryHomeGoods.js";
	
	
	
}
