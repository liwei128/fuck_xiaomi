package com.fuck.xiaomi.pojo;

import java.util.List;

import com.alibaba.fastjson.JSON;

/**
 * 商品的颜色、版本集合
 * @author liwei
 * @date: 2018年6月22日 下午6:10:33
 *
 */
public class GoodsConfig {
	
	private String url;
	
	private List<String> version;
	
	private List<String> color;
	
	private String name;
	

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getVersion() {
		return version;
	}

	public void setVersion(List<String> version) {
		this.version = version;
	}

	public List<String> getColor() {
		return color;
	}

	public void setColor(List<String> color) {
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	

}
