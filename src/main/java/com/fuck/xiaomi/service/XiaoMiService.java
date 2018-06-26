package com.fuck.xiaomi.service;


import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fuck.xiaomi.annotation.Async;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Retry2;
import com.fuck.xiaomi.annotation.Service;
import com.fuck.xiaomi.annotation.Stop;
import com.fuck.xiaomi.annotation.Timing;
import com.fuck.xiaomi.enums.TimingType;
import com.fuck.xiaomi.manage.FilePathManage;
import com.fuck.xiaomi.manage.MyThreadPool;
import com.fuck.xiaomi.manage.StatusManage;
import com.fuck.xiaomi.manage.Config;
import com.fuck.xiaomi.pojo.Cookie;
import com.fuck.xiaomi.pojo.GoodsConfig;
import com.fuck.xiaomi.pojo.User;
import com.fuck.xiaomi.utils.FileUtil;
import com.google.common.collect.Lists;

/**
 * 小米抢购服务
 * @author liwei
 * @date: 2018年6月11日 下午1:48:31
 *
 */
@Service
public class XiaoMiService {
	
	private static  Logger logger = LoggerFactory.getLogger(XiaoMiService.class);
	
	@Resource
	private HttpService httpService;
	
	private ScheduledFuture<?> buy;
	
	private ScheduledFuture<?> stop;
	
	
	public boolean islogin(){
		if(!FileUtil.isFile(FilePathManage.userConfig)){
			return false;
		}
		String miString = FileUtil.readFileToString(FilePathManage.userConfig);
		if(miString==null||miString.length()==0){
			return false;
		}
		User oldUser = JSON.parseObject(miString, User.class);
		if(oldUser==null){
			return false;
		}
		if(!oldUser.equals(Config.user)){
			return false;
		}
		if(oldUser.getCookies()==null||oldUser.getCookies().size()==0){
			return false;
		}
		boolean islogin = false; 
		for(Cookie cookie : oldUser.getCookies()){
			if("userId".equals(cookie.getName())){
				islogin = true;
			}
			if("JSESSIONID".equals(cookie.getName())){
				return false;
			}
		}
		if(islogin){
			Config.user.setCookies(oldUser.getCookies());
			return true;
		}
		return false;

	}
	
	/**
	 * 保持登录状态
	 */
	@Async
	public void login() {
		if(!islogin()){
			StatusManage.isLogin = false;
			toLogin();
			StatusManage.isLogin = true;
		}else{
			logger.info("用户:{} 已登录。",Config.user.getUserName());
			StatusManage.isLogin = true;
		}
	}
	
	@Retry2(success = "ok")
	public String toLogin() {
		long start = System.currentTimeMillis();
		FileUtil.writeToFile(JSON.toJSONString(Config.user), FilePathManage.userConfig);
		String result = httpService.execute(FilePathManage.loginJs);
		if(result.length()==0){
			logger.info("用户:{} 登录失败，正在重试。时间:{}ms",Config.user.getUserName(),System.currentTimeMillis()-start);
			return "fail";
		}
		if(result.equals("confine")){
			stop("用户被限制登录！");
			return "ok";
		}
		if(result.equals("pwd")){
			stop("用户名或密码错误！");
			return "ok";
		}
		List<Cookie> cookies = JSON.parseArray(result, Cookie.class);
		Config.user.setCookies(cookies);
		FileUtil.writeToFile(JSON.toJSONString(Config.user), FilePathManage.userConfig);
		logger.info("用户:{} 登录成功,时间:{}ms",Config.user.getUserName(),System.currentTimeMillis()-start);
		return "ok";
		
	}
	
	/**
	 * 每3秒开一个线程,去获取购买url
	 */
	@Timing(initialDelay = 0, period = 3, type = TimingType.FIXED_RATE, unit = TimeUnit.SECONDS)
	public void getBuyUrl(){
		if(!StatusManage.isLogin){
			return;
		}
		queryBuyCartLink();
		
	}
	@Async
	public void queryBuyCartLink(){
		long startTime = System.currentTimeMillis();
		String result = httpService.execute(FilePathManage.queryBuyCartLinkJs);
		if(result.startsWith("http")){
			Config.goodsInfo.getBuyUrls().add(result);
			logger.info("已获取购买链接({}):{}ms",Config.goodsInfo.getBuyUrls().size(),System.currentTimeMillis()-startTime);
		}
	}
	@Async
	public void submitOrder() {
		long start = System.currentTimeMillis();
		String result = httpService.execute(FilePathManage.submitOrderJs);
		if(result.length()>0){
			logger.info("{},{}ms",result,System.currentTimeMillis()-start);
		}
	}

	/**
	 * httpClient加入购物车
	 * @param buyUrl
	 * @param cookies
	 */
	@Timing(initialDelay = 0, period = 300, type = TimingType.FIXED_RATE, unit = TimeUnit.MILLISECONDS)
	public void buyGoodsTask() {
		if(StatusManage.isLogin&&Config.goodsInfo.getBuyUrls().size()>0){
			buy(Config.goodsInfo.getBuyUrls(),Config.user.getCookies());
		}
	}
	
	@Async(30)
	public void buy(List<String> buyUrl, List<Cookie> cookies){
		String url = selectOneUrl(buyUrl);
		long start = System.currentTimeMillis();
		String re = httpService.getByCookies(url, cookies);
		if(isBuySuccess(re)){
			logger.info("已加入购物车,{}ms",System.currentTimeMillis()-start);
			submitOrder();
			stop("恭喜！抢购成功，赶紧去购物车付款吧！");
			return;
		}
			
	}
	
	public String selectOneUrl(List<String> buyUrl) {
		Random random = new Random();
		int index = 0;
		if(buyUrl.size()<5){
			index = random.nextInt(buyUrl.size());
		}else{
			index = random.nextInt(5)+buyUrl.size()-5;
		}
		return buyUrl.get(index);
	}

	public void start(){
		//登录
		login();
		//购买
		buy = MyThreadPool.schedule(()->{
			logger.info("获取购买链接中。。。");
			getBuyUrl();
			buyGoodsTask();
			
		}, Config.customRule.getBuyTime(), TimeUnit.MILLISECONDS);
		//抢购时间截止
		stop = MyThreadPool.schedule(()->{
			stop("抢购时间截止，停止抢购");
		}, Config.customRule.getEndTime(), TimeUnit.MILLISECONDS);

	}
	@Stop(methods = { "buyGoodsTask","getBuyUrl"})
	public void stop(String msg) {
		
		StatusManage.endMsg = msg;
		logger.info(msg);
		
		if(buy!=null){
			buy.cancel(false);//停止 购买定时器
		}
		
		if(stop!=null){
			stop.cancel(false);//停止 截止时间的定时器
		}

		StatusManage.isEnd = true;
	}
	
	//判断是否抢购成功 
	//jQuery111302798960934517918_1528978041106({"code":1,"message":"2173300005_0_buy","msg":"2173300005_0_buy"});
	public boolean isBuySuccess(String re) {
		if(re==null){
			return false;
		}
		try{
			String substring = re.substring(re.indexOf("(")+1,re.lastIndexOf(")"));
			JSONObject parseObject = JSON.parseObject(substring);
			Integer code = parseObject.getInteger("code");
			return code==1;
		}catch(Exception e){
			logger.error("parseBuyResult err:{}",re);
			return false;
		}
	}
	
	/**
	 * 查询商品详情
	 * @param url
	 * @return
	 */
	public GoodsConfig queryGoodsInfo(String url){
		try{
			String goodsId = url.substring(url.indexOf("product")+8,url.indexOf("html")-1);
			String goodInfoUrl = "https://order.mi.com/product/get?jsonpcallback=proget2callback&product_id="+goodsId+"&_=1529911856856";
			String ret = httpService.getXiaomi(goodInfoUrl, url);
			return parseGoodsInfo(ret,url);
		}catch(Exception e ){
			logger.error("queryGoodsInfo by:{} err",url,e);
			return null;
		}
		
	}
	/**
	 * 解析商品详情
	 * @param ret
	 * @param url
	 * @return
	 */

	private GoodsConfig parseGoodsInfo(String ret,String url) {
		if(ret==null){
			return null;
		}
		GoodsConfig goodsConfig = new GoodsConfig();
		String substring = ret.substring(ret.indexOf("(")+1,ret.lastIndexOf(")"));
		JSONObject parseObject = JSON.parseObject(substring);
		Integer code = parseObject.getInteger("code");
		if(code!=1){
			logger.error("错误码:{}",code);
			return null;
		}
		JSONObject data = parseObject.getJSONObject("data");
		goodsConfig.setName(data.getString("name"));
		JSONArray list = data.getJSONArray("list");
		List<String> versions = Lists.newArrayList();
		List<String> colors = Lists.newArrayList();
		for(int i =0;i<list.size();i++){
			JSONObject jsonObject = list.getJSONObject(i);
			if(jsonObject.getJSONArray("list")==null){
				String color = jsonObject.getString("value");
				colors.add(color);
			}else{
				String version = jsonObject.getString("value");
				versions.add(version);
				JSONArray jsonArray = jsonObject.getJSONArray("list");
				if(jsonArray.size()>colors.size()){
					colors = Lists.newArrayList();
					for(int j = 0;j<jsonArray.size();j++){
						JSONObject jsonObject1 = jsonArray.getJSONObject(j);
						String color = jsonObject1.getString("value");
						colors.add(color);
					}
				}
			}
		}
		goodsConfig.setUrl(url);
		goodsConfig.setVersion(versions);
		goodsConfig.setColor(colors);
		return goodsConfig;
	}
	/**
	 * 搜索商品
	 * @param name
	 * @return
	 */
	public String searchGoods(String name) {
		if(name.startsWith("http")){
			if(!name.startsWith("https://item.mi.com/product/")){
				return "(不支持该链接)";
			}
			Config.goodsConfig = queryGoodsInfo(name);
			if(Config.goodsConfig==null){
				return "(未找到该商品)";
			}
			Config.goodsConfigs.put(Config.goodsConfig.getName(), Config.goodsConfig);
			FileUtil.writeToFile(JSON.toJSONString(Config.goodsConfigs), FilePathManage.goodsInfoDb);
			return null;
		}
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
		return Config.goodsConfig==null?"(商品暂未收录，试试商品地址)":null;
	}
	/**
	 * 查询商品购买页面url
	 * @param url
	 * @return
	 */
	public String queryBuyPageUrl(String url) {
		try{
			if(url.indexOf("?")!=-1){
				url = url.substring(0, url.indexOf("?"));
			}
			String goUrl = "https://order.mi.com/product/gettabinfo?jsonpcallback=jQuery1113041140715231454394_"
					+System.currentTimeMillis()
					+"&url="+URLEncoder.encode(url,"utf-8")
					+"&_="+System.currentTimeMillis();
			String ret = httpService.getXiaomi(goUrl, url);
			return parseBuyPageUrl(ret);
		}catch(Exception e){
			logger.error("queryBuyPageUrl by:{} err",url,e);
			return null;
		}
	}
	/**
	 * 解析商品购买页面
	 * @param ret
	 * @return
	 */
	public String parseBuyPageUrl(String ret){
		if(ret==null){
			return null;
		}
		ret = ret.substring(ret.indexOf("(")+1, ret.lastIndexOf(")"));
		JSONObject parseObject = JSON.parseObject(ret);
		Integer code = parseObject.getInteger("code");
		if(code!=1){
			logger.error("错误码:{}",code);
			return null;
		}
		String id = parseObject.getJSONObject("data").getJSONObject("link").getString("product_id");
		if(id==null){
			return null;
		}
		return "https://item.mi.com/product/"+id+".html";
	}
	
}
