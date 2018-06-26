package com.fuck.xiaomi.view;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.fuck.xiaomi.controller.XiaoMiController;
import com.fuck.xiaomi.manage.ServiceFactory;
import com.fuck.xiaomi.manage.StatusManage;
import com.fuck.xiaomi.manage.Config;
import com.fuck.xiaomi.pojo.CustomRule;
import com.fuck.xiaomi.pojo.GoodsInfo;
import com.fuck.xiaomi.pojo.User;
/**
 * 小米抢购服务
 * @author liwei
 * @date: 2018年6月12日 下午3:49:23
 *
 */
public abstract class AbstractXiaoMiFunction {
		//小米主服务
		private static  XiaoMiController xiaoMiController = ServiceFactory.getService(XiaoMiController.class);
		
		public static AtomicInteger parseCount = new AtomicInteger(0);
		/**
		 * 窗体控件
		 */
		public abstract Display getDisplay();
		public abstract Shell getShell();
		public abstract Button getHideButton();
		public abstract Text getLogText();
		public abstract Button getStartButton();
		public abstract Button getPauseButton();
		public abstract Button getQuitButton();
		public abstract Button getParseButton();
		
		public abstract Text getNameText();
		public abstract Text getBuyTimeText();
		public abstract Text getDurationText();
		public abstract Text getUserText();
		public abstract Text getPasswordText();
		
		public abstract Combo getOption1();
		public abstract Combo getOption2();
		
		
		//日志显示按钮
		public SelectionAdapter getHideFunction(){
			return new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if(getLogText().isVisible()){
						getLogText().setVisible(false);
						getShell().setSize(650, 375);
						getHideButton().setText("显示日志");
					}else{
						getShell().setSize(650, 720);
						getLogText().setVisible(true);
						getHideButton().setText("隐藏日志");
						
					}
				}
			};
		}
		
		//退出按钮
		public SelectionAdapter getQuitFunction(){
			return new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if(verifyMsg("确认要退出吗")==SWT.YES){
						System.exit(0);
					}
				}
			};
		}
		
		//窗体关闭监控
		public ShellAdapter getCloseListener(){
			return new ShellAdapter() {
				@Override
		        public void shellClosed(ShellEvent e) {
		        	if(e.doit=verifyMsg("确认要退出吗")==SWT.YES){
						System.exit(0);
					}
					
		        }
			};
		}
		
		//开始按钮
		public SelectionAdapter getStartFunction(){
			return new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					try {
						//读取用户界面配置
						readParameter(); 
						//修改状态为爬取开始
						modifyStatus(false);
						//开始抢购
						xiaoMiController.start();
						
					} catch (Exception e) {
						sendErrMsg(e.getMessage());
					}
				}
			};
		}
		//停止按钮
		public SelectionAdapter getPauseFunction(){
			return new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					xiaoMiController.stop("停止");
				}
			};
		}
		
		//搜索商品名
		public SelectionAdapter getParseFunction(){
			return new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String name = getNameText().getText().trim();
					if(name.length()==0){
						return;
					}
					String errmsg = xiaoMiController.searchGoods(name);
					if(errmsg!=null){
						getNameText().setText(name+errmsg);
						return;
					}
					getShell().setText(Config.goodsConfig.getName());
					getNameText().setText(Config.goodsConfig.getName());
					List<String> version = Config.goodsConfig.getVersion();
					getOption1().removeAll();
					getOption1().add("默认");
					for(String s:version){
						getOption1().add(s);
					}
					getOption1().select(0);
					
					List<String> color = Config.goodsConfig.getColor();
					getOption2().removeAll();
					getOption2().add("默认");
					for(String s:color){
						getOption2().add(s);
					}
					getOption2().select(0);
				}
			};
		}
		//修改状态后控件的变化效果
		private void modifyStatus(boolean isFinish) {
			getStartButton().setVisible(isFinish);
			getPauseButton().setVisible(!isFinish);
			getParseButton().setVisible(isFinish);
			getNameText().setEditable(isFinish);
			getBuyTimeText().setEditable(isFinish);
			getDurationText().setEditable(isFinish);
			getUserText().setEditable(isFinish);
			getPasswordText().setEditable(isFinish);
			
			getOption1().setEnabled(isFinish);
			getOption2().setEnabled(isFinish);
		}
		public void readParameter() throws Exception {
			
			String name = getNameText().getText().trim();
			if(Config.goodsConfig==null||!name.equals(Config.goodsConfig.getName())){
				throw new Exception("请先搜索商品");
			}
			String version = getOption1().getItem(getOption1().getSelectionIndex());
			String color = getOption2().getItem(getOption2().getSelectionIndex());
			Config.goodsInfo = new GoodsInfo(Config.goodsConfig.getUrl(),version,color);
			String buyTime = getBuyTimeText().getText().trim();
			String duration = getDurationText().getText().trim();
			Config.customRule = new CustomRule(buyTime,duration);
			
			String user = getUserText().getText().trim();
			String password = getPasswordText().getText().trim();
			Config.user = new User(user,password);
			
			
		}
		
		//初始化视图
		private void initView() {
			xiaoMiController.init();
			//设置界面参数
			setViewSetting();
		}
		
		private void setViewSetting() {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			long time = System.currentTimeMillis()+2*60*1000;
			getBuyTimeText().setText(format.format(new Date(time)));
			
		}
		//日志输出
		private void loadLog() {
			String loadLog = xiaoMiController.loadLog();
			if(loadLog.length()==0){
				return;
			}
			if(getLogText().getLineCount()>3000){
				getLogText().setText(loadLog);
				return;
			}
			getLogText().append(loadLog);
			
		}
		
		//任务完成后的处理逻辑
		private void finishMsg() {
			if(StatusManage.isEnd){
				//修改状态为完成
				modifyStatus(true);
				//发送消息
				sendMsg(StatusManage.endMsg);
				//恢复初始状态
				StatusManage.isEnd = false;
				
				
			}
			
		}
		//打开视图
		public void openView(){
			initView();
			getShell().open();
			while (!getShell().isDisposed()) {
				//日志输出
				loadLog();
				//任务完成
				finishMsg();
				if (!getDisplay().readAndDispatch()) {
					try {
						Thread.sleep(150);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//getDisplay().sleep();
			}
			getDisplay().dispose();
		}
		
		//错误弹框
		public void sendErrMsg(String msg) {
			MessageBox errorBox = new MessageBox(getShell(), SWT.ICON_ERROR);
			errorBox.setText("错误");
			if (msg == null) {
				errorBox.setMessage("未知错误,请检查参数设置是否正确");
			} else {
				errorBox.setMessage(msg);
			}
			errorBox.open();
		}
		//确认框
		public int verifyMsg(String msg){
			MessageBox infoMsg = new MessageBox(getShell(), SWT.YES|SWT.NO|SWT.ICON_INFORMATION );
			infoMsg.setText("请确认");
			infoMsg.setMessage(msg);
			return infoMsg.open();
		}
		//提示弹框
		public void sendMsg(String msg) {
			MessageBox infoMsg = new MessageBox(getShell(), SWT.ICON_WORKING);
			infoMsg.setText("提示");
			infoMsg.setMessage(msg);
			infoMsg.open();
		}
}
