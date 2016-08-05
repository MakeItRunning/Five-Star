package com.mgrid.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import com.sg.common.IObject;

public class EquipmentDataModel
{
	/* UI控件注册到信号值，如果该信号有更新直接可调用UI更新。*/
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
	
	// 表示一个告警配置条件项
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
	
	// 表示一个告警配置
	public class EventCfg
	{
		public EventCfg()
		{
			htConditions = new Hashtable<String, EventConditionCfg>();
			
			registedNameObj = new ArrayList<IObject>();
		}
		
		public int     eventid;
		public String  eventname;
		public int     enabled;        // 1 启用 0 停用
		public int     signalid;
		public Hashtable<String, EventConditionCfg> htConditions = null;  // <条件ID， 条件>  储存条件
		
		public List<IObject> registedNameObj = null;
	}
	
	// 表示一个控制项配置
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
		public List<CmdParameaningCfg> meanings = null;  // 控制参数含义信息
	}
	
	// 表示一个设备实例
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
		
		// 缓存请求协议包
		public byte[] signal_req = null;
		
		// 保存响应包以便比较
		public byte[] signal_rsp = null;
		
		// 更新时间
		public long lUpdateTime = 0;  // milliseconds since January 1, 1970 00:00:00 UTC.
		
		// 采集器更新时间
		public String strSampleUpdateTime = "";
		
		// 保存注册的信号列表控件， 若设备信号有更新则据此表跟新。
		public List<IObject> registedLstObj = null;
		
		// 实时信号
		public Hashtable<String, Signal> htSignalData = null;  // <信号ID， 值> 存储实时信号
		
		// 告警配置
		public Hashtable<String, EventCfg> htEventCfg = null;    // <告警ID， 告警配置> 存储告警配置
		
		// 控制配置
		public Hashtable<String, CommandCfg> htCmdCfg = null;  // <控制ID， 控制项配置> 存储控制配置
	}
	
	// 表示一个告警项
	public class Event
	{
		public String name;     // 告警名称
		public int grade;      // 告警级别
		public long starttime;  // 告警开始时间
		public long stoptime;   // 告警结束时间
		public String value;   // 触发值
		public String meaning; //告警含义
	}

	// 设备
	public Hashtable<String, Equipment> htEquipmentData = null;  // <设备ID，设备实例 >
	//static public Hashtable<String, Hashtable<String, List<String>>> htHistoryData = null;  // <设备ID， <信号ID， 列表<值>>> 存储历史信号
	
	// 实时告警
	public Hashtable<String, Hashtable<String, Event>> htEventData = null;  // <设备ID， <告警ID， 告警>> 存储实时告警
	
	// 实时设备，从采集器获取的设备列表名称。
	public Hashtable<String, String> htEquipments = null;  // <设备ID，设备名称>
	
	// 实时告警配置， 从采集器获取的所有告警配置信息。
	public Hashtable<String, Hashtable<String, EventCfg>> htEventCfg = null; // <设备ID， <告警ID， 告警配置>>
	
	// 缓存 所有实时告警信息 请求协议包。
	public byte[] rtalarm_req = null;
	
	// 缓存 接受协议包
	public String rtalarm_rspbody = "";
	
	// 保存注册的 所有实时告警列表控件 。
	public List<IObject> lstRegistedMainAlarmList = null;
	
	// flags, 标记告警列表、设备列表、告警配置 是否初始化成功。
	boolean bEventData = false;
	boolean bEquipments = false;
	boolean bEventCfg = false;
	
	// 组态总设备集
	public HashSet<String> hsEquipSet = null;  // <设备ID>
	
	// 页面设备关系表
	public Hashtable<String, HashSet<String>> htPageEquipSet = null;  // <PageName, HashSet<设备ID>>
	
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
