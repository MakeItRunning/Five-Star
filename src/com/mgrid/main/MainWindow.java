package com.mgrid.main;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mgrid.data.DataGetter;
import com.mgrid.data.EquipmentDataModel.Event;
import com.mgrid.data.EquipmentDataModel.Signal;
import com.sg.common.Calculator;
import com.sg.common.IObject;
import com.sg.common.SgRealTimeData;
import com.sg.common.MutiThreadShareObject;
import com.sg.common.UtExpressionParser;
import com.sg.common.UtExpressionParser.stBindingExpression;
import com.sg.common.UtExpressionParser.stExpression;
import com.sg.uis.SgAlarmLight;
import com.sg.uis.SgAmmeter;
import com.sg.uis.SgButton;
import com.sg.uis.SgCableTerminal;
import com.sg.uis.SgCommandButton;
import com.sg.uis.SgEllipse;
import com.sg.uis.SgEventList;
import com.sg.uis.SgForm;
import com.sg.uis.SgGND;
import com.sg.uis.SgImage;
import com.sg.uis.SgIsolationEventSetter;
import com.sg.uis.SgIsolationSwitch;
import com.sg.uis.SgLabel;
import com.sg.uis.SgChart;
import com.sg.uis.SgCurveLineChart;
import com.sg.uis.SgPolyline;
import com.sg.uis.SgRectangle;
import com.sg.uis.SgSignalList;
import com.sg.uis.SgSignalNameSetter;
import com.sg.uis.SgStatePanel;
import com.sg.uis.SgStraightLine;
import com.sg.uis.SgTable;
import com.sg.uis.SgTextBox;
import com.sg.uis.SgTextClock;
import com.sg.uis.SgThermometer;
import com.sg.uis.SgYKParameter;
import com.sg.uis.SgYTParameter;
import com.sg.uis.SgTriggerSetter;

import comm_service.service;
import data_model.ipc_history_signal;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.FloatMath;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

@SuppressLint({ "HandlerLeak", "NewApi" })
/** 主窗口 */
public class MainWindow extends ViewGroup {

	public MainWindow(final MGridActivity context) {
		super(context);
		setFocusableInTouchMode(true);
		m_mapUIs = new HashMap<String, IObject>();
		m_oShareObject = new MutiThreadShareObject(); // 数据池
		m_YKobj = new ArrayList<ViewGroup>();
		m_oMgridActivity = context;

		m_oInvalidateHandler = new Handler() {
			// 接收到消息后处理
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					updateWidgets();
					break;
					
				case 1:
					Toast.makeText(context, (String)msg.obj, Toast.LENGTH_SHORT).show();
					break;
					
				case 2:
					new AlertDialog.Builder(context).setTitle("错误") .setMessage((String)msg.obj) .show();
					break;
				}
				
				super.handleMessage(msg);

				/* 界面线程里面居然加Sleep！！！
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				*/
			}
		};

		m_oShareObject.clearFromTcpValue(); // instance one share object here
		m_oShareObject.m_oInvalidateHandler = m_oInvalidateHandler;
	}

	/** 更新变化的界面数据 */
	void updateWidgets() {
		// TODO: 这个设置是否为了确定放大后的尺寸？ 为更新放大后的UI提供定位依据。  -- CharlesChen
		// 如果窗口大小改变了
		

		MainWindow.SCREEN_WIDTH = this.getResources().getDisplayMetrics().widthPixels;
		MainWindow.SCREEN_HEIGHT = this.getResources().getDisplayMetrics().heightPixels;

		// TODO: 此等逻辑处理应该放到子线程中，主线程仅从共享队列取出对象调用更新  -- CharlesChen
		for (int i = 0; i < m_oShareObject.m_listUpdateFromTcpValues.size(); ++i) {
			IObject obj = m_oShareObject.m_listUpdateFromTcpValues.get(i);
			if (obj != null) {
				obj.updateWidget();
			}
		}
		m_oShareObject.clearFromTcpValue(); // 读完需要自己清空->以便数据线程写入新的数据

		/* 自己就是最优先的，交给谁？  -- CharlesChen
		// 交出优先权
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		*/
	}

	/** 替换字符串 */
	public String replaceString(String strSource, String strFrom, String strTo) {
		if (strSource == null) {
			return null;
		}
		int i = 0;
		if ((i = strSource.indexOf(strFrom, i)) >= 0) {
			char[] cSrc = strSource.toCharArray();
			char[] cTo = strTo.toCharArray();
			int len = strFrom.length();
			StringBuffer buf = new StringBuffer(cSrc.length);
			buf.append(cSrc, 0, i).append(cTo);
			i += len;
			int j = i;
			while ((i = strSource.indexOf(strFrom, i)) > 0) {
				buf.append(cSrc, j, i - j).append(cTo);
				i += len;
				j = i;
			}
			buf.append(cSrc, j, cSrc.length - j);
			return buf.toString();
		}
		return strSource;
	}

	@SuppressLint({ "InlinedApi", "FloatMath" })
	@Override
	/** 按键响应 */
	public boolean onInterceptTouchEvent(MotionEvent event) {

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			m_fOldX = event.getX();
			m_fOldY = event.getY();
			m_fDragStartX = event.getX();
			m_fDragStartY = event.getY();
			m_fDragEndX = event.getX();
			m_fDragEndY = event.getY();
			m_bTwoFigerDown = false;
			m_bOneFigerDown = true;
			m_bHadTowFiger = false;
			
			// CharlesChen
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
				mVelocityTracker.addMovement(event);
			}
			
			return false;

		case MotionEvent.ACTION_POINTER_DOWN:
			m_fStartScaleValue = distance(event);
			if (m_fStartScaleValue > 5.0f) {
				m_bTwoFigerDown = true;
			}
			m_bOneFigerDown = false;
			m_bHadTowFiger = true;
			break;

		case MotionEvent.ACTION_MOVE:
			// 采集加速度
			if (mVelocityTracker != null) {
				mVelocityTracker.addMovement(event);
			}
			
			if (m_bTwoFigerDown == true) { // 缩放
				float fCurDis = distance(event);
				if (fCurDis < 5.0f)
					break;
				float fScale = (fCurDis - m_fStartScaleValue) / 250.0f;
				m_fStartScaleValue = fCurDis;
				m_fScale += fScale;

				m_fScale = m_fScale < 1.0f ? 1.0f : m_fScale;
				m_fScale = m_fScale > 5.0f ? 5.0f : m_fScale;
				this.setScaleX(m_fScale);
				this.setScaleY(m_fScale);
				// reset translate
	            	if (this.getLeft()-this.getRight()*0.5f*(m_fScale-1.0f) - m_fOffsetX >= 0) {
	            		m_fOffsetX += this.getLeft()-this.getRight()*0.5f*(m_fScale-1.0f) - m_fOffsetX;
	            	}
	            	if (this.getRight()+this.getRight()*0.5f*(m_fScale-1.0f) - m_fOffsetX <= SCREEN_WIDTH) {
	            		m_fOffsetX += this.getRight()+this.getRight()*0.5f*(m_fScale-1.0f) - m_fOffsetX - SCREEN_WIDTH;
	            	}
	            	if (this.getTop()-this.getBottom()*0.5f*(m_fScale-1.0f) - m_fOffsetY >= 0) {
	            		m_fOffsetY += this.getTop()-this.getBottom()*0.5f*(m_fScale-1.0f) - m_fOffsetY;	
	            	}
	            	if (this.getBottom()+this.getBottom()*0.5f*(m_fScale-1.0f) - m_fOffsetY <= SCREEN_HEIGHT) {
	            		m_fOffsetY += this.getBottom()+this.getBottom()*0.5f*(m_fScale-1.0f) - m_fOffsetY - SCREEN_HEIGHT;
	            	}
				if (m_fScale <= 1.0f) {
					m_fOffsetX = 0.0f;
					m_fOffsetY = 0.0f;
				}
				this.setTranslationX(-m_fOffsetX);
				this.setTranslationY(-m_fOffsetY);
			} else if (m_bOneFigerDown == true) { // 拖拉
				
				if (m_fScale <= 1.0f) {
					// TODO: 滑屏操作，应加入画面跟随手指动画。  -- CharlesChen
					break;
				}
				
				float fEllipseX = m_fOldX - event.getX();
				float fEllipseY = m_fOldY - event.getY();
				
				m_fOldX = event.getX();
				m_fOldY = event.getY();
				
	        	if (this.getLeft()-this.getRight()*0.5f*(m_fScale-1.0f) - (m_fOffsetX+fEllipseX) > 0)
	        		fEllipseX = 0.0f;
	        	if (this.getRight()+this.getRight()*0.5f*(m_fScale-1.0f) - (m_fOffsetX+fEllipseX) < SCREEN_WIDTH)
	        		fEllipseX = 0.0f;
	        	if (this.getTop()-this.getBottom()*0.5f*(m_fScale-1.0f) - (m_fOffsetY+fEllipseY) > 0)
	        		fEllipseY = 0.0f;
	        	if (this.getBottom() + this.getBottom()*0.5f*(m_fScale-1.0f) - (m_fOffsetY+fEllipseY) < SCREEN_HEIGHT)
	        		fEllipseY = 0.0f;
				
				m_fOffsetX += fEllipseX * 0.75f;
				m_fOffsetY += fEllipseY * 0.75f;

				this.setTranslationX(-m_fOffsetX);
				this.setTranslationY(-m_fOffsetY);
			}
			break;

		case MotionEvent.ACTION_POINTER_UP:
			m_bTwoFigerDown = false;  //  会引起缩放操作时误切页  -- CharlesChen
			break;

		case MotionEvent.ACTION_UP:
        	this.requestFocus();
			m_oMgridActivity.mImm.hideSoftInputFromWindow(this.getWindowToken(), 0);  // 隐藏任何可能出现的软键盘
			
			/*
			boolean isOpen = m_oMgridActivity.mImm.isActive();
			if (isOpen && !m_bHadTowFiger)
			{
				m_oMgridActivity.mImm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);  // 隐藏任何可能出现的软键盘
			}
			*/
			
			m_bOneFigerDown = false;
			m_fDragEndX = event.getX();
			m_fDragEndY = event.getY();
			if (m_fScale <= 1.0f && !m_bHadTowFiger) { // 滑屏， 增加 m_bHadTowFiger 决断消除误操作。  -- CharlesChen
				
				// 通过加速度判断是否切页，不再使用距离计算。  -- CharlesChen
				int velocityX = 0;
				int velocityY = 0;
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(event);
					mVelocityTracker.computeCurrentVelocity(1000);
					//得到手指移动速度
					velocityX = (int) mVelocityTracker.getXVelocity(); // X轴方向
					velocityY = (int) mVelocityTracker.getYVelocity(); // Y轴方向
					
					// 释放资源
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				
				// 判断坡度
				if (m_fDragEndX-m_fDragStartX == 0 || Math.abs((m_fDragEndY-m_fDragStartY)/(m_fDragEndX-m_fDragStartX)) > 0.6) break;
				
				// 判断加速度纵横比
				if (velocityY > 300 && velocityY != 0 && Math.abs(velocityX/velocityY) < 2.8) break;
				
				//velocityX为正值说明手指向右滑动，为负值说明手指向左滑动
				if (velocityX > SNAP_VELOCITY && null != m_oPrevPage) {
					// 向左滑
					m_oMgridActivity.onPageChange(m_oPrevPage.m_strCurrentPage);
				} else if (velocityX < -SNAP_VELOCITY && null != m_oNextPage) {
					// 向右滑
					m_oMgridActivity.onPageChange(m_oNextPage.m_strCurrentPage);
				} else {
					// 回中
				}
				
			}
			
			m_bHadTowFiger = false;
			break;

		default:
			break;
		}

		return false;
	}
	
	public boolean onTouchEvent(MotionEvent event)
	{
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			break;
			
		//case MotionEvent.ACTION_UP:
			//m_oMgridActivity.mImm.hideSoftInputFromWindow(this.getWindowToken(), 0);  // 隐藏任何可能出现的软键盘
			
		default:
			break;
		}
		
		return false;
	}
	
	// 解决多重 ViewGroup 嵌套事件响应问题。 TODO: 有无其他影响尚待观察  -- CharlesChen
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		/*
		int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
		int measureHeigth = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(measureWidth, measureHeigth);
		// TODO Auto-generated method stub
		for (int i = 0; i < getChildCount(); i++)
		{
			View v = getChildAt(i);
			// Log.v(TAG, "measureWidth is " +v.getMeasuredWidth() +
			// "measureHeight is " +v.getMeasuredHeight());
			int widthSpec = 0;
			int heightSpec = 0;
			LayoutParams params = v.getLayoutParams();
			if (params.width > 0)
			{
				widthSpec = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			} else if (params.width == -1)
			{
				widthSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.EXACTLY);
			} else if (params.width == -2)
			{
				widthSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.AT_MOST);
			}

			if (params.height > 0)
			{
				heightSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
			} else if (params.height == -1)
			{
				heightSpec = MeasureSpec.makeMeasureSpec(measureHeigth, MeasureSpec.EXACTLY);
			} else if (params.height == -2)
			{
				heightSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.AT_MOST);
			}
			v.measure(widthSpec, heightSpec);

		}
		*/
        
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		if (m_YKobj.isEmpty()) return;
		
		Iterator<ViewGroup> ykobj_it = m_YKobj.iterator();
		for (; ykobj_it.hasNext(); )
		{
			ykobj_it.next().measure(widthMeasureSpec, heightMeasureSpec);
		}

		/*
		final int count = getChildCount();
		for (int i = 0; i < count; i++)
		{
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		//*/
	}
	
	protected void dispatchDraw(Canvas canvas)    
	{
	    super.dispatchDraw(canvas);
	    
		if (m_YKobj.isEmpty()) return;
		
		Iterator<ViewGroup> ykobj_it = m_YKobj.iterator();
		for (; ykobj_it.hasNext(); )
		{
			drawChild(canvas, ykobj_it.next(), getDrawingTime());
		}
	}

	/** 计算两点之间的距离像素 **/
	@SuppressLint({ "NewApi", "FloatMath" })
	/** 获取后面的点坐标 - 前面点的坐标 */
	private float distance(MotionEvent e) {
		float eX = e.getX(1) - e.getX(0); // 后面的点坐标 - 前面点的坐标
		float eY = e.getY(1) - e.getY(0);
		return FloatMath.sqrt(eX * eX + eY * eY);
	}

	/** 加载一个新的页面 
	 * @throws FileNotFoundException */
	public void loadPage(String xmlFile) throws FileNotFoundException {
		m_strCurrentPage = xmlFile;
		parseXml(xmlFile);
		
		m_oCaculateThread = new SgExpressionCacularThread();
		m_oCaculateThread.setHasRandomData(m_bHasRandomData);
		Iterator<String> iter = m_mapUIs.keySet().iterator();
		while (iter.hasNext()) {
			String strKey = iter.next();
			IObject obj = m_mapUIs.get(strKey);
			m_oCaculateThread.addExpression(obj.getUniqueID(), obj.getType(),
					obj.getBindingExpression());
			
			obj.initFinished();
		}
		//m_oCaculateThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		m_oCaculateThread.start();
	}

	/** 卸载当前页面 */
	protected void unloadPage() {
		m_oCaculateThread.autoDestroy(); // 让线程自动销毁
		m_fOffsetX = 0.0f;
		m_fOffsetY = 0.0f;
		m_fScale = 1.0f;
		this.setScaleX(m_fScale);
		this.setScaleY(m_fScale);
		this.setTranslationX(m_fOffsetX);
		this.setTranslationY(m_fOffsetY);
		Iterator<String> iter = m_mapUIs.keySet().iterator();
		while (iter.hasNext()) {
			String strKey = iter.next();
			IObject object = m_mapUIs.get(strKey);
			object.removeFromRenderWindow(this);
			object = null;
		}
		m_mapUIs.clear();
		System.gc();
	}

	/** 切换页面xml */
	public void changePage(String strPage) {
		String[] arrStr = strPage.split(",");
		m_strReplaceX1 = null;
		if (arrStr.length == 2)
			m_strReplaceX1 = arrStr[1];
		String strNewPage = arrStr[0] + ".xml";
		if (strNewPage.equals(m_strCurrentPage) == false) {
			// m_strCurrentPage = strNewPage;
			/*
			 * unloadPage(); loadPage(strNewPage);
			 */
			
			// 选择切页效果
			switch (SWITCH_STYLE)
			{
			case 1:
				m_oMgridActivity.applyRotation(strNewPage, 0, 90);
				break;
				
			default:
				m_oMgridActivity.onPageChange(strNewPage);
			}
		}
	}

	public void active(boolean isFrontPage) {
		m_bIsActive = isFrontPage;
		if (isFrontPage)
			synchronized(this){
				this.notifyAll();
			}
	}
	
	private void showMsgDlg(String title, String message)
	{
		if (++NUMOFDAILOG < 10 && MGridActivity.m_bErrMsgParser)
		{
			new AlertDialog.Builder(m_oMgridActivity).setTitle(title) .setMessage(message) .show();
		}
		else if (NUMOFDAILOG == 10 && MGridActivity.m_bErrMsgParser)
		{
			new AlertDialog.Builder(m_oMgridActivity).setTitle("无法显示更多错误信息") .setMessage("该组态配置中错误过多！！！") .show();
		}
	}

	public void showTaskUI(boolean bShow) {
		m_oMgridActivity.showTaskUI(bShow);
	}

	// CharlesChen
	private VelocityTracker mVelocityTracker; // 用于判断甩动手势
	private static final int SNAP_VELOCITY = 600;   //X轴速度基值，大于该值时进行切换
	
	public String m_strCurrentPage = "";
	public MainWindow m_oPrevPage = null;
	public MainWindow m_oNextPage = null;
	public List<ViewGroup> m_YKobj = null;  // 记录需要处理的内部 ViewGroup 控件

	@Override
	/** 重写layout */
	protected void onLayout(boolean bool, int l, int t, int r, int b) {
		VIEW_LEFT = l;
		VIEW_RIGHT = r;
		VIEW_TOP = t;
		VIEW_BOTTOM = b;

		Iterator<String> iter = m_mapUIs.keySet().iterator();
		while (iter.hasNext()) {
			String strKey = iter.next();
			IObject object = m_mapUIs.get(strKey);
			object.doLayout(bool, l, t, r, b);
		}
	}

	/** 解析XML 
	 * @throws FileNotFoundException */
	public void parseXml(String xmlFile) throws FileNotFoundException {
		String[] arrStr = xmlFile.split("\\.");
		m_strResFolder = m_strRootFolder + arrStr[0] + ".files/";

		// 考虑到错误通知在此处不好做，改抛出异常代替捕获。
		//try {
		
			InputStream is = new BufferedInputStream(new FileInputStream(
					Environment.getExternalStorageDirectory().getPath()
							+ m_strRootFolder + xmlFile));
			parseStream(is);
			
		// 添加控件到页
		//HashMap<String, IObject> uis = new HashMap<String, IObject>();
		for (int i = 0; i <= MAXZINDEX; i++)
		{
			Iterator<HashMap.Entry<String, IObject>> entry_it = m_mapUIs.entrySet().iterator();
			while (entry_it.hasNext())
			{
				HashMap.Entry<String, IObject> entry = entry_it.next();
				IObject obj = entry.getValue();
				if (i == obj.getZIndex())
				{
					obj.addToRenderWindow(this);
					//uis.put(entry.getKey(), obj);
				}
			}
		}
		//m_mapUIs=uis;
			
			try
			{
				is.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			/*
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
	}

	/** 解析XML中的所有控件 */
	public void parseStream(InputStream inStream) {
		String strElementType = "";
		IObject iCurrentObj = null;
		XmlPullParser pullParser = Xml.newPullParser();
		
		try {
			pullParser.setInput(inStream, "utf-8");
			int eventType = pullParser.getEventType();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				
				if (eventType == XmlPullParser.START_TAG) {
					String name = pullParser.getName();
					
					if ("Element".equals(name)) {
						String strID = pullParser.getAttributeValue("", "ID");
						String strType = pullParser.getAttributeValue("",
								"Type");
						strElementType = strType;

						boolean bExit = true;
						if ("Form".equals(strType)) {
							SgForm from = new SgForm(this.getContext());
							m_mapUIs.put(strID, from);
						} else if ("Label".equals(strType)) {
							SgLabel sgLabel = new SgLabel(this.getContext());
							m_mapUIs.put(strID, sgLabel);
						} else if ("TextClock".equals(strType)) {
							SgTextClock sgTextClock = new SgTextClock(this.getContext());
							m_mapUIs.put(strID, sgTextClock);
						} else if ("StraightLine".equals(strType)) {
							SgStraightLine sgStraightLine = new SgStraightLine(
									this.getContext());
							m_mapUIs.put(strID, sgStraightLine);
						} else if ("Rectangle".equals(strType)) {
							SgRectangle sgRectangle = new SgRectangle(
									this.getContext());
							m_mapUIs.put(strID, sgRectangle);
						} else if ("Ellipse".equals(strType)) {
							SgEllipse sgEllipse = new SgEllipse(
									this.getContext());
							m_mapUIs.put(strID, sgEllipse);
						} else if ("Polyline".equals(strType)) {
							SgPolyline sgPolyline = new SgPolyline(
									this.getContext());
							m_mapUIs.put(strID, sgPolyline);
						} else if ("Image".equals(strType)) {
							SgImage sgImage = new SgImage(this.getContext());
							m_mapUIs.put(strID, sgImage);
						} else if ("Button".equals(strType)) {
							SgButton sgButton = new SgButton(this.getContext());
							m_mapUIs.put(strID, sgButton);
						} else if ("TextBox".equals(strType)) {
							SgTextBox sgTextBox = new SgTextBox(
									this.getContext());
							m_mapUIs.put(strID, sgTextBox);
						} else if ("Table".equals(strType)) {
							SgTable sgTable = new SgTable(this.getContext());
							m_mapUIs.put(strID, sgTable);
						} else if ("CommandButton".equals(strType)) {
							SgCommandButton sgCommandButton = new SgCommandButton(
									this.getContext());
							m_mapUIs.put(strID, sgCommandButton);
						} else if ("YTParameter".equals(strType)) {
							SgYTParameter sgYTParameter = new SgYTParameter(
									this.getContext());
							m_mapUIs.put(strID, sgYTParameter);
						}
	                    else if ("YKParameter".equals(strType)) {
	                    	SgYKParameter sgYKParameter = new SgYKParameter(this.getContext());
	                    	m_mapUIs.put(strID, sgYKParameter);
	                    	m_YKobj.add(sgYKParameter.m_oSpinner);
	                    }
						else if ("AlarmLight".equals(strType)) {
							SgAlarmLight sgAlarmLight = new SgAlarmLight(
									this.getContext());
							m_mapUIs.put(strID, sgAlarmLight);
						} else if ("Thermometer".equals(strType)) {
							SgThermometer sgThermometer = new SgThermometer(
									this.getContext());
							m_mapUIs.put(strID, sgThermometer);
						} else if ("Ammeter".equals(strType)) {
							SgAmmeter sgAmmeter = new SgAmmeter(
									this.getContext());
							m_mapUIs.put(strID, sgAmmeter);
						} else if ("SignalList".equals(strType)) {
							SgSignalList sgSignalList = new SgSignalList(
									this.getContext());
							m_mapUIs.put(strID, sgSignalList);
						} else if ("EventList".equals(strType)) {
							SgEventList sgEventList = new SgEventList(
									this.getContext());
							m_mapUIs.put(strID, sgEventList);
						} else if ("StatePanel".equals(strType)) {
							SgStatePanel sgStatePanel = new SgStatePanel(
									this.getContext());
							m_mapUIs.put(strID, sgStatePanel);
						} else if ("ThreeDPieChart".equals(strType)) {
							SgChart sgMultiChart = new SgChart(
									this.getContext());
							sgMultiChart.setChartType("Pie");
							m_mapUIs.put(strID, sgMultiChart);
						} else if ("MultiChart".equals(strType)) {
							SgChart sgMultiChart = new SgChart(
									this.getContext());
							sgMultiChart.setChartType("Bar");
							m_mapUIs.put(strID, sgMultiChart);
	                    }else if ("EventConditionStartSetter".equals(strType)) {
							SgTriggerSetter triggerSetter = new SgTriggerSetter(
									this.getContext());
							m_mapUIs.put(strID, triggerSetter);
	                    }
	                    else if ("HistorySignalCurve".equals(strType)) {
	                    	SgCurveLineChart sgLineChart = new SgCurveLineChart(this.getContext());
	                    	m_mapUIs.put(strID, sgLineChart);
						} else if ("IsolationSwitch".equals(strType)) {
							SgIsolationSwitch sgIsolationSwitch = new SgIsolationSwitch(this.getContext());
							m_mapUIs.put(strID, sgIsolationSwitch);
						} else if ("DoubleImageButton".equals(strType)) {
							SgIsolationEventSetter isolationEventSetter = new SgIsolationEventSetter(this.getContext());
							m_mapUIs.put(strID, isolationEventSetter);
						} else if ("SignalNameSetter".equals(strType)) {
							SgSignalNameSetter signalNameSetter = new SgSignalNameSetter(this.getContext());
							m_mapUIs.put(strID, signalNameSetter);
						} else if ("CableTerminal".equals(strType)) {
							SgCableTerminal cableTerminal = new SgCableTerminal(this.getContext());
							m_mapUIs.put(strID, cableTerminal);
						} else if ("GND".equals(strType)) {
							SgGND end = new SgGND(this.getContext());
							m_mapUIs.put(strID, end);
						}



						//LightingProtected
						//CableTerminal
						//GND
						else {
							Log.v("Warnning", "暂时不支持类型 = " + strType + "的控件");
							showMsgDlg("警告", "不支持的控件类型： " + strType);
							bExit = false;
						}

						// 控件存在
						if (bExit == true) 
						{
							iCurrentObj = m_mapUIs.get(strID);
							iCurrentObj.setUniqueID(strID);
							iCurrentObj.setType(strType);
							//iCurrentObj.addToRenderWindow(this);
						}
						
					} else if ("Property".equals(name)) {
						
						String strName = pullParser.getAttributeValue("", "Name");
						String strOrigValue = pullParser.getAttributeValue("", "Value");
						
						String strValue = strOrigValue;
						
						if (m_strReplaceX1 != null)
							strValue = replaceString(strOrigValue, "X1", m_strReplaceX1); // 模板字符串替换
						
						if (iCurrentObj == null)
							continue;
						
						// 可以解析的控件
						/* 无意义的判断  -- CharlesChen */ 
						if ("Form".equals(strElementType)
								|| "AlarmLight".equals(strElementType)
								|| "Ammeter".equals(strElementType)
								|| "Button".equals(strElementType)
								|| "CommandButton".equals(strElementType)
								|| "Ellipse".equals(strElementType)
								|| "Image".equals(strElementType)
								|| "Label".equals(strElementType)
								|| "TextClock".equals(strElementType)
								|| "Polyline".equals(strElementType)
								|| "YTParameter".equals(strElementType)
								|| "YKParameter".equals(strElementType)
								|| "Rectangle".equals(strElementType)
								|| "StatePanel".equals(strElementType)
								|| "StraightLine".equals(strElementType)
								|| "Table".equals(strElementType)
								|| "TextBox".equals(strElementType)
								|| "Thermometer".equals(strElementType)
								|| "EventList".equals(strElementType)
								|| "SignalList".equals(strElementType)
								|| "ThreeDPieChart".equals(strElementType)
								|| "MultiChart".equals(strElementType)
								|| "HistorySignalCurve".equals(strElementType)
								|| "IsolationSwitch".equals(strElementType)
								|| "DoubleImageButton".equals(strElementType)
								|| "SignalNameSetter".equals(strElementType)
							    || "EventConditionStartSetter".equals(strElementType)
							    || "CableTerminal".equals(strElementType)
							    || "GND".equals(strElementType))
						{
							try
							{
								iCurrentObj.parseProperties(strName, strValue,
									m_strResFolder);
							}
							catch(Throwable e)
							{
								e.printStackTrace();
								showMsgDlg("错误", "解析参数失败！\n\n参数名： [ " + strName 
										+ " ]\n参数值： [ " + strValue 
										+ " ]\n\n页面名称： [ " + m_strCurrentPage 
										+ " ]\n控件类型： [ " + strElementType 
										+ " ]\n资源路径： [ " + m_strResFolder 
										+ " ]\n\n错误信息：\n" + e.toString());
								
								/*
								// 控制 Dialog 数量，避免溢出死机。
								if (++NUMOFDAILOG < 10 && MGridActivity.m_bErrMsgParser)
								{
									new AlertDialog.Builder(m_oMgridActivity).setTitle("错误") .setMessage("解析参数失败！\n\n参数名： [ " + strName 
											+ " ]\n参数值： [ " + strValue 
											+ " ]\n\n页面名称： [ " + m_strCurrentPage 
											+ " ]\n控件类型： [ " + strElementType 
											+ " ]\n资源路径： [ " + m_strResFolder 
											+ " ]\n\n错误信息：\n" + e.toString()) .show();
								}
								else if (NUMOFDAILOG == 10 && MGridActivity.m_bErrMsgParser)
								{
									new AlertDialog.Builder(m_oMgridActivity).setTitle("无法显示更多错误信息") .setMessage("该组态配置中错误过多！！！") .show();
								}
								//*/
							} // end of catch
						}
					}  /*  else if ("Property".equals(name)) */
				}  /* if (eventType == XmlPullParser.START_TAG) */

				try 
				{
					eventType = pullParser.next();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				
			}  /* end of while */

		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		
	}  /* end of parseStream(InputStream inStream) */

	/** 判断一个矩形框是否在屏幕内 */
	public boolean isLayoutVisible(Rect bb) {
		if (bb.left < 0 && bb.right <= 0)
			return false;
		if (bb.top < 0 && bb.bottom <= 0)
			return false;
    	if (bb.left >= MainWindow.SCREEN_WIDTH && bb.right > MainWindow.SCREEN_WIDTH)
    		return false;
    	if (bb.top >= MainWindow.SCREEN_WIDTH && bb.bottom > MainWindow.SCREEN_WIDTH)
    		return false;

		return true;
	}
	
	// TODO: 通知列表框充填数据 解决列表不正确layout问题
	/*
	boolean listinited = false;
	public void notifylistflush()
	{
		if (listinited) return;
		listinited = true;
		
		boolean hasupdate = false;
		Iterator<Hashtable.Entry<String, IObject>> it = m_mapUIs.entrySet().iterator();
		while(it.hasNext())
		{
			Hashtable.Entry<String, IObject> entry = it.next();
			if (entry.getValue().getType().equals("EventList"))
			{
				//entry.getValue().needupdate(true);
				m_oShareObject.m_listUpdateFromTcpValues.add(entry.getValue());
				hasupdate = true;
			}
			else if (entry.getValue().getType().equals("SignalList"))
			{
				//entry.getValue().needupdate(true);
				m_oShareObject.m_listUpdateFromTcpValues.add(entry.getValue());
				hasupdate = true;
			}
		}
		
		if (hasupdate) m_oInvalidateHandler.sendEmptyMessage(0);
	}
	*/

	// Params:
	static public int MAXZINDEX = 0;
	static public int FORM_WIDTH = 0;
	static public int FORM_HEIGHT = 0;
	static public int SWITCH_STYLE = 0;
	HashMap<String, IObject> m_mapUIs = null;

	String m_strResFolder = "/";
	String m_strRootFolder = "/"; // 指定文件夹,如 "/ShangeAndroidRes/"

	public int VIEW_LEFT = 0;
	public int VIEW_RIGHT = 0;
	public int VIEW_TOP = 0;
	public int VIEW_BOTTOM = 0;
	public static int SCREEN_WIDTH = 0;
	public static int SCREEN_HEIGHT = 0;
	// scale
	float m_fScale = 1.0f;

	Handler m_oInvalidateHandler = null;
	boolean m_bFirstSetLayout = true;

	boolean m_bOneFigerDown = false;
	boolean m_bTwoFigerDown = false;
	boolean m_bHadTowFiger  = false;  // 记录是否有多个指示点，消除缩放时误切页。
	float m_fStartScaleValue = 1.0f;
	float m_fOffsetX = 0.0f;
	float m_fOffsetY = 0.0f;
	float m_fOldX = 0.0f;
	float m_fOldY = 0.0f;
	float m_fDragStartX = 0.0f;
	float m_fDragStartY = 0.0f;
	float m_fDragEndX = 0.0f;
	float m_fDragEndY = 0.0f;

	public boolean m_bHasRandomData = false;
	public boolean m_bIsActive = false; // 对应的界面是否处于最前
	public MutiThreadShareObject m_oShareObject = null; // 采集数据共享区
	MGridActivity m_oMgridActivity = null;

	String m_strReplaceX1 = null; 

	SgExpressionCacularThread m_oCaculateThread = null;
	
	// 记录弹出框的数目，避免对话框过多引起当机。
	static private short NUMOFDAILOG = 0; 

	/** 子线程->处理数据 */
	public class SgExpressionCacularThread extends Thread {

		public SgExpressionCacularThread() {
			m_mapExpression = new HashMap<String, stExpression>();
			m_mapCaculateValues = new HashMap<String, String>();
			m_oCalculator = new Calculator(); // 用于计算数学表达式
			m_bIsRunning = true;
		}

		/** 把UI无关的数据添加到这个线程来处理 */
		public void addExpression(String strUniqueID, String strUiType, String strExpression) {
			stExpression oMathExpress = UtExpressionParser.getInstance().parseExpression(strExpression);
			if (oMathExpress != null) {
				oMathExpress.strUiType = strUiType;
				m_mapExpression.put(strUniqueID, oMathExpress);
				
				// TODO: 向数据模型注册信号
				Iterator<HashMap.Entry<String, stBindingExpression>> it = oMathExpress.mapObjectExpress.entrySet().iterator();
				while(it.hasNext())
				{
					stBindingExpression oBindingExpression = it.next().getValue();
					
					if (oMathExpress.strBindType.equals("Value"))
					{
						// 普通信号
						DataGetter.setSignal(oBindingExpression.nEquipId, oBindingExpression.nSignalId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
					}
					else if (oMathExpress.strBindType.equals("EventSeverity"))
					{
						// 告警控件
						DataGetter.setAlarmSignal(oBindingExpression.nEquipId, oBindingExpression.nSignalId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
					}
					else if (oMathExpress.strBindType.equals("Equip"))
					{
						if (strUiType.equals("SignalList"))
						{
							// 普通设备信号集合控件
							DataGetter.setSignalList(oBindingExpression.nEquipId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
						}
						else if (strUiType.equals("EventList"))
						{
							// 全局实时告警列表控件
							DataGetter.setMainAlarmList(m_mapUIs.get(strUniqueID));
						}
					}
					else if (oMathExpress.strBindType.equals("Name"))
					{
						// 非法设备ID
						if (0 == oBindingExpression.nEquipId)
							continue;
						
						if (0<oBindingExpression.nSignalId && 1>oBindingExpression.nEventId)
							DataGetter.regSignalName(oBindingExpression.nEquipId, oBindingExpression.nSignalId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
						else if (1>oBindingExpression.nSignalId && 0<oBindingExpression.nEventId)
							DataGetter.regEventName(oBindingExpression.nEquipId, oBindingExpression.nSignalId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
					}
					// TODO: ...
				}
			}
		}

		/** 将数据写入共享对象-->主线程会去访问这些数据 */
		/** 写入MutiChart 数据 */
		public void pushMutiChartDatas(String strKey, stExpression oExpression) {
			String strMutiChartKey = "";
			Iterator<String> iterMuti = oExpression.mapObjectExpress.keySet().iterator();
			List<String> listCharts = new ArrayList<String>();
			while (iterMuti.hasNext()) {
				strMutiChartKey = iterMuti.next();
				stBindingExpression bindingExpression = oExpression.mapObjectExpress.get(strMutiChartKey);
    			if ("Value".equals(bindingExpression.strBindType)) {
    				listCharts.add(DataGetter.getSignalValue(bindingExpression.nEquipId, bindingExpression.nSignalId));
    				//listCharts.add(service.get_signal_data(service.IP, service.PORT, bindingExpression.nEquipId, bindingExpression.nSignalId));
    			}
			}
			m_oShareObject.m_mapMutiChartDatas.put(strKey, listCharts);
		}

		/** 写入名称数据 */
		private void pushSignalName(String strUniqueId, stExpression oMathExpression) {
			if (oMathExpression == null) return;

			String strRetValue = "";
			String strKey = "";
			Iterator<String> iter = oMathExpression.mapObjectExpress.keySet().iterator();
			if (iter.hasNext())
				strKey = iter.next();
			stBindingExpression oFirstBindingExp = oMathExpression.mapObjectExpress.get(strKey);
			if (oFirstBindingExp == null)
				return;
			
			if (0<oFirstBindingExp.nSignalId && 1>oFirstBindingExp.nEventId)
				strRetValue = DataGetter.getSignalName(oFirstBindingExp.nEquipId, oFirstBindingExp.nSignalId);
			else if (1>oFirstBindingExp.nSignalId && 0<oFirstBindingExp.nEventId)
				strRetValue = DataGetter.getEventName(oFirstBindingExp.nEquipId, oFirstBindingExp.nEventId);
			else
				return;

			SgRealTimeData oRealTimeData = new SgRealTimeData();
			oRealTimeData.nDataType = oFirstBindingExp.nValueType;
			oRealTimeData.strValue = strRetValue;
			m_oShareObject.m_mapRealTimeDatas.put(strUniqueId, oRealTimeData);
		}
		
		/** 写入实时数据 */
		private void pushRealTimeValue(String strUniqueId, stExpression oMathExpression) {
			if (oMathExpression == null) return;

			String strRetValue = "";
			String strKey = "";
			Iterator<String> iter = oMathExpression.mapObjectExpress.keySet().iterator();
			if (iter.hasNext())
				strKey = iter.next();
			stBindingExpression oFirstBindingExp = oMathExpression.mapObjectExpress.get(strKey);
			if (oFirstBindingExp == null)
				return;

			// 数值型的实时值->需要计算数学表达式
			if (oFirstBindingExp.nValueType == 0
					|| oFirstBindingExp.nValueType == 2
					|| oFirstBindingExp.nValueType == 3
					|| oFirstBindingExp.nValueType == 4)
			{
				String strMathExpression = "";
				int nSize = oMathExpression.listMathExpress.size();
				for (int i = 0; i < nSize; ++i)
				{
					String strStr = oMathExpression.listMathExpress.get(i);

					if (strStr.length() != 1)
					{
						stBindingExpression oExpress = oMathExpression.mapObjectExpress.get(strStr);
						if (nSize > 1)
						{
							strStr = DataGetter.getSignalValue(oExpress.nEquipId, oExpress.nSignalId);
						}
						else if (m_mapUIs.get(strUniqueId) instanceof SgIsolationSwitch)
						{
							// m_mapUIs.get(strUniqueId) instanceof SgIsolationSwitch
							// SgIsolationSwitch.class.isInstance(m_mapUIs.get(strUniqueId))
							strStr = DataGetter.getSignalValue(oExpress.nEquipId, oExpress.nSignalId);
						}
						else
						{
							strStr = getRealTimeValueFromTcp(oExpress);
						}
					}

					strMathExpression += strStr;
				}

				// 如果nSize<=1，无需计算数学表达式
				if ("".equals(strMathExpression) == false && nSize > 1)
				{
			    	try {
			    		strRetValue = m_oCalculator.calculate(strMathExpression) + "";
			    	} catch(Exception e) {
			    		 Log.v("Warnning", "MathExpression = ["+strMathExpression+"] can not calculate！"+" EqId_SignalId=("+oFirstBindingExp.nEquipId + "," + oFirstBindingExp.nSignalId +")");
			    	}
				} else {
					strRetValue = strMathExpression;
				}
			}
			else
			{ // 非数值的直接返回
				strRetValue = getRealTimeValueFromTcp(oFirstBindingExp);
			}

			SgRealTimeData oRealTimeData = new SgRealTimeData();
			oRealTimeData.nDataType = oFirstBindingExp.nValueType;
			oRealTimeData.strValue = strRetValue;
			m_oShareObject.m_mapRealTimeDatas.put(strUniqueId, oRealTimeData);
		}
		
		/** 获取历史曲线 数据 */
		public List<ipc_history_signal> getHistorySinals(stExpression oMathExpression) {
			if (oMathExpression == null)
				return null;
			
			String strKey = "";
			Iterator<String> iter = oMathExpression.mapObjectExpress.keySet().iterator();
			if (iter.hasNext())
	        	strKey = iter.next();
	       return service.get_history_signal_list(service.IP, service.PORT, oMathExpression.mapObjectExpress.get(strKey).nEquipId);
		}
		
		/** 获取实时告警列表 */
		private Hashtable<String, Hashtable<String, Event>> getActiveEvents(stExpression oMathExpression) {
			if (oMathExpression == null)
				return null;
			
	       return DataGetter.getRTEventList();
		}
		
		/** 获取实时信号列表 */
		private Hashtable<String, Signal> getActiveSignals(stExpression oMathExpression) {
			if (oMathExpression == null)
				return null;
			
			String strKey = "";
			Iterator<String> iter = oMathExpression.mapObjectExpress.keySet().iterator();
			if (iter.hasNext())
	        	strKey = iter.next();
	       return DataGetter.getEquipSignalList(oMathExpression.mapObjectExpress.get(strKey).nEquipId);
		}

		/** 自动销毁线程 */
		public void autoDestroy() {
			m_bIsRunning = false;
		}

		@Override
		public void run() {
			m_oCaculateThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			
			// 判断是否有绑定控件，如无任何需处理控件则处理线程退出。
			if (m_mapExpression.isEmpty()) return;
			
			// Cmd list
			HashMap<String, stExpression> mapCmds = new HashMap<String, stExpression>();

			// Trigger list
			HashMap<String, stExpression> mapTriggers = new HashMap<String, stExpression>();
			
			// Signel list
			HashMap<IObject, stExpression> mapSignals = new HashMap<IObject, stExpression>();
			
			// EventSeverity list
			
			// Name configure list
			HashMap<String, stExpression> mapNamings = new HashMap<String, stExpression>();

			// 把数据拆分到各个map
			Iterator<HashMap.Entry<String, stExpression>> exp_it = m_mapExpression.entrySet().iterator();
			while (exp_it.hasNext())
			{
				HashMap.Entry<String, stExpression> entry = exp_it.next();
				String strKey = entry.getKey();
				stExpression oExpression = entry.getValue();
				if (oExpression == null)
					continue;

				if ("Cmd".equals(oExpression.strBindType))
				{
					mapCmds.put(strKey, oExpression);
					continue;
				}else if ("Trigger".equals(oExpression.strBindType) || "Mask".equals(oExpression.strBindType))
				{
					mapTriggers.put(strKey, oExpression);
					continue;
				}else if ("Naming".equals(oExpression.strBindType) || "SignalNameSetter".equals(oExpression.strUiType))
				{
					mapNamings.put(strKey, oExpression);
					continue;
				} else
				{
					mapSignals.put(m_mapUIs.get(strKey), oExpression);
				}
			}  // end of while (exp_it.hasNext())

			try {
				
				// 记录列表控件数据更新时间倍率
				int listneedupdate = 10;
				int curveneedupdate = 7;

				// 线程 while 循环
				while (m_bIsRunning) {

					// 对于非活动页面的后台线程，使进入慢速运作状态。  --  CharlesChen
					if (!m_bIsActive) {
						synchronized (MainWindow.this) {
							MainWindow.this.wait(5000);
						}
						
						// 对非活动页面彻底休眠该线程。
						continue;
					}

					if (m_oShareObject.m_listUpdateFromTcpValues.size() > 0) // 主线程是否已经处理完毕
					{
						yield();  // 切出CPU时间片代替死循环等待  -- CharlesChen
						continue;
					}
					
					boolean hasupdate = false;
					Iterator<HashMap.Entry<IObject, stExpression>> iter = mapSignals.entrySet().iterator();
					while (iter.hasNext()) 
					{
						HashMap.Entry<IObject, stExpression> entry = iter.next();
						if (!entry.getKey().needupdate()) continue;
						
						String strKey = entry.getKey().getUniqueID();
						stExpression oExpression = entry.getValue();
						if (oExpression == null) continue;

						if ("Equip".equals(oExpression.strBindType))
						{
							if ("EventList".equals(oExpression.strUiType))
							{
								m_oShareObject.m_mapEventListDatas.put(strKey, getActiveEvents(oExpression));
							} else if ("SignalList".equals(oExpression.strUiType))
							{
								// 是否到达更新时间
								if (--listneedupdate != 0) continue;
								else listneedupdate = 10;
								
								m_oShareObject.m_mapSignalListDatas.put(strKey, getActiveSignals(oExpression));
							}
						}else if ("Name".equals(oExpression.strBindType))
						{
							pushSignalName(strKey, oExpression);
						} else // 处理实时值
						{
							// mutichart 处理
							if ("HistorySignalCurve".equals(oExpression.strUiType) || "ThreeDPieChart".equals(oExpression.strUiType)
									|| "MultiChart".equals(oExpression.strUiType))
							{
								// 是否到达更新时间
								if (--curveneedupdate != 0) continue;
								else curveneedupdate = 7;
								
								pushMutiChartDatas(strKey, oExpression);
								
								/*
								// 获取历史曲线数据
								if ("HistorySignalCurve".equals(oExpression.strUiType))
								{
									List<ipc_history_signal> listHistorySignals = getHistorySinals(oExpression);
									List<List<ipc_history_signal>> mutiLines = new ArrayList<List<ipc_history_signal>>();
									for (int i = 0; i < oExpression.mapObjectExpress.size(); ++i)
									{
										mutiLines.add(new ArrayList<ipc_history_signal>());
									}
									for (int i = 0; i < listHistorySignals.size(); ++i)
									{
										ipc_history_signal si = listHistorySignals.get(i);
										int nIndex1 = 0;
										Iterator<String> iterMuti = oExpression.mapObjectExpress.keySet().iterator();
										while (iterMuti.hasNext())
										{
											String strHistorySignalKey = iterMuti.next();
											stBindingExpression bindExp = oExpression.mapObjectExpress.get(strHistorySignalKey);

											if (bindExp.nSignalId == si.sigid)
											{
												mutiLines.get(nIndex1).add(si);
												break;
											}
											nIndex1++;
										}

									}
									m_oShareObject.m_mapHistorySignals.put(strKey, mutiLines);
								}
								*/
							} else
							{
								pushRealTimeValue(strKey, oExpression);
							}
						}  // end of else 处理实时值
						
						if (entry.getKey().updateValue())
						{
							hasupdate = true;
							m_oShareObject.m_listUpdateFromTcpValues.add(entry.getKey()); // 添加到需要更新UI列表
						}
					}  /* end of while (iter.hasNext()) */

					// 处理 CMD 指令
					m_oShareObject.processCmdCommands(mapCmds);

					// 处理 Trigger 指令
					m_oShareObject.processTriggerCommands(mapTriggers);
					
					// 处理 Naming 指令
					m_oShareObject.processNamingCommands(mapNamings);

					// 发送界面刷新的消息
					//if (m_bIsActive && hasupdate) m_oInvalidateHandler.sendEmptyMessage(0);
					if (hasupdate) m_oInvalidateHandler.sendEmptyMessage(0);

					Thread.sleep(300);
				} /* end of while (m_bIsRunning) */
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// will stop
			m_mapExpression.clear();
			m_mapCaculateValues.clear();
			m_oCalculator = null;
		}  /* end of run() */

		/** 是否使用随机数据 */
		public void setHasRandomData(boolean bHasRandom) {
			m_bHasRandomData = bHasRandom;
		}

		/** 从TCP层获取实时数据 */
		String getRealTimeValueFromTcp(stBindingExpression bindingExpression) {
			// 如何需要生产随即数据
			if (m_bHasRandomData == true)
			{
				Random rand = new Random();
				return "1" + rand.nextInt(99) + 1;
			}
			
			if ("Value".equals(bindingExpression.strBindType))
			{
				String value = DataGetter.getSignalMeaning(bindingExpression.nEquipId, bindingExpression.nSignalId);
				if (!value.isEmpty()) return value;
			}
			else if ("EventSeverity".equals(bindingExpression.strBindType))
			{
				return String.valueOf(DataGetter.getSignalSeverity(
								bindingExpression.nEquipId,
								bindingExpression.nSignalId));
			}
			else if ("State".equals(bindingExpression.strBindType))
			{
				return DataGetter.getSignalMeaning(bindingExpression.nEquipId, 10001);
			}

			return "-999999";
		}

		public HashMap<String, stExpression> m_mapExpression = null;  // <IObject UniqueID, stExpression>
		HashMap<String, String> m_mapCaculateValues = null;
		public Calculator m_oCalculator = null;
		boolean m_bIsRunning = true;
		boolean m_bHasRandomData = false;
	}
}