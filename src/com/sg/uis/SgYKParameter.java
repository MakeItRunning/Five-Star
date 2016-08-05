package com.sg.uis;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.mgrid.data.DataGetter;
import com.mgrid.data.EquipmentDataModel.CommandCfg.CmdParameaningCfg;
import com.mgrid.main.MainWindow;
import com.sg.common.CFGTLS;
import com.sg.common.IObject;
import com.sg.common.MutiThreadShareObject;
import com.sg.common.UtExpressionParser;
import com.sg.common.UtExpressionParser.stBindingExpression;
import com.sg.common.UtExpressionParser.stExpression;

import android.R;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener; 
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/** Setting ���� */
public class SgYKParameter extends TextView implements IObject {

	public SgYKParameter(Context context) {
		super(context); 
		this.setClickable(true);
		this.setGravity(Gravity.CENTER);
		this.setBackgroundColor(Color.GRAY);
		m_fFontSize = this.getTextSize();
        this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
	            switch (event.getAction())
	            {
		            case MotionEvent.ACTION_DOWN:
		            	m_bPressed = true;	
		            	view.invalidate();
		            	
		            	m_xscal = event.getX();
		            	m_yscal = event.getY();
		            break;
		            
		            case MotionEvent.ACTION_UP:
		            	m_bPressed = false;	
		            	view.invalidate();
		            	
		            	if (m_xscal == event.getX() && m_yscal == event.getY())
		            	onClicked();
		            break;
		            default: break;
	            }
				return true;
			}
        });
        m_oPaint = new Paint();
        m_rBBox = new Rect();
        
        setBackgroundResource(android.R.drawable.btn_default);
        setPadding(0, 0, 0, 0);
        
        m_oTextView = new TextView(context);
        m_oTextView.setTextColor(Color.BLACK);
        
        //m_oTextView.setBackgroundDrawable(new BitmapDrawable(Environment.getExternalStorageDirectory().getPath() + "/MGridRes/button.png"));
        m_oTextView.setBackgroundResource(R.drawable.btn_dropdown);
        m_oTextView.setPadding(4, 0, 0, 0);
        
        m_oSpinner = new Spinner(context);
        //m_oSpinner.setSelection(0);
        
        //Ϊ�����б���һ��������
        m_arrAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item);
        
        //���������������б�����ʱ�Ĳ˵���ʽ
        m_arrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        //����������ӵ������б���
        m_oSpinner.setAdapter(m_arrAdapter);

        //m_oSpinner.setGravity(Gravity.CENTER_VERTICAL);
        
        //m_oSpinner.setSelection(0,true);
        //m_oSpinner.setPrompt("��ѡ��");
        
        m_arrAdapter.add("��ѡ�� ��");
        
        //Ϊ�����б����ø����¼�����Ӧ���������Ӧ�˵���ѡ�� 
        m_oSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                    	m_nSpinnerPosition = position;
                    	m_oTextView.setText(parent.getSelectedItem().toString());
                    	
                    	//setSelectedText(parent.getSelectedItem().toString());
                    	//parent.setVisibility(View.VISIBLE);   
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    	m_nSpinnerPosition = 0;
                    }
                });
        
        m_oTextView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent arg1) {
	            switch (arg1.getAction())
	            {
		            case MotionEvent.ACTION_DOWN:
		            break;
		            
		            case MotionEvent.ACTION_UP:
		            	if (m_bSpinnerInited) break;
		            	
		                if (null == m_strEquipId || m_strEquipId.trim().isEmpty() || 
		            	null == m_strCommandId || m_strCommandId.trim().isEmpty()) break;
		                
			            mParameanings = DataGetter.getCtrlParameaning(m_strEquipId, m_strCommandId);
			            if (null == mParameanings) break;
			            
			            m_arrAdapter.clear();
			            m_arrAdapter.add("��ѡ�� ��");
			            Iterator<CmdParameaningCfg> cmdParamCfg_it = mParameanings.iterator();
			            for (; cmdParamCfg_it.hasNext(); )
			            {
			            	CmdParameaningCfg cmdParamCfg = cmdParamCfg_it.next();
			            	m_arrAdapter.add(cmdParamCfg.meaning);
			            }
			            
			            m_bSpinnerInited = true;
		            break;
		            
		            default: break;
	            }
				return false;
			}
        });
        
        m_oTextView.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				m_oSpinner.performClick();
			}
        });
        
	}
	
	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas) {
		if (m_rRenderWindow == null)
			return;
		if (m_rRenderWindow.isLayoutVisible(getBBox()) == false)
			return;

		if (m_bPressed) {
			int nWidth = (int) (((float)(m_nWidth) / (float)MainWindow.FORM_WIDTH) * (m_rRenderWindow.VIEW_RIGHT - m_rRenderWindow.VIEW_LEFT));
			int nHeight = (int) (((float)(m_nHeight) / (float)MainWindow.FORM_HEIGHT) * (m_rRenderWindow.VIEW_BOTTOM - m_rRenderWindow.VIEW_TOP));

			m_oPaint.setColor(0x50FF00F0);
			m_oPaint.setStyle(Paint.Style.FILL); 
			canvas.drawRect(0,0,nWidth,nHeight, m_oPaint);
		}
		super.onDraw(canvas);
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
			this.layout(nX+(int)(nWidth*0.71f), nY, nX+nWidth, nY+nHeight);
			//m_oSpinner.layout(nX, nY-15, nX+(int)(nWidth*0.7f), nY+nHeight);
			m_oSpinner.layout(nX, nY+nHeight, nX+(int)(nWidth*0.7f), nY+nHeight);
			
			m_oTextView.layout(nX, nY, nX+(int)(nWidth*0.7f), nY+nHeight);
		}
	}

	@Override
	public void addToRenderWindow(MainWindow rWin) {
		m_rRenderWindow = rWin;
		rWin.addView(m_oSpinner);
		rWin.addView(m_oTextView);
		rWin.addView(this);
	}

	@Override
	public void removeFromRenderWindow(MainWindow rWin) {
		rWin.removeView(m_oSpinner);
		rWin.removeView(m_oTextView);
		rWin.removeView(this);
	}

	public void parseProperties(String strName, String strValue, String strResFolder) throws Exception {

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
        }
        else if ("Content".equals(strName)) {
	       	 m_strContent = strValue;
	       	 this.setText(m_strContent);
        }
        else if ("FontFamily".equals(strName))
       	 	m_strFontFamily = strValue;
        else if ("IsBold".equals(strName))
       	 	m_bIsBold = Boolean.parseBoolean(strValue);
        else if ("BackgroundColor".equals(strName)) 
        	m_cBackgroundColor = Color.parseColor(strValue);
        else if ("FontColor".equals(strName)) {
	       	 m_cFontColor = Color.parseColor(strValue);
	       	 this.setTextColor(m_cFontColor);
        }
        else if ("CmdExpression".equals(strName)) {
       	 	m_strCmdExpression = strValue;
        }
        else if ("IsValueRelateSignal".equals(strName)) {
        	if ("True".equals(strValue))
        		m_bIsValueRelateSignal = true;
        	else
        		m_bIsValueRelateSignal = false;
        }
        else if ("ButtonWidthRate".equals(strName)) {
        	m_fButtonWidthRate = Float.parseFloat(strValue);
        }
	}

	@Override
	public void initFinished()
	{
		setGravity(Gravity.CENTER);
		
		double padSize = CFGTLS.getPadHeight(m_nHeight, MainWindow.FORM_HEIGHT, getTextSize())/2;
		setPadding(0, (int) padSize, 0, (int) padSize);
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
	
	private void onClicked() {
		//new AlertDialog.Builder(this.getContext()).setTitle("ѡ��") .setMessage(String.valueOf(m_nSpinnerPosition)) .show();

		if (0 == m_nSpinnerPosition || null == mParameanings || m_nSpinnerPosition > mParameanings.size()) return;
		
		// ���Ϳ�������
		if (m_strCmdExpression.trim().isEmpty()) return;
		
		synchronized (m_rRenderWindow.m_oShareObject)
		{
			m_rRenderWindow.m_oShareObject.m_mapCmdCommand.put(getUniqueID(),
					String.valueOf(mParameanings.get(m_nSpinnerPosition - 1).value));
		}
	}
	
	public String getBindingExpression() {
		parseExpression(m_strCmdExpression);
		return m_strCmdExpression;
	}
	
	@Override
	public void updateWidget() {		
	}

	@Override
	public boolean updateValue()
	{
        return false;
	}

	@Override
    public boolean needupdate()
    {
	    return false;
    }
	
	@Override
    public void needupdate(boolean bNeedUpdate)
    {
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

	public boolean parseExpression(String strBindingExpression) {
		String newStr = UtExpressionParser.getMathExpression(strBindingExpression);
		
		if ("".equals(newStr)) return false;
		
		// [Value[Equip:61-Temp:6-Signal:2]]
		String[] arrExpress = newStr.split("\\[");
		// strBindType = arrExpress[1];
		String strNew = arrExpress[2];
		String[] arrTemp = strNew.split("\\]");
		strNew = arrTemp[0];
		String[] arrId = strNew.split("-");
		for (int i = 0; i < arrId.length; ++i)
		{
			String[] arrValue = arrId[i].split(":");
			if (arrValue.length < 2)
				continue;
			if ("Equip".equals(arrValue[0]))
			{
				m_strEquipId = arrValue[1];
			} else if ("Temp".equals(arrValue[0]))
			{
				// nTemplateId = Integer.parseInt(arrValue[1]);
			} else if ("Signal".equals(arrValue[0]))
			{
				// nSignalId = Integer.parseInt(arrValue[1]);
			} else if ("Command".equals(arrValue[0]))
			{
				m_strCommandId = arrValue[1];
			} else if ("Parameter".equals(arrValue[0]))
			{
				// nValueType = Integer.parseInt(arrValue[1]);
			} else if ("Value".equals(arrValue[0]))
			{
				// strValue = arrValue[1];
			}
		}
        
        if (null == m_strEquipId || m_strEquipId.trim().isEmpty() || 
        	null == m_strCommandId || m_strCommandId.trim().isEmpty()) return false;
        
        mParameanings = DataGetter.getCtrlParameaning(m_strEquipId, m_strCommandId);
        if (null == mParameanings) return false;
        
    	m_arrAdapter.clear();
    	m_arrAdapter.add("��ѡ�� ��");
        Iterator<CmdParameaningCfg> cmdParamCfg_it = mParameanings.iterator();
        for (; cmdParamCfg_it.hasNext(); )
        {
        	CmdParameaningCfg cmdParamCfg = cmdParamCfg_it.next();
        	m_arrAdapter.add(cmdParamCfg.meaning);
        }
        
		return true;
	}
	
// params:
	String m_strID = "";
	String m_strType = "";
    int m_nZIndex = 10;
	int m_nPosX = 152;
	int m_nPosY = 287;
	int m_nWidth = 75;
	int m_nHeight = 23;
	float m_fAlpha = 1.0f;
	boolean m_bIsBold = false;
	String m_strFontFamily = "΢���ź�";
	int m_cBackgroundColor = 0xFFFCFCFC;
	int m_cFontColor = 0xFF000000;
	String m_strContent = "Setting";
	String m_strCmdExpression = "Binding{[Cmd[Equip:113-Temp:168-Command:1]]}";
	boolean m_bIsValueRelateSignal = false;
	float m_fButtonWidthRate = 0.3f;
	MainWindow m_rRenderWindow = null;
	float m_fFontSize = 6.0f;
	boolean m_bPressed = false;
	Paint m_oPaint = null;
	Rect m_rBBox = null;
	
	// ��¼�������꣬���˻���������������������������⡣
	public float m_xscal = 0;
	public float m_yscal = 0;
	
	private int m_nSpinnerPosition = 0;
	private TextView m_oTextView = null; // �� TextView ���� Spinner ��ʾ
	public Spinner m_oSpinner = null;
    private ArrayAdapter<String> m_arrAdapter=null;

    private boolean m_bSpinnerInited = false;
    private String m_strEquipId = null;
    private String m_strCommandId = null;
    public List<CmdParameaningCfg> mParameanings = null;
}
