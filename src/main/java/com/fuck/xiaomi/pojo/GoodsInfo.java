package com.fuck.xiaomi.pojo;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;

/**
 * 要购买的商品
 * @author liwei
 * @date: 2018年6月8日 下午4:34:00
 *
 */
public class GoodsInfo {
	
	//购买或抢购页面地址
	private String url;
	
	//版本
	private String version;
	
	//颜色
	private String color;
	
	//抢购链接
	private List<String> buyUrls = Lists.newArrayList();
	
	

	public List<String> getBuyUrls() {
		return buyUrls;
	}

	public void setBuyUrls(List<String> buyUrls) {
		this.buyUrls = buyUrls;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}

	public GoodsInfo(String url, String version, String color) throws Exception {
		super();
		if(url==null||url.length()==0){
			throw new Exception("链接地址不能为空");
		}
		this.url = url;
		if(!version.equals("默认")){
			this.version = version;
		}
		if(!color.equals("默认")){
			this.color = color;
		}
	}

	public GoodsInfo() {
		super();
	}
	
	
	
	
	
	
	

}
