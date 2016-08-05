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
		//m_oPageList =  new ArrayList<String>(1024);  // Ĭ����� 1024 ��ҳ��
		
		// ���ý����ú�������������������⡣
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		// ��ʼ�����뷨
		mImm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        
		/*
        // ���ر���
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);  
        */
		
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        	    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		
        mContainer = new ContainerView(this);
        
        // ���������˿���������ȡ�߶���Ϣ720����Ļ��Ϣ�᲻��Ӧ��
        //Display display = getWindowManager().getDefaultDisplay();
        //MainWindow.SCREEN_WIDTH = display.getWidth();
        //MainWindow.SCREEN_HEIGHT = display.getHeight();
        
		MainWindow.SCREEN_WIDTH = 1024;  // VTU screen width
		MainWindow.SCREEN_HEIGHT = 768;  // VTU screen height
		
		UtIniReader iniReader = null;		// ��ȡINI����
		try {
			iniReader = new UtIniReader(Environment.getExternalStorageDirectory().getPath() + "/MGrid.ini");
		} catch (Exception e) {
			iniReader = null;
			e.printStackTrace();
			new AlertDialog.Builder(this).setTitle("����") .setMessage("��ȡ�����ļ� [ MGrid.ini ] �쳣��ֹͣ���أ�\n���飺"+ e.toString()) .show();
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
		
		// ��ȡ�ɼ������ڻ��� IP
		String strIPC_IP = iniReader.getValue("NetConf", "IPC_IP");
		if (null != strIPC_IP && !strIPC_IP.trim().isEmpty())
		{
			service.IP = strIPC_IP.trim();
		}
		
		// ��ȡ VTU-IPC �˿�
		try
		{
			int port = Integer.parseInt(iniReader.getValue("NetConf", "IPC_PORT"));
			service.PORT = port;  // ��������쳣�����Ӧ�ò���ִ�С�
		}
		catch(java.lang.NumberFormatException e){
		}
		
		// ��ʼԤ������̬ҳ��
		String line = "";
		//MainWindow prevpage = null;
		MainWindow page = null;
		BufferedReader reader = null;
		
		try{
		reader = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(Environment.getExternalStorageDirectory().getPath() + m_sRootFolder + "pagelist"), 
						"gb2312"));
	    
		// ׼������ҳ��
		DataGetter.bIsLoading = true;
		for (int i = 0; i < 1024; i++)
		{
			// �Ƿ��ȡ����ĩβ
	        if ((line = reader.readLine()) == null) break;
			
	        // ��������
	        line = line.trim();
	        if (line.isEmpty()) continue;
	        
	        m_oPageList.add(line);
	        
	        // ������ҳ�ݲ�����
	        if (!line.equals(m_sMainPage))
	        {
			    continue;
	        }
			
	        // ������ҳ
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
			new AlertDialog.Builder(this).setTitle("����") .setMessage("����ҳ�� [ " + line +" ] �����쳣��ֹͣ���أ�\n���飺"+ e.toString()) .show();
		}
		
		m_oSgSgRenderManager = m_oViewGroups.get(m_sMainPage);
		if (null == m_oSgSgRenderManager)
		{
			new AlertDialog.Builder(this).setTitle("����") .setMessage("�Ҳ�����ҳ [ " + m_sMainPage +" ] ��").show();
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

		    //���� �ޱ��������޵����� �� ȫ��
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
	        
	        //setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
			
			setContentView(mContainer);
			mContainer.requestFocus();
	        
			// ����������
			showTaskUI(false);
			
			// �������뷨����ʱ��ѹ������
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
			
			if (m_oPageList.size() == 1)
			{
				tmp_flag_loading = false;
				DataGetter.bIsLoading = false;
				Toast.makeText(this, "�������", Toast.LENGTH_LONG).show();
			}
			else
			{
				String strPageName = m_oPageList.get( m_sMainPage.equals(m_oPageList.get(0)) ? 1 : 0);
				strPageName = strPageName.substring(0, strPageName.length()-4);
				showLoadProgToast("����  [ " + (tmp_load_pageseek+1) + "/" + m_oPageList.size() 
						+ " ] , ���ڼ���  [ " + strPageName + " ]  ...", Toast.LENGTH_SHORT);
			}
			
			// ��ʼ����ҳ�ϵ��б�ؼ�
			//* �����߳�ģ��������������ò��������б�ؼ����޸��������⡣ע�������õķֱ��ʲ���Ӧ����Ȼ�������⡣
			final Handler handler=new Handler();
			Runnable runnable=new Runnable(){
				public void run() {
					//m_oSgSgRenderManager.notifylistflush();
					
					String pagename = m_oPageList.get(tmp_load_pageseek);
					
					// ���ص���ҳʱ
					if (pagename.equals(m_sMainPage))
					{
						// �趨��ҳǰ������̽ڵ�
						MainWindow mainPage = m_oViewGroups.get(pagename);
						mainPage.m_oPrevPage = tmp_load_prevpage;
						if (null != tmp_load_prevpage) tmp_load_prevpage.m_oNextPage = mainPage;
						tmp_load_prevpage = mainPage;
						
						// ��ҳ����ɼ��ء��α�ǰ��������������һҳ��
						tmp_load_pageseek ++;
						if (m_oPageList.size() > tmp_load_pageseek)
						    pagename = m_oPageList.get(tmp_load_pageseek);
						else
						{
							tmp_flag_loading = false;
							DataGetter.bIsLoading = false;
							Toast.makeText(MGridActivity.this, "�������", Toast.LENGTH_LONG).show();
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
						new AlertDialog.Builder(MGridActivity.this).setTitle("����") .setMessage("����ҳ�� [ " + pagename +" ] �����쳣��ֹͣ���أ�\n���飺"+ e.toString()) .show();
						return;
					}

					// �趨ǰ������̽ڵ�
					page.m_oPrevPage = tmp_load_prevpage;
					if (null != tmp_load_prevpage) tmp_load_prevpage.m_oNextPage = page;
					tmp_load_prevpage = page;
					
					// �α�ǰ��������������һҳ��
					tmp_load_pageseek ++;
					
					// ��ʾ������Ϣ
					if (m_oPageList.size() > tmp_load_pageseek)
					{
						String nextPage = m_oPageList.get(tmp_load_pageseek);
						if (m_sMainPage.equals(nextPage))
						{
							if (m_oPageList.size() > tmp_load_pageseek+1)
							{
								nextPage = m_oPageList.get(tmp_load_pageseek+1);
								nextPage = nextPage.substring(0, nextPage.length()-4);
								showLoadProgToast("����  [ " + (tmp_load_pageseek+1) + "/" + m_oPageList.size() 
										+ " ] , ���ڼ���  [ " + nextPage + " ]  ...", Toast.LENGTH_SHORT);
							}
						}
						else
						{
							nextPage = nextPage.substring(0, nextPage.length()-4);
							showLoadProgToast("����  [ " + (tmp_load_pageseek+1) + "/" + m_oPageList.size() 
									+ " ] , ���ڼ���  [ " + nextPage + " ]  ...", Toast.LENGTH_SHORT);
						}
						
					    handler.postDelayed(this, tmp_load_int_time);
					}
					else
					{
						tmp_flag_loading = false;
						DataGetter.bIsLoading = false;
						Toast.makeText(MGridActivity.this, "�������", Toast.LENGTH_LONG).show();
					}
					
				}  // end of run
			};
			// ��ʱ������Ӧ�ȴ�������ʾ�����̫�����ʼ���б���ʧЧ����̫�����ʼ������̫���ԡ�
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
		
		// �����ж���Ч������ Cache
		mContainer.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
		
		// ִ�������߳�
		DataGetter.currentPage = m_sMainPage;
		mDataGetter = new DataGetter();
		mDataGetter.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		mDataGetter.start();
		
    }  // end of onCreate
	
	// �����µ�3D��������Ч��
    public void applyRotation(String pagename, float start, float end) {
        // Find the center of the container
        final float centerX = mContainer.getWidth() / 2.0f;
        final float centerY = mContainer.getHeight() / 2.0f;

        // �ṩ��������һ���µ�3D��������
        // �����������������������һ������
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
		
		// ����ת��������ҳ���ݴ���
		if (null ==  m_oViewGroups.get(pagename))
		{
			if (tmp_flag_loading)
				new AlertDialog.Builder(this).setTitle("��ʾ��") .setMessage("Ŀ��ҳ�����ڼ����� ��") .show();
			else
				new AlertDialog.Builder(this).setTitle("����") .setMessage("�޷��ҵ���̬ҳ�棺 " + pagename) .show();
			
			return ;
		}
		
		m_oSgSgRenderManager.active(false);
		m_oSgSgRenderManager = m_oViewGroups.get(pagename);
		m_oSgSgRenderManager.active(true);
		
		// ����ʹ��������ʾ View ������������������� -- CharlesChen May 8, 2014.
		//setContentView(m_oSgSgRenderManager);
		
		mContainer.mCurrentView.setVisibility(View.GONE);
		mContainer.mCurrentView = m_oSgSgRenderManager;
		mContainer.mCurrentView.setVisibility(View.VISIBLE);
		//mContainer.bringChildToFront(mContainer.mCurrentView);
		
		DataGetter.currentPage = pagename;
		//m_oSgSgRenderManager.notifylistflush();
	}
	

	/** ��ʾ/��������˵� */
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

	// ������
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
	private Intent m_oTaskIntent = null;  // ��������������
	private MainWindow m_oSgSgRenderManager = null;
	private HashMap<String, MainWindow> m_oViewGroups = null;
	
	// �߲���̬ҳ������
	private ContainerView mContainer;
	
	// ����ģ���߳�
	private DataGetter mDataGetter;
	
	// ���뷨������
	public InputMethodManager mImm = null;

	
    /** ���´���Ϊ�ڲ���
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
     * ���ฺ���л�View����
     * ��ִ�к�벿�ֵĶ���Ч��
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
            
            /*  �����л��Ƕȵĳ���
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
            
            // TODO: ��ʱ������Բ�ܷ���Ч�����Ժ���ʱ���ٵ�У���Ч��  -- CharlesChen
            rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);  // �ٴ��Ż��� ��270�ȿ�ʼ�����ɱ��ⷴת��

            rotation.setDuration(500);
            rotation.setFillAfter(true);
            rotation.setInterpolator(new DecelerateInterpolator());

            mContainer.startAnimation(rotation);
        }
    }  /* end of class SwapViews */
    
}