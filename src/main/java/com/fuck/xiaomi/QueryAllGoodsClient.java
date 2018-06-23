package com.fuck.xiaomi;

import com.fuck.xiaomi.manage.ServiceFactory;
import com.fuck.xiaomi.service.QueryGoodsService;

/**
 * 抓取所有商品信息
 * @author LW
 * @time 2018年6月23日 下午2:41:02
 */
public class QueryAllGoodsClient {
	
	public static void main(String[] args) throws InterruptedException {
		QueryGoodsService queryGoodsService = ServiceFactory.getService(QueryGoodsService.class);
		queryGoodsService.execute();

	}


}
