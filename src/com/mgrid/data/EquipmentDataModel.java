package com.mgrid.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import com.sg.common.IObject;

public class EquipmentDataModel
{
	/* UI�ؼ�ע�ᵽ�ź�ֵ��������ź��и���ֱ�ӿɵ���UI���¡�*/
	public class Signal
	{
		public Signal()
		{
			registedObj = new ArrayList<IObject>();
			registedNameObj = new ArrayList<IObject>();
			registedAlarmObj = new ArrayList<IObject>();
		}
		
		public String id = "";
		public String value = "";
		public String meaning = "";
		public long freshtime = 0;
		public String name = "";
		public String unit = "";
		public int is_invalid;
		public int value_type = 0;
		public int severity = 0;
		public int precision = 2;
		public String description = "";
		public List<IObject> registedObj = null;
		public List<IObject> registedNameObj = null;
		public List<IObject> registedAlarmObj = null;
	}
	
	// ��ʾһ���澯����������
	public class EventConditionCfg
	{
		public int    conditionid;
		public int    severity;
		public String  startcompare;
		public String  endcompare;
		public int    startdelay;
		public int    enddelay;
		public String  meaning;
	}
	
	// ��ʾһ���澯����
	public class EventCfg
	{
		public EventCfg()
		{
			htConditions = new Hashtable<String, EventConditionCfg>();
			
			registedNameObj = new ArrayList<IObject>();
		}
		
		public int     eventid;
		public String  eventname;
		public int     enabled;        // 1 ���� 0 ͣ��
		public int     signalid;
		public Hashtable<String, EventConditionCfg> htConditions = null;  // <����ID�� ����>  ��������
		
		public List<IObject> registedNameObj = null;
	}
	
	// ��ʾһ������������
	public class CommandCfg
	{
		public class CmdParameaningCfg
		{
			public int value;
			public String meaning;
		}
		
		public CommandCfg()
		{
			meanings = new ArrayList<CmdParameaningCfg>();
		}
		
		public int cmdid;
		public float fMaxValue;
		public float fMinValue;
		public String name;
		public String unit;
		public List<CmdParameaningCfg> meanings = null;  // ���Ʋ���������Ϣ
	}
	
	// ��ʾһ���豸ʵ��
	public class Equipment
	{
		public Equipment()
		{
			registedLstObj = new ArrayList<IObject>();
			htSignalData = new Hashtable<String, Signal>();
			htEventCfg = new Hashtable<String, EventCfg>();
			htCmdCfg = new Hashtable<String, CommandCfg>();
		}
		
		public String m_equipid = "";
		public String m_tempid = "";
		public String m_category = "";
		public String m_name = "";
		public String m_xmlfile = "";
		
		// ��������Э���
		public byte[] signal_req = null;
		
		// ������Ӧ���Ա�Ƚ�
		public byte[] signal_rsp = null;
		
		// ����ʱ��
		public long lUpdateTime = 0;  // milliseconds since January 1, 1970 00:00:00 UTC.
		
		// �ɼ�������ʱ��
		public String strSampleUpdateTime = "";
		
		// ����ע����ź��б�ؼ��� ���豸�ź��и�����ݴ˱���¡�
		public List<IObject> registedLstObj = null;
		
		// ʵʱ�ź�
		public Hashtable<String, Signal> htSignalData = null;  // <�ź�ID�� ֵ> �洢ʵʱ�ź�
		
		// �澯����
		public Hashtable<String, EventCfg> htEventCfg = null;    // <�澯ID�� �澯����> �洢�澯����
		
		// ��������
		public Hashtable<String, CommandCfg> htCmdCfg = null;  // <����ID�� ����������> �洢��������
	}
	
	// ��ʾһ���澯��
	public class Event
	{
		public String name;     // �澯����
		public int grade;      // �澯����
		public long starttime;  // �澯��ʼʱ��
		public long stoptime;   // �澯����ʱ��
		public String value;   // ����ֵ
		public String meaning; //�澯����
	}

	// �豸
	public Hashtable<String, Equipment> htEquipmentData = null;  // <�豸ID���豸ʵ�� >
	//static public Hashtable<String, Hashtable<String, List<String>>> htHistoryData = null;  // <�豸ID�� <�ź�ID�� �б�<ֵ>>> �洢��ʷ�ź�
	
	// ʵʱ�澯
	public Hashtable<String, Hashtable<String, Event>> htEventData = null;  // <�豸ID�� <�澯ID�� �澯>> �洢ʵʱ�澯
	
	// ʵʱ�豸���Ӳɼ�����ȡ���豸�б����ơ�
	public Hashtable<String, String> htEquipments = null;  // <�豸ID���豸����>
	
	// ʵʱ�澯���ã� �Ӳɼ�����ȡ�����и澯������Ϣ��
	public Hashtable<String, Hashtable<String, EventCfg>> htEventCfg = null; // <�豸ID�� <�澯ID�� �澯����>>
	
	// ���� ����ʵʱ�澯��Ϣ ����Э�����
	public byte[] rtalarm_req = null;
	
	// ���� ����Э���
	public String rtalarm_rspbody = "";
	
	// ����ע��� ����ʵʱ�澯�б�ؼ� ��
	public List<IObject> lstRegistedMainAlarmList = null;
	
	// flags, ��Ǹ澯�б��豸�б��澯���� �Ƿ��ʼ���ɹ���
	boolean bEventData = false;
	boolean bEquipments = false;
	boolean bEventCfg = false;
	
	// ��̬���豸��
	public HashSet<String> hsEquipSet = null;  // <�豸ID>
	
	// ҳ���豸��ϵ��
	public Hashtable<String, HashSet<String>> htPageEquipSet = null;  // <PageName, HashSet<�豸ID>>
	
	public EquipmentDataModel()
	{
		htEquipmentData = new Hashtable<String, Equipment>();
		htEventData = new Hashtable<String, Hashtable<String, Event>>();
		lstRegistedMainAlarmList = new ArrayList<IObject>();
		htEquipments = new Hashtable<String, String>();
		htEventCfg = new Hashtable<String, Hashtable<String, EventCfg>>();
		hsEquipSet = new HashSet<String>();
		htPageEquipSet = new Hashtable<String, HashSet<String>>();
	}
}
