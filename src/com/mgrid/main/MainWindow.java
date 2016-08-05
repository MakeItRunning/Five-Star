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
/** ������ */
public class MainWindow extends ViewGroup {

	public MainWindow(final MGridActivity context) {
		super(context);
		setFocusableInTouchMode(true);
		m_mapUIs = new HashMap<String, IObject>();
		m_oShareObject = new MutiThreadShareObject(); // ���ݳ�
		m_YKobj = new ArrayList<ViewGroup>();
		m_oMgridActivity = context;

		m_oInvalidateHandler = new Handler() {
			// ���յ���Ϣ����
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					updateWidgets();
					break;
					
				case 1:
					Toast.makeText(context, (String)msg.obj, Toast.LENGTH_SHORT).show();
					break;
					
				case 2:
					new AlertDialog.Builder(context).setTitle("����") .setMessage((String)msg.obj) .show();
					break;
				}
				
				super.handleMessage(msg);

				/* �����߳������Ȼ��Sleep������
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

	/** ���±仯�Ľ������� */
	void updateWidgets() {
		// TODO: ��������Ƿ�Ϊ��ȷ���Ŵ��ĳߴ磿 Ϊ���·Ŵ���UI�ṩ��λ���ݡ�  -- CharlesChen
		// ������ڴ�С�ı���
		

		MainWindow.SCREEN_WIDTH = this.getResources().getDisplayMetrics().widthPixels;
		MainWindow.SCREEN_HEIGHT = this.getResources().getDisplayMetrics().heightPixels;

		// TODO: �˵��߼�����Ӧ�÷ŵ����߳��У����߳̽��ӹ������ȡ��������ø���  -- CharlesChen
		for (int i = 0; i < m_oShareObject.m_listUpdateFromTcpValues.size(); ++i) {
			IObject obj = m_oShareObject.m_listUpdateFromTcpValues.get(i);
			if (obj != null) {
				obj.updateWidget();
			}
		}
		m_oShareObject.clearFromTcpValue(); // ������Ҫ�Լ����->�Ա������߳�д���µ�����

		/* �Լ����������ȵģ�����˭��  -- CharlesChen
		// ��������Ȩ
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		*/
	}

	/** �滻�ַ��� */
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
	/** ������Ӧ */
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
			// �ɼ����ٶ�
			if (mVelocityTracker != null) {
				mVelocityTracker.addMovement(event);
			}
			
			if (m_bTwoFigerDown == true) { // ����
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
			} else if (m_bOneFigerDown == true) { // ����
				
				if (m_fScale <= 1.0f) {
					// TODO: ����������Ӧ���뻭�������ָ������  -- CharlesChen
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
			m_bTwoFigerDown = false;  //  ���������Ų���ʱ����ҳ  -- CharlesChen
			break;

		case MotionEvent.ACTION_UP:
        	this.requestFocus();
			m_oMgridActivity.mImm.hideSoftInputFromWindow(this.getWindowToken(), 0);  // �����κο��ܳ��ֵ������
			
			/*
			boolean isOpen = m_oMgridActivity.mImm.isActive();
			if (isOpen && !m_bHadTowFiger)
			{
				m_oMgridActivity.mImm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);  // �����κο��ܳ��ֵ������
			}
			*/
			
			m_bOneFigerDown = false;
			m_fDragEndX = event.getX();
			m_fDragEndY = event.getY();
			if (m_fScale <= 1.0f && !m_bHadTowFiger) { // ������ ���� m_bHadTowFiger ���������������  -- CharlesChen
				
				// ͨ�����ٶ��ж��Ƿ���ҳ������ʹ�þ�����㡣  -- CharlesChen
				int velocityX = 0;
				int velocityY = 0;
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(event);
					mVelocityTracker.computeCurrentVelocity(1000);
					//�õ���ָ�ƶ��ٶ�
					velocityX = (int) mVelocityTracker.getXVelocity(); // X�᷽��
					velocityY = (int) mVelocityTracker.getYVelocity(); // Y�᷽��
					
					// �ͷ���Դ
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				
				// �ж��¶�
				if (m_fDragEndX-m_fDragStartX == 0 || Math.abs((m_fDragEndY-m_fDragStartY)/(m_fDragEndX-m_fDragStartX)) > 0.6) break;
				
				// �жϼ��ٶ��ݺ��
				if (velocityY > 300 && velocityY != 0 && Math.abs(velocityX/velocityY) < 2.8) break;
				
				//velocityXΪ��ֵ˵����ָ���һ�����Ϊ��ֵ˵����ָ���󻬶�
				if (velocityX > SNAP_VELOCITY && null != m_oPrevPage) {
					// ����
					m_oMgridActivity.onPageChange(m_oPrevPage.m_strCurrentPage);
				} else if (velocityX < -SNAP_VELOCITY && null != m_oNextPage) {
					// ���һ�
					m_oMgridActivity.onPageChange(m_oNextPage.m_strCurrentPage);
				} else {
					// ����
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
			//m_oMgridActivity.mImm.hideSoftInputFromWindow(this.getWindowToken(), 0);  // �����κο��ܳ��ֵ������
			
		default:
			break;
		}
		
		return false;
	}
	
	// ������� ViewGroup Ƕ���¼���Ӧ���⡣ TODO: ��������Ӱ���д��۲�  -- CharlesChen
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

	/** ��������֮��ľ������� **/
	@SuppressLint({ "NewApi", "FloatMath" })
	/** ��ȡ����ĵ����� - ǰ�������� */
	private float distance(MotionEvent e) {
		float eX = e.getX(1) - e.getX(0); // ����ĵ����� - ǰ��������
		float eY = e.getY(1) - e.getY(0);
		return FloatMath.sqrt(eX * eX + eY * eY);
	}

	/** ����һ���µ�ҳ�� 
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

	/** ж�ص�ǰҳ�� */
	protected void unloadPage() {
		m_oCaculateThread.autoDestroy(); // ���߳��Զ�����
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

	/** �л�ҳ��xml */
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
			
			// ѡ����ҳЧ��
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
			new AlertDialog.Builder(m_oMgridActivity).setTitle("�޷���ʾ���������Ϣ") .setMessage("����̬�����д�����࣡����") .show();
		}
	}

	public void showTaskUI(boolean bShow) {
		m_oMgridActivity.showTaskUI(bShow);
	}

	// CharlesChen
	private VelocityTracker mVelocityTracker; // �����ж�˦������
	private static final int SNAP_VELOCITY = 600;   //X���ٶȻ�ֵ�����ڸ�ֵʱ�����л�
	
	public String m_strCurrentPage = "";
	public MainWindow m_oPrevPage = null;
	public MainWindow m_oNextPage = null;
	public List<ViewGroup> m_YKobj = null;  // ��¼��Ҫ������ڲ� ViewGroup �ؼ�

	@Override
	/** ��дlayout */
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

	/** ����XML 
	 * @throws FileNotFoundException */
	public void parseXml(String xmlFile) throws FileNotFoundException {
		String[] arrStr = xmlFile.split("\\.");
		m_strResFolder = m_strRootFolder + arrStr[0] + ".files/";

		// ���ǵ�����֪ͨ�ڴ˴������������׳��쳣���沶��
		//try {
		
			InputStream is = new BufferedInputStream(new FileInputStream(
					Environment.getExternalStorageDirectory().getPath()
							+ m_strRootFolder + xmlFile));
			parseStream(is);
			
		// ��ӿؼ���ҳ
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

	/** ����XML�е����пؼ� */
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
							Log.v("Warnning", "��ʱ��֧������ = " + strType + "�Ŀؼ�");
							showMsgDlg("����", "��֧�ֵĿؼ����ͣ� " + strType);
							bExit = false;
						}

						// �ؼ�����
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
							strValue = replaceString(strOrigValue, "X1", m_strReplaceX1); // ģ���ַ����滻
						
						if (iCurrentObj == null)
							continue;
						
						// ���Խ����Ŀؼ�
						/* ��������ж�  -- CharlesChen */ 
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
								showMsgDlg("����", "��������ʧ�ܣ�\n\n�������� [ " + strName 
										+ " ]\n����ֵ�� [ " + strValue 
										+ " ]\n\nҳ�����ƣ� [ " + m_strCurrentPage 
										+ " ]\n�ؼ����ͣ� [ " + strElementType 
										+ " ]\n��Դ·���� [ " + m_strResFolder 
										+ " ]\n\n������Ϣ��\n" + e.toString());
								
								/*
								// ���� Dialog �������������������
								if (++NUMOFDAILOG < 10 && MGridActivity.m_bErrMsgParser)
								{
									new AlertDialog.Builder(m_oMgridActivity).setTitle("����") .setMessage("��������ʧ�ܣ�\n\n�������� [ " + strName 
											+ " ]\n����ֵ�� [ " + strValue 
											+ " ]\n\nҳ�����ƣ� [ " + m_strCurrentPage 
											+ " ]\n�ؼ����ͣ� [ " + strElementType 
											+ " ]\n��Դ·���� [ " + m_strResFolder 
											+ " ]\n\n������Ϣ��\n" + e.toString()) .show();
								}
								else if (NUMOFDAILOG == 10 && MGridActivity.m_bErrMsgParser)
								{
									new AlertDialog.Builder(m_oMgridActivity).setTitle("�޷���ʾ���������Ϣ") .setMessage("����̬�����д�����࣡����") .show();
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

	/** �ж�һ�����ο��Ƿ�����Ļ�� */
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
	
	// TODO: ֪ͨ�б��������� ����б���ȷlayout����
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
	String m_strRootFolder = "/"; // ָ���ļ���,�� "/ShangeAndroidRes/"

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
	boolean m_bHadTowFiger  = false;  // ��¼�Ƿ��ж��ָʾ�㣬��������ʱ����ҳ��
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
	public boolean m_bIsActive = false; // ��Ӧ�Ľ����Ƿ�����ǰ
	public MutiThreadShareObject m_oShareObject = null; // �ɼ����ݹ�����
	MGridActivity m_oMgridActivity = null;

	String m_strReplaceX1 = null; 

	SgExpressionCacularThread m_oCaculateThread = null;
	
	// ��¼���������Ŀ������Ի���������𵱻���
	static private short NUMOFDAILOG = 0; 

	/** ���߳�->�������� */
	public class SgExpressionCacularThread extends Thread {

		public SgExpressionCacularThread() {
			m_mapExpression = new HashMap<String, stExpression>();
			m_mapCaculateValues = new HashMap<String, String>();
			m_oCalculator = new Calculator(); // ���ڼ�����ѧ���ʽ
			m_bIsRunning = true;
		}

		/** ��UI�޹ص�������ӵ�����߳������� */
		public void addExpression(String strUniqueID, String strUiType, String strExpression) {
			stExpression oMathExpress = UtExpressionParser.getInstance().parseExpression(strExpression);
			if (oMathExpress != null) {
				oMathExpress.strUiType = strUiType;
				m_mapExpression.put(strUniqueID, oMathExpress);
				
				// TODO: ������ģ��ע���ź�
				Iterator<HashMap.Entry<String, stBindingExpression>> it = oMathExpress.mapObjectExpress.entrySet().iterator();
				while(it.hasNext())
				{
					stBindingExpression oBindingExpression = it.next().getValue();
					
					if (oMathExpress.strBindType.equals("Value"))
					{
						// ��ͨ�ź�
						DataGetter.setSignal(oBindingExpression.nEquipId, oBindingExpression.nSignalId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
					}
					else if (oMathExpress.strBindType.equals("EventSeverity"))
					{
						// �澯�ؼ�
						DataGetter.setAlarmSignal(oBindingExpression.nEquipId, oBindingExpression.nSignalId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
					}
					else if (oMathExpress.strBindType.equals("Equip"))
					{
						if (strUiType.equals("SignalList"))
						{
							// ��ͨ�豸�źż��Ͽؼ�
							DataGetter.setSignalList(oBindingExpression.nEquipId, m_strCurrentPage, m_mapUIs.get(strUniqueID));
						}
						else if (strUiType.equals("EventList"))
						{
							// ȫ��ʵʱ�澯�б�ؼ�
							DataGetter.setMainAlarmList(m_mapUIs.get(strUniqueID));
						}
					}
					else if (oMathExpress.strBindType.equals("Name"))
					{
						// �Ƿ��豸ID
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

		/** ������д�빲�����-->���̻߳�ȥ������Щ���� */
		/** д��MutiChart ���� */
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

		/** д���������� */
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
		
		/** д��ʵʱ���� */
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

			// ��ֵ�͵�ʵʱֵ->��Ҫ������ѧ���ʽ
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

				// ���nSize<=1�����������ѧ���ʽ
				if ("".equals(strMathExpression) == false && nSize > 1)
				{
			    	try {
			    		strRetValue = m_oCalculator.calculate(strMathExpression) + "";
			    	} catch(Exception e) {
			    		 Log.v("Warnning", "MathExpression = ["+strMathExpression+"] can not calculate��"+" EqId_SignalId=("+oFirstBindingExp.nEquipId + "," + oFirstBindingExp.nSignalId +")");
			    	}
				} else {
					strRetValue = strMathExpression;
				}
			}
			else
			{ // ����ֵ��ֱ�ӷ���
				strRetValue = getRealTimeValueFromTcp(oFirstBindingExp);
			}

			SgRealTimeData oRealTimeData = new SgRealTimeData();
			oRealTimeData.nDataType = oFirstBindingExp.nValueType;
			oRealTimeData.strValue = strRetValue;
			m_oShareObject.m_mapRealTimeDatas.put(strUniqueId, oRealTimeData);
		}
		
		/** ��ȡ��ʷ���� ���� */
		public List<ipc_history_signal> getHistorySinals(stExpression oMathExpression) {
			if (oMathExpression == null)
				return null;
			
			String strKey = "";
			Iterator<String> iter = oMathExpression.mapObjectExpress.keySet().iterator();
			if (iter.hasNext())
	        	strKey = iter.next();
	       return service.get_history_signal_list(service.IP, service.PORT, oMathExpression.mapObjectExpress.get(strKey).nEquipId);
		}
		
		/** ��ȡʵʱ�澯�б� */
		private Hashtable<String, Hashtable<String, Event>> getActiveEvents(stExpression oMathExpression) {
			if (oMathExpression == null)
				return null;
			
	       return DataGetter.getRTEventList();
		}
		
		/** ��ȡʵʱ�ź��б� */
		private Hashtable<String, Signal> getActiveSignals(stExpression oMathExpression) {
			if (oMathExpression == null)
				return null;
			
			String strKey = "";
			Iterator<String> iter = oMathExpression.mapObjectExpress.keySet().iterator();
			if (iter.hasNext())
	        	strKey = iter.next();
	       return DataGetter.getEquipSignalList(oMathExpression.mapObjectExpress.get(strKey).nEquipId);
		}

		/** �Զ������߳� */
		public void autoDestroy() {
			m_bIsRunning = false;
		}

		@Override
		public void run() {
			m_oCaculateThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			
			// �ж��Ƿ��а󶨿ؼ��������κ��账��ؼ������߳��˳���
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

			// �����ݲ�ֵ�����map
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
				
				// ��¼�б�ؼ����ݸ���ʱ�䱶��
				int listneedupdate = 10;
				int curveneedupdate = 7;

				// �߳� while ѭ��
				while (m_bIsRunning) {

					// ���ڷǻҳ��ĺ�̨�̣߳�ʹ������������״̬��  --  CharlesChen
					if (!m_bIsActive) {
						synchronized (MainWindow.this) {
							MainWindow.this.wait(5000);
						}
						
						// �Էǻҳ�泹�����߸��̡߳�
						continue;
					}

					if (m_oShareObject.m_listUpdateFromTcpValues.size() > 0) // ���߳��Ƿ��Ѿ��������
					{
						yield();  // �г�CPUʱ��Ƭ������ѭ���ȴ�  -- CharlesChen
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
								// �Ƿ񵽴����ʱ��
								if (--listneedupdate != 0) continue;
								else listneedupdate = 10;
								
								m_oShareObject.m_mapSignalListDatas.put(strKey, getActiveSignals(oExpression));
							}
						}else if ("Name".equals(oExpression.strBindType))
						{
							pushSignalName(strKey, oExpression);
						} else // ����ʵʱֵ
						{
							// mutichart ����
							if ("HistorySignalCurve".equals(oExpression.strUiType) || "ThreeDPieChart".equals(oExpression.strUiType)
									|| "MultiChart".equals(oExpression.strUiType))
							{
								// �Ƿ񵽴����ʱ��
								if (--curveneedupdate != 0) continue;
								else curveneedupdate = 7;
								
								pushMutiChartDatas(strKey, oExpression);
								
								/*
								// ��ȡ��ʷ��������
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
						}  // end of else ����ʵʱֵ
						
						if (entry.getKey().updateValue())
						{
							hasupdate = true;
							m_oShareObject.m_listUpdateFromTcpValues.add(entry.getKey()); // ��ӵ���Ҫ����UI�б�
						}
					}  /* end of while (iter.hasNext()) */

					// ���� CMD ָ��
					m_oShareObject.processCmdCommands(mapCmds);

					// ���� Trigger ָ��
					m_oShareObject.processTriggerCommands(mapTriggers);
					
					// ���� Naming ָ��
					m_oShareObject.processNamingCommands(mapNamings);

					// ���ͽ���ˢ�µ���Ϣ
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

		/** �Ƿ�ʹ��������� */
		public void setHasRandomData(boolean bHasRandom) {
			m_bHasRandomData = bHasRandom;
		}

		/** ��TCP���ȡʵʱ���� */
		String getRealTimeValueFromTcp(stBindingExpression bindingExpression) {
			// �����Ҫ�����漴����
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