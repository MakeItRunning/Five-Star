package com.mgrid.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.mgrid.data.DataGetter;
import com.sg.common.CFGTLS;
import com.sg.common.IObject;
import com.sg.common.UtIniReader;

import comm_service.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressLint("InlinedApi")
public class MGridActivity extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		m_oViewGroups = new HashMap<String, MainWindow>();
		m_oPageList =  new ArrayList<String>();
		//m_oPageList =  new ArrayList<String>(1024);  // 默认最多 1024 个页面
		
		// 设置仅适用横屏，解决竖屏死机问题。
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		// 初始化输入法
		mImm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        
		/*
        // 隐藏标题
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);  
        */
		
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        	    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		
        mContainer = new ContainerView(this);
        
        // 由于隐藏了控制条，获取高度信息720与屏幕信息会不对应。
        //Display display = getWindowManager().getDefaultDisplay();
        //MainWindow.SCREEN_WIDTH = display.getWidth();
        //MainWindow.SCREEN_HEIGHT = display.getHeight();
        
		MainWindow.SCREEN_WIDTH = 1024;  // VTU screen width
		MainWindow.SCREEN_HEIGHT = 768;  // VTU screen height
		
		UtIniReader iniReader = null;		// 读取INI配置
		try {
			iniReader = new UtIniReader(Environment.getExternalStorageDirectory().getPath() + "/MGrid.ini");
		} catch (Exception e) {
			iniReader = null;
			e.printStackTrace();
			new AlertDialog.Builder(this).setTitle("错误") .setMessage("读取配置文件 [ MGrid.ini ] 异常，停止加载！\n详情："+ e.toString()) .show();
		}
		
		if (iniReader == null) {
			return;
		}
		
		m_sRootFolder = iniReader.getValue("SysConf", "FolderRoot");
		m_sMainPage = iniReader.getValue("SysConf", "MainPage");
		
		m_bHasRandomData = Boolean.parseBoolean(iniReader.getValue("SysConf", "HasRandomData"));
		m_bBitmapHIghQuality = Boolean.parseBoolean(iniReader.getValue("SysConf", "BitmapHIghQuality"));
		m_bErrMsgParser = !Boolean.parseBoolean(iniReader.getValue("SysConf", "NoErrMsgParser"));
		m_bShowLoadProgress = Boolean.parseBoolean(iniReader.getValue("SysConf", "ShowLoadProgress", "true"));
		
		try
		{
			tmp_load_int_time = Integer.parseInt(iniReader.getValue("SysConf", "LoadingInterval"));
		} catch (java.lang.NumberFormatException e)
		{
			tmp_load_int_time = 200;
		}

		try{
		MainWindow.SWITCH_STYLE = Integer.parseInt(iniReader.getValue("SysConf", "UseAnimation"));
		}
		catch(java.lang.NumberFormatException e){
			MainWindow.SWITCH_STYLE = 0;
		}
		
		CFGTLS.BITMAP_HIGHQUALITY = m_bBitmapHIghQuality;
		
		// 获取采集器所在机器 IP
		String strIPC_IP = iniReader.getValue("NetConf", "IPC_IP");
		if (null != strIPC_IP && !strIPC_IP.trim().isEmpty())
		{
			service.IP = strIPC_IP.trim();
		}
		
		// 获取 VTU-IPC 端口
		try
		{
			int port = Integer.parseInt(iniReader.getValue("NetConf", "IPC_PORT"));
			service.PORT = port;  // 如果发生异常，这句应该不会执行。
		}
		catch(java.lang.NumberFormatException e){
		}
		
		// 开始预加载组态页面
		String line = "";
		//MainWindow prevpage = null;
		MainWindow page = null;
		BufferedReader reader = null;
		
		try{
		reader = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(Environment.getExternalStorageDirectory().getPath() + m_sRootFolder + "pagelist"), 
						"gb2312"));
	    
		// 准备加载页面
		DataGetter.bIsLoading = true;
		for (int i = 0; i < 1024; i++)
		{
			// 是否读取到了末尾
	        if ((line = reader.readLine()) == null) break;
			
	        // 跳过空行
	        line = line.trim();
	        if (line.isEmpty()) continue;
	        
	        m_oPageList.add(line);
	        
	        // 不是主页暂不加载
	        if (!line.equals(m_sMainPage))
	        {
			    continue;
	        }
			
	        // 加载主页
			page = new MainWindow(this);
			page.m_strRootFolder = m_sRootFolder;
			page.m_bHasRandomData = m_bHasRandomData;
			page.loadPage(line);
			page.active(false);
			
			page.setVisibility(View.GONE);
			m_oViewGroups.put(line, page);
			mContainer.addView(page, 1024, 768);
			
			//page.m_oPrevPage = prevpage;
			//if (0 != i) prevpage.m_oNextPage = page;
			//prevpage = page;
		}
		
	    reader.close();
		}
		catch(Exception e){
			e.printStackTrace();
			new AlertDialog.Builder(this).setTitle("错误") .setMessage("加载页面 [ " + line +" ] 出现异常，停止加载！\n详情："+ e.toString()) .show();
		}
		
		m_oSgSgRenderManager = m_oViewGroups.get(m_sMainPage);
		if (null == m_oSgSgRenderManager)
		{
			new AlertDialog.Builder(this).setTitle("错误") .setMessage("找不到主页 [ " + m_sMainPage +" ] ！").show();
		}
		
		if (0 != mContainer.getChildCount() && null != m_oSgSgRenderManager) 
		{
			// m_oPageList.trimToSize();

			m_oSgSgRenderManager.active(true);
			// setContentView(m_oSgSgRenderManager);

			mContainer.setClipChildren(false);
			mContainer.mCurrentView = m_oSgSgRenderManager;
			m_oSgSgRenderManager.setVisibility(View.VISIBLE);
			m_oSgSgRenderManager.requestFocus();

		    //设置 无标题栏、无导航栏 和 全屏
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
	        
	        //setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
			
			setContentView(mContainer);
			mContainer.requestFocus();
	        
			// 隐藏任务栏
			showTaskUI(false);
			
			// 设置输入法弹出时不压缩界面
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
			
			if (m_oPageList.size() == 1)
			{
				tmp_flag_loading = false;
				DataGetter.bIsLoading = false;
				Toast.makeText(this, "加载完毕", Toast.LENGTH_LONG).show();
			}
			else
			{
				String strPageName = m_oPageList.get( m_sMainPage.equals(m_oPageList.get(0)) ? 1 : 0);
				strPageName = strPageName.substring(0, strPageName.length()-4);
				showLoadProgToast("进度  [ " + (tmp_load_pageseek+1) + "/" + m_oPageList.size() 
						+ " ] , 正在加载  [ " + strPageName + " ]  ...", Toast.LENGTH_SHORT);
			}
			
			// 初始化主页上的列表控件
			//* 在现线程模型运作下已无需该操作，且列表控件已修复布局问题。注，若配置的分辨率不对应则依然会有问题。
			final Handler handler=new Handler();
			Runnable runnable=new Runnable(){
				public void run() {
					//m_oSgSgRenderManager.notifylistflush();
					
					String pagename = m_oPageList.get(tmp_load_pageseek);
					
					// 加载到主页时
					if (pagename.equals(m_sMainPage))
					{
						// 设定主页前驱、后继节点
						MainWindow mainPage = m_oViewGroups.get(pagename);
						mainPage.m_oPrevPage = tmp_load_prevpage;
						if (null != tmp_load_prevpage) tmp_load_prevpage.m_oNextPage = mainPage;
						tmp_load_prevpage = mainPage;
						
						// 主页已完成加载。游标前进，继续加载下一页。
						tmp_load_pageseek ++;
						if (m_oPageList.size() > tmp_load_pageseek)
						    pagename = m_oPageList.get(tmp_load_pageseek);
						else
						{
							tmp_flag_loading = false;
							DataGetter.bIsLoading = false;
							Toast.makeText(MGridActivity.this, "加载完毕", Toast.LENGTH_LONG).show();
							return;
						}
					}
					
					MainWindow page = new MainWindow(MGridActivity.this);
					page.m_strRootFolder = m_sRootFolder;
					page.m_bHasRandomData = m_bHasRandomData;
					
					try
					{
						page.loadPage(pagename);
						page.active(false);
						
						page.setVisibility(View.GONE);
						m_oViewGroups.put(pagename, page);
						mContainer.addView(page, 1024, 768);
					} catch (FileNotFoundException e)
					{
						e.printStackTrace();
						tmp_flag_loading = false;
						DataGetter.bIsLoading = false;
						new AlertDialog.Builder(MGridActivity.this).setTitle("错误") .setMessage("加载页面 [ " + pagename +" ] 出现异常，停止加载！\n详情："+ e.toString()) .show();
						return;
					}

					// 设定前驱、后继节点
					page.m_oPrevPage = tmp_load_prevpage;
					if (null != tmp_load_prevpage) tmp_load_prevpage.m_oNextPage = page;
					tmp_load_prevpage = page;
					
					// 游标前进，继续加载下一页。
					tmp_load_pageseek ++;
					
					// 显示加载信息
					if (m_oPageList.size() > tmp_load_pageseek)
					{
						String nextPage = m_oPageList.get(tmp_load_pageseek);
						if (m_sMainPage.equals(nextPage))
						{
							if (m_oPageList.size() > tmp_load_pageseek+1)
							{
								nextPage = m_oPageList.get(tmp_load_pageseek+1);
								nextPage = nextPage.substring(0, nextPage.length()-4);
								showLoadProgToast("进度  [ " + (tmp_load_pageseek+1) + "/" + m_oPageList.size() 
										+ " ] , 正在加载  [ " + nextPage + " ]  ...", Toast.LENGTH_SHORT);
							}
						}
						else
						{
							nextPage = nextPage.substring(0, nextPage.length()-4);
							showLoadProgToast("进度  [ " + (tmp_load_pageseek+1) + "/" + m_oPageList.size() 
									+ " ] , 正在加载  [ " + nextPage + " ]  ...", Toast.LENGTH_SHORT);
						}
						
					    handler.postDelayed(this, tmp_load_int_time);
					}
					else
					{
						tmp_flag_loading = false;
						DataGetter.bIsLoading = false;
						Toast.makeText(MGridActivity.this, "加载完毕", Toast.LENGTH_LONG).show();
					}
					
				}  // end of run
			};
			// 延时触发，应等待界面显示后。如果太快则初始化列表动作失效，如太慢则初始化动作太明显。
			handler.postDelayed(runnable, tmp_load_int_time);
			//*/
		}
		else
		{
	        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	        requestWindowFeature(Window.FEATURE_PROGRESS);
	        setContentView(R.layout.main);
	        
	        setProgressBarVisibility(true);
	        setProgressBarIndeterminateVisibility(true);
		}
		
		// 对所有动画效果进行 Cache
		mContainer.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
		
		// 执行数据线程
		DataGetter.currentPage = m_sMainPage;
		mDataGetter = new DataGetter();
		mDataGetter.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		mDataGetter.start();
		
    }  // end of onCreate
	
	// 设置新的3D翻滚动画效果
    public void applyRotation(String pagename, float start, float end) {
        // Find the center of the container
        final float centerX = mContainer.getWidth() / 2.0f;
        final float centerY = mContainer.getHeight() / 2.0f;

        // 提供参数创建一个新的3D翻滚动画
        // 这个动画监听器用来触发下一个动画
        final Rotate3dAnimation rotation =
                new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
        rotation.setDuration(500);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateInterpolator());
        rotation.setAnimationListener(new DisplayNextView(pagename));

        mContainer.startAnimation(rotation);
    }
	
	public void onPageChange(String pagename)
	{
		/*
		InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
		*/
		
		// 对跳转到不存在页面容错处理
		if (null ==  m_oViewGroups.get(pagename))
		{
			if (tmp_flag_loading)
				new AlertDialog.Builder(this).setTitle("提示！") .setMessage("目标页面正在加载中 …") .show();
			else
				new AlertDialog.Builder(this).setTitle("错误！") .setMessage("无法找到组态页面： " + pagename) .show();
			
			return ;
		}
		
		m_oSgSgRenderManager.active(false);
		m_oSgSgRenderManager = m_oViewGroups.get(pagename);
		m_oSgSgRenderManager.active(true);
		
		// 不再使用设置显示 View 方案，以下面操作代替 -- CharlesChen May 8, 2014.
		//setContentView(m_oSgSgRenderManager);
		
		mContainer.mCurrentView.setVisibility(View.GONE);
		mContainer.mCurrentView = m_oSgSgRenderManager;
		mContainer.mCurrentView.setVisibility(View.VISIBLE);
		//mContainer.bringChildToFront(mContainer.mCurrentView);
		
		DataGetter.currentPage = pagename;
		//m_oSgSgRenderManager.notifylistflush();
	}
	

	/** 显示/隐藏任务菜单 */
	public void showTaskUI(boolean bShow)
	{
		if (m_oTaskIntent == null)
			m_oTaskIntent = new Intent();
		if (bShow)
		{
			m_oTaskIntent.setAction("android.intent.action.STATUSBAR_VISIBILITY");
			m_oSgSgRenderManager.getContext().sendBroadcast(m_oTaskIntent);
		} else
		{
			m_oTaskIntent.setAction("android.intent.action.STATUSBAR_INVISIBILITY");
			m_oSgSgRenderManager.getContext().sendBroadcast(m_oTaskIntent);
		}
	}

	 @Override
	protected void onResume()
	{
		super.onResume();
		if (m_oSgSgRenderManager == null)
			return;
		showTaskUI(false);
	}

	void showToast(CharSequence msg)
	{
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	void showLoadProgToast(CharSequence msg, int duration)
	{
		if (m_bShowLoadProgress)
			Toast.makeText(this, msg, duration).show();
	}

	// 加载用
	public int tmp_load_int_time = 200;
	public int tmp_load_pageseek = 0;
	public boolean tmp_flag_loading = true;
	public MainWindow tmp_load_prevpage = null;
	 
	// Params:
	public String m_sMainPage = null;
	public String m_sRootFolder = null;
	public static boolean m_bHasRandomData = false;
	public static boolean m_bBitmapHIghQuality = false;
	public static boolean m_bShowLoadProgress = true;
	public static boolean m_bErrMsgParser = true;
	
	public ArrayList<String> m_oPageList = null;
	private Intent m_oTaskIntent = null;  // 用于隐藏任务栏
	private MainWindow m_oSgSgRenderManager = null;
	private HashMap<String, MainWindow> m_oViewGroups = null;
	
	// 高层组态页面容器
	private ContainerView mContainer;
	
	// 数据模型线程
	private DataGetter mDataGetter;
	
	// 输入法管理器
	public InputMethodManager mImm = null;

	
    /** 以下代码为内部类
     * This class listens for the end of the first half of the animation.
     * It then posts a new action that effectively swaps the views when the container
     * is rotated 90 degrees and thus invisible.
     */
    private final class DisplayNextView implements Animation.AnimationListener {
        private final String mPageName;

        private DisplayNextView(String pagename) {
            mPageName = pagename;
        }

        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
            mContainer.post(new SwapViews(mPageName));
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }

    /**
     * 该类负责切换View界面
     * 并执行后半部分的动画效果
     */
    private final class SwapViews implements Runnable {
        private final String mPageName;

        public SwapViews(String pagename) {
        	mPageName = pagename;
        }

        public void run() {
            final float centerX = mContainer.getWidth() / 2.0f;
            final float centerY = mContainer.getHeight() / 2.0f;
            Rotate3dAnimation rotation;
            
            onPageChange(mPageName);
            
            /*  两种切换角度的尝试
            if (mPosition > -1) {
                mPhotosList.setVisibility(View.GONE);
                mImageView.setVisibility(View.VISIBLE);
                mImageView.requestFocus();

                rotation = new Rotate3dAnimation(90, 180, centerX, centerY, 310.0f, false);
            } else {
                mImageView.setVisibility(View.GONE);
                mPhotosList.setVisibility(View.VISIBLE);
                mPhotosList.requestFocus();

                rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
            }
            */
            
            // TODO: 暂时尝试做圆周翻滚效果，以后有时间再调校最佳效果  -- CharlesChen
            rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);  // 再次优化， 从270度开始翻滚可避免反转。

            rotation.setDuration(500);
            rotation.setFillAfter(true);
            rotation.setInterpolator(new DecelerateInterpolator());

            mContainer.startAnimation(rotation);
        }
    }  /* end of class SwapViews */
    
}