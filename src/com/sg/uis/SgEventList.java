package com.sg.uis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.mgrid.data.DataGetter;
import com.mgrid.data.EquipmentDataModel.Event;
import com.mgrid.main.MainWindow;
import com.sg.common.IObject;
import com.sg.common.MutiThreadShareObject;
import com.sg.common.UtTable;

import data_model.apk_active_event;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** 事件列表 */
public class SgEventList extends UtTable implements IObject {

	public SgEventList(Context context) {
		super(context);
		m_rBBox = new Rect();
		
		m_listTempEvents = new Hashtable<String, Hashtable<String, Event>>();
		
		/*
		// 随机。。。
		Random rand = new Random(); 
		int nDataCount = rand.nextInt(8) + 2;
		for (int i = 0; i < nDataCount; ++i) {
			apk_active_event e = new apk_active_event();
			e.name = rand.nextInt(12) % 3 == 0 ? "Communication" : "Remote Turn";
			e.starttime = 138996003 + rand.nextInt(3200000);
			e.meaning = rand.nextInt(32) % 5 == 0 ? "Interrupt" : "Alarmed";
			m_listTempEvents.add(e);
		}
		*/
		
		// 表头
		lstTitles = new ArrayList<String>();
		// 名称，含义，开始时间
		lstTitles.add("设备");
		lstTitles.add("告警名称");
		lstTitles.add("开始时间");
		//lstTitles.add("结束时间");
		lstTitles.add("含义");
		
		lstContends = new ArrayList<List<String>>();
	}

	@Override
	public void doLayout(boolean bool, int l, int t, int r, int b) {
		if (m_rRenderWindow == null)
			return;
		int nX = l + (int) (((float)m_nPosX / (float)MainWindow.FORM_WIDTH) * (r-l));
		int nY = t + (int) (((float)m_nPosY / (float)MainWindow.FORM_HEIGHT) * (b-t));
		int nWidth = (int) (((float)(m_nWidth) / (float)MainWindow.FORM_WIDTH) * (r-l));
		int nHeight = (int) (((float)(m_nHeight) / (float)MainWindow.FORM_HEIGHT) * (b-t));
		m_rBBox.left = nX;
		m_rBBox.top = nY;
		m_rBBox.right = nX+nWidth;
		m_rBBox.bottom = nY+nHeight;
		if (m_rRenderWindow.isLayoutVisible(m_rBBox)) {
			notifyTableLayoutChange(nX, nY, nX+nWidth, nY+nHeight);
			
			for (int i = 0; i < m_title.length; ++i)
				m_title[i].layout(nX + i * nWidth / m_title.length, nY-18, nX + i * nWidth / m_title.length + nWidth / m_title.length, nY);
		}
	}

	@Override
	public void addToRenderWindow(MainWindow rWin) {

		this.setClickable(true);
		this.setBackgroundColor(m_cBackgroundColor);
		
		m_bUseTitle = false;
		m_title = new TextView[lstTitles.size()];
		for (int i = 0; i < m_title.length; i++)
		{
			m_title[i] = new TextView(getContext());
			//m_title[i].setTextColor(Color.BLACK);
			//m_title[i].setTextSize(25);
			//m_title[i].setBackgroundColor(Color.GRAY);
			m_title[i].setGravity(Gravity.CENTER);
			m_title[i].setText(lstTitles.get(i));
			rWin.addView(m_title[i]);
		}
		
		m_rRenderWindow = rWin;
		rWin.addView(this);
	}

	@Override
	public void removeFromRenderWindow(MainWindow rWin) {
		m_rRenderWindow = null;
		rWin.removeView(this);
	}

	public void parseProperties(String strName, String strValue, String strResFolder) {
        if ("ZIndex".equals(strName)) {
       	 	m_nZIndex = Integer.parseInt(strValue);
       	    if (MainWindow.MAXZINDEX < m_nZIndex) MainWindow.MAXZINDEX = m_nZIndex;
        }
        else if ("Location".equals(strName)) {
       	 	String[] arrStr = strValue.split(",");
       	 	m_nPosX = Integer.parseInt(arrStr[0]);
       	 	m_nPosY = Integer.parseInt(arrStr[1]);

       	 	// 设定列表坐标参数
			m_nLeft = m_nPosX;
			m_nTop  = m_nPosY;
			m_nRight  = m_nLeft + m_nTableWidth;
			m_nBottom = m_nTop + m_nTableHeight;
        }
        else if ("Size".equals(strName)) {
       	 	String[] arrSize = strValue.split(",");
       	 	m_nWidth = Integer.parseInt(arrSize[0]);
       	 	m_nHeight = Integer.parseInt(arrSize[1]);

       	 	// 设定列表坐标参数
			m_nTableWidth  = m_nWidth;
			m_nTableHeight = m_nHeight;
			m_nRight  = m_nLeft + m_nTableWidth;
			m_nBottom = m_nTop + m_nTableHeight;
        }
        else if ("Alpha".equals(strName)) {
       	 	m_fAlpha = Float.parseFloat(strValue);
        }
        else if ("Expression".equals(strName))
        	m_strExpression = strValue;
        else if ("ForeColor".equals(strName)) {
        	m_cForeColor = Color.parseColor(strValue);
        	this.setFontColor(m_cForeColor);
        }
        else if ("BackgroundColor".equals(strName)) {
        	m_cBackgroundColor = Color.parseColor(strValue);
        	this.setBackgroundColor(m_cBackgroundColor);
        }
        else if ("BorderColor".equals(strName)) {
        	m_cBorderColor = Color.parseColor(strValue);
        }
        else if ("OddRowBackground".equals(strName)) {
        	m_cOddRowBackground = Color.parseColor(strValue);
        }
        else if ("EvenRowBackground".equals(strName)) {
        	m_cEvenRowBackground = Color.parseColor(strValue);
        }
	}

	@Override
	public void initFinished()
	{
	}

	public void setUniqueID(String strID) {
		m_strID = strID;
	}
	
	public void setType(String strType) {
		m_strType = strType;
	}
	
	public String getUniqueID() {
		return m_strID;
	}
	
	public String getType() {
		return m_strType;
	}
	
	public String getBindingExpression() {
		return m_strExpression;
	}
	
	public void onDraw(Canvas canvas) {
		if (m_rRenderWindow == null)
			return;
		if (m_rRenderWindow.isLayoutVisible(getBBox()) == false)
			return;
		
		super.onDraw(canvas);
	}
	
	@Override
	public void updateWidget()
	{
		update();
	}
	
	@Override
	public boolean updateValue()
	{
		m_bneedupdate = false;
		
		if (m_rRenderWindow == null) return false;
		
		Hashtable<String, Hashtable<String, Event>> listEvents = null;
		if (m_rRenderWindow.m_bHasRandomData == false)
		{ // 是否用随机数据
			listEvents = m_rRenderWindow.m_oShareObject.m_mapEventListDatas.get(this.getUniqueID());
		} else
		{
			listEvents = m_listTempEvents;
		}
		
		if (listEvents == null) return false;

		// 表数据
		Iterator<Hashtable.Entry<String, Hashtable<String, Event>>> equip_it = listEvents.entrySet().iterator();
		lstContends.clear();
		
		while(equip_it.hasNext())
		{
			Hashtable.Entry<String, Hashtable<String, Event>> entry = equip_it.next();
			
			String equipname = DataGetter.getEquipmentName(entry.getKey());
			Iterator<Hashtable.Entry<String, Event>> it = entry.getValue().entrySet().iterator();
			
			while(it.hasNext())
			{
				Hashtable.Entry<String, Event> event_entry = it.next();
				Event event = event_entry.getValue();
				
				List<String> lstRow = new ArrayList<String>();
				
				lstRow.add(equipname);
				lstRow.add(event.name);
				
				if (m_needsort)
					lstRow.add(String.valueOf(event.starttime*1000));
				else
					lstRow.add(getDate(event.starttime*1000, "yyyy.MM.dd HH:mm:ss"));
				
				lstRow.add(event.meaning+"");
				lstContends.add(lstRow);
			}
		}
		
		// 按时间排序处理
		if (m_needsort)
		{
			class SortByEventTime implements Comparator<Object>
			{
				public int compare(Object o1, Object o2)
				{
					@SuppressWarnings("unchecked")
					List<String> l1 = (List<String>) o1;
					@SuppressWarnings("unchecked")
					List<String> l2 = (List<String>) o2;
					
					Long s1 = Long.valueOf(l1.get(2));
					Long s2 = Long.valueOf(l2.get(2));
					
					return s2.compareTo(s1);
				}
			}

			Collections.sort(lstContends, new SortByEventTime());
			
			// 处理时间
			Iterator<List<String>> it = lstContends.iterator();
			while(it.hasNext())
			{
				List<String> next = it.next();
				String startime = getDate(Long.valueOf(next.get(2)), "yyyy.MM.dd HH:mm:ss");
				next.set(2, startime);
			}
		}
		
		updateContends(lstTitles, lstContends);
		return true;
	}

	@Override
    public boolean needupdate()
    {
	    return m_bneedupdate;
    }
	
	@Override
    public void needupdate(boolean bNeedUpdate)
    {
	    m_bneedupdate = bNeedUpdate;
    }
	
	public View getView() {
		return this;
	}
	
	public int getZIndex()
	{
		return m_nZIndex;
	}
	
	public Rect getBBox() {
		return m_rBBox;
	}
	
// params:
	String m_strID = "";
	String m_strType = ""; 
    int m_nZIndex = 16;
	int m_nPosX = 2;
	int m_nPosY = 103;
	int m_nWidth = 321;
	int m_nHeight = 175;
	float m_fAlpha = 0.8f;
	String m_strExpression = "Binding{[Equip[Equip:115]]}";
	int m_cForeColor = 0xFF00FF00;
	int m_cBackgroundColor = 0xFF000000;
	int m_cBorderColor = 0xFFFFFFFF;
	MainWindow m_rRenderWindow = null;
	Rect m_rBBox = null;
	Hashtable<String, Hashtable<String, Event>> m_listTempEvents = null;
	
	public boolean m_bneedupdate = true;
	
	// 固定标题栏
	TextView[] m_title;
	
	// TODO: 临时代替方案
	boolean m_needsort = true;
	List<String> lstTitles = null;
	List<List<String>> lstContends = null;
}
