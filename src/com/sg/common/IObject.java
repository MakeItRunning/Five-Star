package com.sg.common;

import com.mgrid.main.MainWindow;

import android.view.View;
/** IObject */
public interface IObject {
	
	/** ���ڲ���UI����Ļ����ʾλ��l,t,r,b�ֱ��Ӧ���ε����ϡ��ҡ��� */
	public void doLayout(boolean bool, int l, int t, int r, int b);
	
	/** UI��ΨһID��ʶ����ID����XML��ʱ���ȡ�õ� */
	public void setUniqueID(String strID);
	
	/** ��ȡUI��ΨһID */
	public String getUniqueID();
	
	/** UI������(Label��Image or Button) */
	public void setType(String strType);
	
	/** ��ȡUI������ */
	public String getType();
	
	/** ���ڽ���xml����<Element ID="label0" Type="Form">
	   strName��ӦElement,strValue��Ӧlabel0��strResFolder��Դ·��  */
	public void parseProperties(String strName, String strValue, String strResFolder) throws Exception;
	
	/** ��ɳ�ʼ�������� */
	public void initFinished();
	
	/** ���ڽ�UI��ӵ����� */
	public void addToRenderWindow(MainWindow rWin);
	
	/** �Ӵ������Ƴ�UI */
	public void removeFromRenderWindow(MainWindow rWin);
	
	/** ����UI���Եı仯 */
	public void updateWidget();
	
	/** ���¿ؼ����ݣ� ����ֵ��ʾ�Ƿ���Ҫˢ�½��� */
	public boolean updateValue();
	
	/** �Ƿ���Ҫ���� */
	public boolean needupdate();
	public void needupdate(boolean bNeedUpdate);
	
	/** ��ȡ�󶨱��ʽ */
	public String getBindingExpression();
	
	/** ��ȡView */
	public View getView();
	
	/** ��ȡ�ؼ����ڲ� */
	public int getZIndex();
}
