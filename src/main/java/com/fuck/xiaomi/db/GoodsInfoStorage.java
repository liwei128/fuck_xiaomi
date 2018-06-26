package com.fuck.xiaomi.db;

import java.util.Map;
import java.util.Set;

import com.fuck.xiaomi.pojo.GoodsConfig;
import com.google.common.collect.Maps;

/**
 * 商品仓库
 * @author liwei
 * @date: 2018年6月26日 下午4:58:25
 *
 */
public class GoodsInfoStorage {
	
	private static Map<String, GoodsConfig> goodsConfigs = Maps.newHashMap();

	public static Map<String, GoodsConfig> getAll() {
		return goodsConfigs;
	}

	public static void putAll(Map<String, GoodsConfig> goodsConfigs) {
		GoodsInfoStorage.goodsConfigs.putAll(goodsConfigs);;
	}
	
	public static void put(String name , GoodsConfig goodsConfig){
		goodsConfigs.put(name, goodsConfig);
	}

	public static GoodsConfig get(String name) {
		GoodsConfig goodsConfig = goodsConfigs.get(name);
		if(goodsConfig==null){
			Set<String> keySet = goodsConfigs.keySet();
			for(String key : keySet){
				if(key.contains(name)||name.contains(key)){
					goodsConfig = goodsConfigs.get(key);
					break;
				}
			}
		}
		return goodsConfig;
	}

}
