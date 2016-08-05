
package com.sg.uis;

import java.io.IOException;

import com.sg.common.SgRealTimeData;

import java.io.InputStream;
import java.text.DecimalFormat;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.mgrid.main.MainWindow;
import com.sg.common.IObject;
import com.sg.common.MutiThreadShareObject;
import com.sg.common.UtExpressionParser.stExpression;

/** 仪表盘 */
public class SgAmmeter extends View implements IObject {
	
	public SgAmmeter(Context context) {
		super(context);
		this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
        });
		m_rBBox = new Rect();
		try {
			AssetManager assetManager = this.getContext().getResources().getAssets();
			InputStream is = null;

			if (null == m_oBackImage)
			{
				is = assetManager.open("ui/Ammeter_Back.png");
				m_oBackImage = BitmapFactory.decodeStream(is);
				is.close();
			}

			if (null == m_oFrontImage)
			{
				is = assetManager.open("ui/Ammeter_Front.png");
				m_oFrontImage = BitmapFactory.decodeStream(is);
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		m_oPaint = new Paint();
		m_oPaint.setTextSize(m_fFontSize);
		m_oPaint.setColor(Color.BLACK);
		m_oPaint.setAntiAlias(true); // 设置画笔的锯齿效果
		m_oPaint.setStyle(Paint.Style.STROKE); 
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (m_rRenderWindow == null)
			return;
		if (m_rRenderWindow.isLayoutVisible(getBBox()) == false)
			return;
		
		int nWidth = (int) (((float)(m_nWidth) / (float)MainWindow.FORM_WIDTH) * (m_rRenderWindow.VIEW_RIGHT - m_rRenderWindow.VIEW_LEFT));
		int nHeight = (int) (((float)(m_nHeight) / (float)MainWindow.FORM_HEIGHT) * (m_rRenderWindow.VIEW_BOTTOM - m_rRenderWindow.VIEW_TOP));

		float fScaleX = (float)nWidth / (float)m_oBackImage.getWidth();
		float fScaleY = (float)nHeight / (float)m_oBackImage.getHeight();
	
		canvas.scale(fScaleX, fScaleY);
		// draw back image
		canvas.drawBitmap(m_oBackImage, 0.0f,0.0f, m_oPaint);
		// 指针移到中心位置(减m_oFrontImage.getHeight()/2是为了让指针刚好在中心位置)
		canvas.translate(m_oBackImage.getWidth()/2.0f, m_oBackImage.getHeight()/2.0f-m_oFrontImage.getHeight()/2.0f);
		// draw Gradations
		drawGradations(canvas, -m_oBackImage.getWidth()/2.0f*0.56f, 0.0f, m_oFrontImage.getHeight()/2.0f);
		// draw pointer
		canvas.rotate(-80.0f, 0.0f, m_oFrontImage.getHeight()/2.0f);
		float fAngle = m_fCurrentValue / m_fMeasure * m_nTotalAngle;
		canvas.rotate(fAngle, 0.0f, m_oFrontImage.getHeight()/2.0f);
		canvas.drawBitmap(m_oFrontImage, 0.0f,0.0f, m_oPaint);
	}

	// 绘制刻度
	private void drawGradations(Canvas canvas, float fY, float CenterXOffset, float CenterYOffset) {
		DecimalFormat decimalFormat = new DecimalFormat("###.#");
		canvas.rotate(-2*80, CenterXOffset, CenterYOffset);
		//Log.v("Warnning", "仪表盘 = "+m_fMeasure);
		for (int i = 0; i < 9; ++i) {
			if (i % 2 == 0) {
				String valueString = String.valueOf(decimalFormat.format((float)i * m_fMeasure / 8.0f));
				canvas.drawText(valueString, -3.0f*valueString.length(), fY, m_oPaint);
			}
			canvas.rotate(40.0f, CenterXOffset, CenterYOffset);
		}		
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
	
	@Override
	public void addToRenderWindow(MainWindow rWin) {
		m_rRenderWindow = rWin;
		rWin.addView(this);
	}
	
	@Override
	public void removeFromRenderWindow(MainWindow rWin) {  
		if (m_oBackImage != null && m_oBackImage.isRecycled() == false)
			m_oBackImage.recycle();
		if (m_oFrontImage != null && m_oFrontImage.isRecycled() == false)
			m_oFrontImage.recycle();
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
        else if ("CurrentValue".equals(strName))
        	m_fCurrentValue = Float.parseFloat(strValue);
        else if ("Measure".equals(strName))
        	m_fMeasure = Float.parseFloat(strValue);
        else if ("CurrentUnit".equals(strName))
        	m_strCurrentUnit = strValue;
        else if ("Expression".equals(strName)) {
        	m_strExpression = strValue;
        }
	}

	@Override
	public void initFinished()
	{
	}

	public String getBindingExpression() {
		return m_strExpression;
	}
	
	// 设备更新
	public void updateWidget() {
		this.invalidate();
	}
	
	@Override
	public boolean updateValue()
	{
		m_bneedupdate = false;
		
		SgRealTimeData oRealTimeData = m_rRenderWindow.m_oShareObject.m_mapRealTimeDatas.get(this.getUniqueID());
		if (oRealTimeData == null)
			return false;
		String strValue = oRealTimeData.strValue;
		if (strValue == null || "".equals(strValue) == true)
			return false;
		
		float fValue = -1.0f;
		try {
			fValue = Float.parseFloat(strValue);
		}catch(Exception e) {
			
		}
       
        // 数值变化时才刷新页面
        if (fValue != m_fCurrentValue) {
        	setCurrentValue(fValue);
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
	
	public void setCurrentValue(float fValue) {
		m_fCurrentValue = fValue;
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
    int m_nZIndex = 13;
	int m_nPosX = 300;
	int m_nPosY = 397;
	int m_nWidth = 150;
	int m_nHeight = 137;
	float m_fAlpha = 1.0f;
	
	float m_fCurrentValue = 0.0f;
	float m_fMeasure = 1500.0f;
	String m_strCurrentUnit = "KV";
	String m_strExpression = "Binding{[Value[Equip:119-Temp:167-Signal:2]]}";
	MainWindow m_rRenderWindow = null;
	
	stExpression m_oMathExpression = null;
	
	private static Bitmap m_oBackImage = null;
	private static Bitmap m_oFrontImage = null;
	
	Rect m_rBBox = null;
	
	Paint m_oPaint = null;
	float m_fFontSize = 18.0f;
	float m_nTotalAngle = 40f*8;
	
	public boolean m_bneedupdate = true;
}
