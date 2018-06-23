package com.fuck.xiaomi.service;


import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import com.fuck.xiaomi.pojo.User;
import com.fuck.xiaomi.utils.FileUtil;

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
		getGoodsLink();
		
	}
	@Async
	public void getGoodsLink(){
		long startTime = System.currentTimeMillis();
		String result = httpService.execute(FilePathManage.getGoodsLinkJs);
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
	
}
