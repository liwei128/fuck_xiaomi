package com.fuck.xiaomi.service;
/**
 * 查询商品信息
 * @author LW
 * @time 2018年6月23日 下午2:41:53
 */

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fuck.xiaomi.annotation.Async;
import com.fuck.xiaomi.annotation.Resource;
import com.fuck.xiaomi.annotation.Service;
import com.fuck.xiaomi.db.GoodsInfoStorage;
import com.fuck.xiaomi.manage.FilePathManage;
import com.fuck.xiaomi.pojo.GoodsConfig;
import com.fuck.xiaomi.utils.FileUtil;
import com.google.common.collect.Lists;

@Service
public class QueryGoodsService {
	
	private static  Logger logger = LoggerFactory.getLogger(QueryGoodsService.class);
	
	@Resource
	private HttpService httpService;
	@Resource
	private XiaoMiService xiaoMiService;
	
	private List<String> goodsHomeUrl = Lists.newArrayList();
	
	private List<String> buyPageUrls = Lists.newArrayList();
	
	/**
	 * 查询所有商品主页
	 */
	public void queryHomePage() {
		logger.info("主页商品获取开始");
		long startTime = System.currentTimeMillis();
		try{
			String ret = httpService.execute(FilePathManage.queryHomeGoodsJs);
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
					buyPageUrls.add(u);
				}
			});
		}catch(Exception e){
			logger.error("查询商品列表失败",e);
		}
		logger.info("主页商品获取完成,time:{}ms",System.currentTimeMillis()-startTime);
	}
	/**
	 * 查询商品购买页面
	 * @throws InterruptedException 
	 */
	public void queryBuyPageUrl() throws InterruptedException{
		long startTime = System.currentTimeMillis();
		CountDownLatch buyUrlCount = new CountDownLatch(goodsHomeUrl.size()); 
		Semaphore semaphore = new Semaphore(15);
		logger.info("商品购买页面获取开始");
		goodsHomeUrl.forEach(o->{
			semaphore.acquireUninterruptibly();
			queryBuyPageUrl(o,buyUrlCount);
			semaphore.release();
		});
		buyUrlCount.await();
		logger.info("商品购买页面获取完成,数量:{},时间:{}ms",buyPageUrls.size(),System.currentTimeMillis()-startTime);
	}
	@Async
	public void queryBuyPageUrl(String url,CountDownLatch buyUrlCount) {
		String buyPageUrl = xiaoMiService.queryBuyPageUrl(url);
		if(buyPageUrl==null){
			logger.error("queryBuyPageUrl fail:{}",url);
		}else{
			buyPageUrls.add(buyPageUrl);
		}
		buyUrlCount.countDown();

	}
	/**
	 * 查询商品详情
	 * @throws InterruptedException 
	 */
	public void queryGoodsInfo() throws InterruptedException{
		CountDownLatch goodsInfoCount = new CountDownLatch(buyPageUrls.size());
		Semaphore semaphore = new Semaphore(15);
		logger.info("商品详情获取开始");
		buyPageUrls.forEach(o->{
			semaphore.acquireUninterruptibly();
			queryGoodsInfo(o,goodsInfoCount);
			semaphore.release();
		});
		goodsInfoCount.await();
		logger.info("商品详情获取完成");
	}
	@Async
	public void queryGoodsInfo(String url,CountDownLatch goodsInfoCount) {
		long startTime = System.currentTimeMillis();
		GoodsConfig queryGoodsInfo = xiaoMiService.queryGoodsInfo(url);
		if(queryGoodsInfo!=null){
			logger.info("{},time:{}ms",queryGoodsInfo.getName(),System.currentTimeMillis()-startTime);
			GoodsInfoStorage.put(queryGoodsInfo.getName(), queryGoodsInfo);
		}else{
			logger.error("queryGoodsInfo fail:{}",url);
		}
		goodsInfoCount.countDown();
	}
	
	
	public void execute() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		queryHomePage();
		queryBuyPageUrl();
		queryGoodsInfo();
		FileUtil.writeToFile(JSON.toJSONString(GoodsInfoStorage.getAll()), FilePathManage.goodsInfoDb);
		logger.info("耗时:{}ms",System.currentTimeMillis()-startTime);
		System.exit(0);
	}

}
