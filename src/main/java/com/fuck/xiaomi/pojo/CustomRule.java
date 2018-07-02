package com.fuck.xiaomi.pojo;

import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSON;

/**
 * 自定义规则
 * @author liwei
 * @date: 2018年6月8日 下午4:33:48
 *
 */
public class CustomRule {
	
	//抢购时间
	private long buyTime;//提前3.5s,考虑时间误差
	
	//抢购截止时间
	private long endTime;

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	
	public long getBuyTime() {
		return buyTime;
	}

	public void setBuyTime(long buyTime) {
		this.buyTime = buyTime;
	}
	
	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public void builderTime(String startTime,String duration) throws Exception{

		int minute = Integer.parseInt(duration);
		if(minute<=0){
			throw new Exception("抢购时长必须大于0");
		}
		long time = 0L;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try{
			time = simpleDateFormat.parse(startTime).getTime();
		}catch(Exception e){
			throw new Exception("时间格式不正确");
		}
		this.buyTime = time-3500;
		
		if(buyTime-System.currentTimeMillis()<0){
			throw new Exception("貌似错过了抢购时间");
		}
		
		this.endTime = time+minute*60*1000;
	}
	
	public CustomRule(String buyTime, String duration) throws Exception {
		if(buyTime==null||buyTime.length()==0){
			throw new Exception("抢购时间不能为空");
		}
		if(duration ==null||duration.length()==0){
			throw new Exception("抢购时长不能为空");
		}
		builderTime(buyTime, duration);
	}

	public CustomRule() {
		super();
	}
	
	
	
	

}
