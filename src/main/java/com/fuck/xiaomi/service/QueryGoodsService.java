package com.fuck.xiaomi.service;
/**
 * 查询商品信息
 * @author LW
 * @time 2018年6月23日 下午2:41:53
 */

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fuck.xiaomi.annotation.Async;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Service;
import com.fuck.xiaomi.manage.FilePathManage;
import com.fuck.xiaomi.pojo.GoodsConfig;
import com.fuck.xiaomi.utils.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Service
public class QueryGoodsService {
	
	private static  Logger logger = LoggerFactory.getLogger(QueryGoodsService.class);
	
	@Resource
	private HttpService httpService;
	
	private List<String> goodsHomeUrl = Lists.newArrayList();
	
	private List<String> goodsBuyUrl = Lists.newArrayList();
	
	private List<String> goodsHomeUrlFail = Lists.newArrayList();
	
	private List<String> goodsBuyUrlFail = Lists.newArrayList();
	
	private Map<String,GoodsConfig> goodsInfos = Maps.newHashMap();
	
	private CountDownLatch buyUrlCount;
	
	private CountDownLatch goodsInfoCount;
	/**
	 * 查询所有商品主页
	 */
	public void queryHomePage() {
		logger.info("主页商品获取开始");
		try{
			String ret = httpService.execute(FilePathManage.queryGoodsListJs);
			if(ret.length()==0){
				logger.error("查询商品列表失败");
				return;
			}
			List<String> urlList = JSON.parseArray(ret, String.class);
			if(urlList==null||urlList.size()==0){
				logger.error("查询商品列表失败");
				return;
			}
			List<String> collect = urlList.stream().map(u->{
				if(u.startsWith("//")){
					return "https:"+u;
				}
				return u;
			}).filter(u->{
				return u.startsWith("https://www.mi.com/")||u.startsWith("https://item.mi.com/product/");
			}).collect(Collectors.toList());
			collect.forEach(u->{
				if(u.startsWith("https://www.mi.com/")){
					goodsHomeUrl.add(u);
				}
				if(u.startsWith("https://item.mi.com/product/")){
					goodsBuyUrl.add(u);
				}
			});
		}catch(Exception e){
			logger.error("查询商品列表失败",e);
		}
		logger.info("主页商品获取完成");
	}
	/**
	 * 查询商品购买页面
	 * @throws InterruptedException 
	 */
	public void queryBuyUrl() throws InterruptedException{
		buyUrlCount = new CountDownLatch(goodsHomeUrl.size());  
		logger.info("商品购买页面获取开始");
		for(int i =0;i<goodsHomeUrl.size();i++){
			if(i%5==0&&i!=0){
				Thread.sleep(5000);
			}
			getBuyUrl(goodsHomeUrl.get(i));
		}
		buyUrlCount.await();
		logger.info("商品购买页面获取完成");
	}
	@Async
	public void getBuyUrl(String url) {
		try{
			String ret = httpService.execute(FilePathManage.queryBuyUrlJs, url);
			if(ret.startsWith("//")){
				ret = "https:"+ret;
			}
			if(ret.startsWith("https://item.mi.com/product/")){
				goodsBuyUrl.add(ret);
			}else{
				goodsHomeUrlFail.add(url);
			}
		}catch(Exception e){
			goodsHomeUrlFail.add(url);
		}finally {
			buyUrlCount.countDown();
		}
		
	}
	/**
	 * 查询商品详情
	 * @throws InterruptedException 
	 */
	public void queryGoodsInfo() throws InterruptedException{
		goodsInfoCount = new CountDownLatch(goodsBuyUrl.size());
		logger.info("商品详情获取开始");
		for(int i =0;i<goodsBuyUrl.size();i++){
			if(i%5==0&&i!=0){
				Thread.sleep(5000);
			}
			parseUrl(goodsBuyUrl.get(i));
			
		}
		goodsInfoCount.await();
		logger.info("商品详情获取完成");
	}
	@Async
	public void parseUrl(String url) {
		String ret = "";
		try{
			ret = httpService.execute(FilePathManage.queryGoodsInfoJs, url);
			GoodsConfig goodsConfig = JSON.parseObject(ret, GoodsConfig.class);
			if(goodsConfig.getColor().size()==0){
				goodsBuyUrlFail.add(url);
				return;
			}
			goodsInfos.put(goodsConfig.getName(), goodsConfig);
		}catch(Exception e){
			goodsBuyUrlFail.add(url);
		}finally {
			goodsInfoCount.countDown();
		}
	}
	
	public void endCheck(){
		logger.info("结束时,检查失败情况");
		goodsHomeUrlFail.forEach(u->{
			String ret = "";
			try{
				ret = httpService.execute(FilePathManage.queryBuyUrlJs, u);
				if(ret.startsWith("//")){
					ret = "https:"+ret;
				}
				if(ret.startsWith("https://item.mi.com/product/")){
					goodsBuyUrlFail.add(ret);
				}else{
					logger.error("无法获取购买页面：{},{}",u,ret);
				}
			}catch(Exception e){
				logger.error("无法获取购买页面：{},{}",u,ret,e);
			}
		});
		goodsBuyUrlFail.forEach(u->{
			String ret = "";
			try{
				ret = httpService.execute(FilePathManage.queryGoodsInfoJs, u);
				GoodsConfig goodsConfig = JSON.parseObject(ret, GoodsConfig.class);
				if(goodsConfig.getColor().size()==0){
					logger.error("无法获商品详情：{},{}",u,ret);
					return;
				}
				goodsInfos.put(goodsConfig.getName(), goodsConfig);
			}catch(Exception e){
				logger.error("无法获商品详情：{},{}",u,ret,e);
			}
		});
		FileUtil.writeToFile(JSON.toJSONString(goodsInfos), FilePathManage.goodsInfoDb);
		logger.info("任务完成，失败情况请手动处理");
	}
	
	public void execute() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		queryHomePage();
		queryBuyUrl();
		queryGoodsInfo();
		endCheck();
		logger.info("耗时:{}ms",System.currentTimeMillis()-startTime);
	}

}
