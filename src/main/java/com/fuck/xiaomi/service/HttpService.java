package com.fuck.xiaomi.service;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fuck.xiaomi.annotation.Service;
import com.fuck.xiaomi.manage.FilePathManage;
import com.fuck.xiaomi.pojo.Cookie;

/**
 * 网络请求服务
 * @author wei.li
 * @time 2017年12月22日上午10:51:26
 */
@Service
public class HttpService {
	
	private static  Logger logger = LoggerFactory.getLogger(HttpService.class);
	
	
    
	
	/**
	 * 执行phantomjs
	 * @param jsPath
	 * @return
	 */
	public String execute(String jsPath){
		return execute(jsPath, "");
	}
	
	public String execute(String jsPath,String url){
		Process p=null;
		try {
			p= Runtime.getRuntime().exec(FilePathManage.exe+" " +jsPath+" "+url);
			InputStream is = p.getInputStream(); 
			BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8")); 
			StringBuffer sbf = new StringBuffer();  
			String tmp = "";  
			while((tmp = br.readLine())!=null){  
				   sbf.append(tmp); 
			}
			br.close();
			is.close();
			return sbf.toString();
		} catch (Exception e) {
			logger.error("Phantomjs请求异常,js:{}",jsPath);
			return "";
		}finally {
			if(p!=null){
				p.destroy();
			}
		}		
	}
	/**
	 * 携带cookies请求网页
	 * @param url
	 * @param cookies
	 * @return
	 */
	public String getByCookies(String url,List<Cookie> cookies){
		CloseableHttpClient httpClient = createCookiesHttpClient();
    	CloseableHttpResponse response=null;
		try{
    		HttpGet httpGet = new HttpGet(url);
    		httpGet.addHeader(new BasicHeader("Cookie",builderCookiesStr(cookies)));
    		httpGet.setHeader("Connection", "keep-alive");
       		httpGet.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Mobile Safari/537.36");
            response = httpClient.execute(httpGet);
            return toString(response);
            
		}catch(Exception e){
			logger.info("链接:"+url+"异常");
			return null;
    	}finally{
    		closeStream(response);
    	}
	}
	
	private String builderCookiesStr(List<Cookie> cookies) {
		StringBuilder str = new StringBuilder();
		cookies.forEach(o->{
			str.append(o.getName()).append("=").append(o.getValue()).append(";");
		});
		return str.toString();
	}
	/**
	 * 携带cookies的HttpClient
	 * @param cookies
	 * @return
	 */
	private CloseableHttpClient createCookiesHttpClient() {

		RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).setConnectTimeout(10000).setSocketTimeout(10000)  
                       .setConnectionRequestTimeout(10000).build();
        // 设置默认跳转以及存储cookie  
        CloseableHttpClient httpClient = HttpClientBuilder.create()  
                     .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())  
                     .setRedirectStrategy(new DefaultRedirectStrategy()).setDefaultRequestConfig(requestConfig)  
                     .setDefaultCookieStore(new BasicCookieStore()).build(); 
		return httpClient;
	}
	
	
	
	/**
	 * 关闭资源
	 * @param streams      
	 * void
	 */
	public void closeStream(Closeable... streams) {
		for(Closeable stream:streams){
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {
					logger.error("资源关闭异常",e);
				}
			}
			
		}
	}

	public String toString(CloseableHttpResponse httpResponse){  
        // 获取响应消息实体  
    	try{
        	int statusCode = httpResponse.getStatusLine().getStatusCode();
        	if(statusCode!=200){
	        	logger.error("状态值:{}",statusCode); 
        		return null;
        	}
    		HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity,"utf-8");  
    	}catch(Exception e){
    		logger.error("http返回数据转字符出现异常");  
    	}
    	return null;
        
    }  
	
}
