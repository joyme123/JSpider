package com.myway5.www.Spider;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myway5.www.Pool.MultiProcessPagePool;
import com.myway5.www.Urlpool.UrlPool;
import com.myway5.www.Util.HttpSpiderConfig;
import com.myway5.www.Util.Page;


public class HttpSpider {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private static String GET = "get";
	private static String POST = "post";
	private ProcessSpider processSpider = null;
	private Page page = new Page();
	private String method;
	private Map<String,String> data;
	private Map<String,String> cookies;
	private int timeout = 3000;//默认3秒，0代表无时间限制
	private AtomicInteger errorCount = new AtomicInteger(0);
	private String userAgent;
	private UrlPool urlPool = UrlPool.getInstance();
	private MultiProcessPagePool processPagePool = MultiProcessPagePool.getInstance();
	
	
	public void setPrecessSpider(ProcessSpider processSpider){
		this.processSpider = processSpider;
	}
	
	public void setCookies(Map<String,String> cookies){
		this.cookies = cookies;
	}
	
	public void setStartUrl(String url){
		urlPool.push(url);
	}
	
	public void setData(Map<String,String> data){
		this.data = data;
	}
	
	/*
	 * 设置超时时间，默认为三秒，0代表无超时限制
	 * @param int timeout 超时时间，毫秒为单位
	 */
	public void setTimeout(int timeout){
		this.timeout = timeout;
	}
	
	/*
	 * 设置请求方式，默认为GET
	 * @param String method 总共有两种:"get"或者"post"，不区分大小写
	 */
	public void setMethod(String method){
		this.method = method;
	}
	
	public void setUserAgent(String userAgent){
		this.userAgent = userAgent;
	}
	
	public void setConfig(HttpSpiderConfig config){
		this.method = config.getMethod();
		this.data = config.getData();
		this.timeout = config.getTimeout();
		this.cookies = config.getCookies();
		this.userAgent = config.getUserAgent();
	}
	
	public void requestPage(String url){
		Connection con = null;
		try{
			con = Jsoup.connect(url);
			logger.debug("Http爬虫启动---{}",url);
			if(cookies != null){
				con.cookies(cookies);
			}
			if(data != null){
				con.data(data);
			}
			if(userAgent != null){
				con.userAgent(userAgent);
			}
			con.timeout(timeout);
							
			if(method == null || method.equalsIgnoreCase(GET)){
				try {
					Document document = con.get();
					if(document!=null)
						page.setDocument(document);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(method.equalsIgnoreCase(POST)){
				try {
					Document document = con.get();
					page.setDocument(document);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				logger.error("不支持的请求类型");
			}
			page.setUrl(url);
			//如果设置了processSpider，则以普通的模式启动
			if(processSpider != null){
				processSpider.process(page);
			}else{
				//否则将page保存到page pool里
				processPagePool.push(page);
			}
			logger.trace("httpSpider启动");
		}catch(IllegalArgumentException e){		//URL无效异常
			errorCount.incrementAndGet();
			logger.debug("无效或失败链接地址{}",errorCount.get());
		}
	}
}
