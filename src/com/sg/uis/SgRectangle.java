package com.sg.uis;

import com.mgrid.main.MainWindow;
import com.sg.common.SgRealTimeData;
import com.sg.common.IObject;
import com.sg.common.MutiThreadShareObject;
import com.sg.common.UtExpressionParser;
import com.sg.common.UtExpressionParser.stIntervalExpression;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.view.MotionEvent;
import android.view.View;

/** 方型 */
public class SgRectangle extends View implements IObject {
	public SgRectangle(Context context) {  
        super(context);
        this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
        });
        m_oPaint = new Paint(); 
        m_rBBox = new Rect();
    }
	
	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas) {
		if (m_rRenderWindow == null)
			return;
		if (m_rRenderWindow.isLayoutVisible(getBBox()) == false)
			return;
		
		m_oPaint.setColor(m_cBorderColor);
		m_oPaint.setAntiAlias(false); // 设置画笔的锯齿效果
		m_oPaint.setStrokeWidth(m_nBorderWidth);
		m_oPaint.setStyle(Paint.Style.STROKE);
        
        int nWidth = (int) (((float)(m_nWidth) / (float)MainWindow.FORM_WIDTH) * (m_rRenderWindow.VIEW_RIGHT - m_rRenderWindow.VIEW_LEFT));
		int nHeight = (int) (((float)(m_nHeight) / (float)MainWindow.FORM_HEIGHT) * (m_rRenderWindow.VIEW_BOTTOM - m_rRenderWindow.VIEW_TOP));

        canvas.drawRect(m_nBorderWidth, m_nBorderWidth, nWidth-m_nBorderWidth, nHeight-m_nBorderWidth, m_oPaint);   
        
        // 0,0.4,#FFC0C0C0,0,#FF585858,0.5,#FFC0C0C0,1
        // 渐变颜色和渐变点
        if (m_arrGradientColorPos != null) {
		    LinearGradient lg = null;
		    if (m_bIsHGradient) {
		        lg = new LinearGradient(0, nHeight/2, nWidth, nHeight/2, m_arrGradientFillColor, 
		        		m_arrGradientColorPos, TileMode.MIRROR);
		    }
		    else {
		        lg = new LinearGradient(nWidth/2, 0, nWidth/2, nHeight, m_arrGradientFillColor, 
		        		m_arrGradientColorPos, TileMode.MIRROR);      	
		    }
		    m_oPaint.setShader(lg);
        }
        else
        	m_oPaint.setColor(m_cSingleFillColor); // 仅填充单色
        m_oPaint.setStyle(Paint.Style.FILL);  
        canvas.drawRect(0, 0, nWidth, nHeight, m_oPaint);
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
			layout(nX, nY, nX+nWidth, nY+nHeight);
		}
	}
	
	public View getView() {
		return this;
	}
	
	public int getZIndex()
	{
		return m_nZIndex;
	}
	
	@Override
	public void addToRenderWindow(MainWindow rWin) {
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
        }
        else if ("Size".equals(strName)) {
       	 	String[] arrSize = strValue.split(",");
       	 	m_nWidth = Integer.parseInt(arrSize[0]);
       	 	m_nHeight = Integer.parseInt(arrSize[1]);
        }
        else if ("Alpha".equals(strName)) {
       	 	m_fAlpha = Float.parseFloat(strValue);
       	 	m_oPaint.setAlpha((int)(m_fAlpha*255));
        }
        else if ("RotateAngle".equals(strName)) {
       	 	m_fRotateAngle = Float.parseFloat(strValue);
        }
        else if ("BorderColor".equals(strName)) {
        	m_cBorderColor = Color.parseColor(strValue);
        }
        else if ("BorderWidth".equals(strName))
        	m_nBorderWidth = Integer.parseInt(strValue);
        else if ("FillColor".equals(strName)) {
        	String[] arrStr = strValue.split(",");
        	if (arrStr.length == 1) {
        		m_cSingleFillColor = Color.parseColor(strValue);
        	}
        	else {
        		if (Integer.parseInt(arrStr[0]) == 0)
        			m_bIsHGradient = false;
        		else
        			m_bIsHGradient = true;
        		m_fAlpha = Float.parseFloat(arrStr[1]);
        		
        		int nCount = (arrStr.length - 2) / 2;
        		m_arrGradientColorPos = new float[nCount];
        		m_arrGradientFillColor = new int[nCount];
        		int nIndex = 0;
        		for (int i = 2; i < arrStr.length; i += 2) {	
        			int color = Color.parseColor(arrStr[i]);
        			m_arrGradientFillColor[nIndex] = Color.argb((int)(Color.alpha(color)*m_fAlpha), Color.red(color), Color.green(color), Color.blue(color));
        			m_arrGradientColorPos[nIndex] = Float.parseFloat(arrStr[i+1]);
        			nIndex++;
        		}
        	}
        }
        else if ("IsDashed".equals(strName)) {
        	m_bIsDashed = Boolean.parseBoolean(strValue);
        }
        else if ("Radius".equals(strName))
        	m_fRadius = Float.parseFloat(strValue);
        else if ("StateExpression".equals(strName))
        	m_strStateExpression = strValue;
        else if ("Effect".equals(strName))
        	m_strEffect = strValue;
        else if ("ColorExpression".equals(strName)) {
        	m_strColorExpression = strValue;
        	//m_oMathExpression = UtExpressionParser.getInstance().parseExpression(strValue);
        	m_oColorIntervalExpression = UtExpressionParser.getInstance().parseColorIntervalExpression(strValue);
        }
	}

	@Override
	public void initFinished()
	{
	}

	public String getBindingExpression() {
		return m_strColorExpression;
	}
	
	// 设备更新
	public void updateWidget() {
		this.invalidate();
	}
	
	@Override
	public boolean updateValue()
	{
		m_bneedupdate = false;

		if (m_oColorIntervalExpression == null)
			return false;

		SgRealTimeData oRealTimeData = m_rRenderWindow.m_oShareObject.m_mapRealTimeDatas.get(this.getUniqueID());
		if (oRealTimeData == null)
			return false;
		String strValue = oRealTimeData.strValue;
		if (strValue == null || "".equals(strValue) == true)
			return false;
		
		int nValue = 0;
		try {
			nValue = Integer.parseInt(strValue);
		}catch(Exception e) {
			
		}
		
      	if (nValue != m_nSignalValue) {
      		m_nSignalValue = nValue;
	    	for (int i = m_oColorIntervalExpression.listInterval.size()-1; i > 0; --i) {
	    		if (nValue > Integer.parseInt(m_oColorIntervalExpression.listInterval.get(i))) {
	    			m_cSingleFillColor = Integer.parseInt(m_oColorIntervalExpression.listValue.get(i));
	    			break;
	    		}
	    	}
	        
	    	return true;
        }
		
		return false;
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

	public Rect getBBox() {
		return m_rBBox;
	}
	
// params:
	String m_strID = "";
	String m_strType = "";
    int m_nZIndex = 3;
	int m_nPosX = 94;
	int m_nPosY = 9;
	int m_nWidth = 200;
	int m_nHeight = 150;
	float m_fAlpha = 1.0f;
	float m_fRotateAngle = 0.0f;
	int m_cBorderColor = 0xFF000000;
	int m_nBorderWidth = 3;
	int m_cSingleFillColor = 0x00000000;
	float[] m_arrGradientColorPos = null;
	int[] m_arrGradientFillColor = null;
	boolean m_bIsDashed = false;
	float m_fRadius = 0.0f;
	String m_strStateExpression = "";
	String m_strEffect = "";
	String m_strColorExpression = "Binding{[Value[Equip:114-Temp:173-Signal:1]]}|1&amp;2|#FF008000&amp;#FF000040";
	boolean m_bIsHGradient = true; // 水平渐变
	MainWindow m_rRenderWindow = null;
	
	int m_nSignalValue = -1;
	//stMathExpression m_oMathExpression = null;
	stIntervalExpression m_oColorIntervalExpression;
	
	Paint m_oPaint = null;  
	Rect m_rBBox = null;
	
	public boolean m_bneedupdate = true;
}
