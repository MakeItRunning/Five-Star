package com.mgrid.data;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.mgrid.data.EquipmentDataModel.CommandCfg;
import com.mgrid.data.EquipmentDataModel.CommandCfg.CmdParameaningCfg;
import com.mgrid.data.EquipmentDataModel.Equipment;
import com.mgrid.data.EquipmentDataModel.Event;
import com.mgrid.data.EquipmentDataModel.EventCfg;
import com.mgrid.data.EquipmentDataModel.EventConditionCfg;
import com.mgrid.data.EquipmentDataModel.Signal;
import com.sg.common.IObject;

import comm_service.msg_head;
import comm_service.msg_type;
import comm_service.protocol;
import comm_service.service;
import data_model.ipc_active_event;
import data_model.ipc_cfg_control;
import data_model.ipc_cfg_ctrl_parameaning;
import data_model.ipc_cfg_event;
import data_model.ipc_cfg_signal;
import data_model.ipc_cfg_trigger_value;
import data_model.ipc_equipment;

public class DataGetter extends Thread
{
	public DataGetter()
	{
		// 
	}
	
	public void run()
	{
		if (null == equipment.rtalarm_req)
		{
			equipment.rtalarm_req = protocol.build_query_all_active_alarm_list();
		}
		// TODO: 初始化告警配置信息，IPC现无法获取完整告警配置信息。
		
		// 初始化设备列表、 设备告警配置信息
		while(true)
		{
			//if (equipment.bEquipments) break;
			
			List<ipc_equipment> equip_list = service.get_equipment_list(service.IP, service.PORT);
			
			// 如果获取到的设备列表为空，等待两秒后重复获取。
			if (equip_list.isEmpty())
			{
				try
				{
					sleep(2000);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				continue;
			}
			
			Iterator<ipc_equipment> it = equip_list.iterator();
			while(it.hasNext())
			{
				ipc_equipment equip = it.next();
				equipment.htEquipments.put(String.valueOf(equip.id), equip.name);
				
				// a. 保存设备信息
				Equipment equipobj = equipment.htEquipmentData.get(String.valueOf(equip.id));
				if (null == equipobj) 
				{
					equipobj = equipment.new Equipment();
					equipment.htEquipmentData.put(String.valueOf(equip.id), equipobj);
				}
				
				equipobj.m_equipid = String.valueOf(equip.id);
				equipobj.m_tempid = String.valueOf(equip.templateId);
				equipobj.m_category = String.valueOf(equip.category);
				equipobj.m_name = equip.name;
				equipobj.m_xmlfile = equip.xmlname;
				equipobj.signal_req = protocol.build_query_signal_rt_list(equip.id);
				
				// b. 获取该设备告警配置信息
				Hashtable<String, EventCfg> equip_event_cfg = new Hashtable<String, EventCfg>();
				
				List<ipc_cfg_event> event_cfg_list = service.get_event_cfg_list(service.IP, service.PORT, equip.id);
				Iterator<ipc_cfg_event> event_cfg_it = event_cfg_list.iterator();
				while(event_cfg_it.hasNext())
				{
					EventCfg eventcfg = equipment.new EventCfg();
					ipc_cfg_event ipc_evtcfg = event_cfg_it.next();
					
					eventcfg.eventid = ipc_evtcfg.id;
					eventcfg.eventname = ipc_evtcfg.name;
					
					equip_event_cfg.put(String.valueOf(ipc_evtcfg.id), eventcfg);
					equipobj.htEventCfg.put(String.valueOf(ipc_evtcfg.id), eventcfg);
				}
				
				equipment.htEventCfg.put(String.valueOf(equip.id), equip_event_cfg);
				
				// c. 获取事件告警信息 & 告警阈值信息
				List<ipc_cfg_trigger_value> trigger_value_list = service.get_cfg_trigger_value(service.IP, service.PORT, equip.id);
				Iterator<ipc_cfg_trigger_value> trigger_value_it = trigger_value_list.iterator();
				EventCfg evtcfg = null;
				while(trigger_value_it.hasNext())
				{
					ipc_cfg_trigger_value ipc_trigger = trigger_value_it.next();
					
					evtcfg = equip_event_cfg.get(String.valueOf(ipc_trigger.eventid));
					if (null == evtcfg) continue;
					evtcfg.enabled = ipc_trigger.enabled;
					
					EventConditionCfg condcfg = evtcfg.htConditions.get(String.valueOf(ipc_trigger.conditionid));
					if (null == condcfg)
					{
						condcfg = equipment.new EventConditionCfg();
						evtcfg.htConditions.put(String.valueOf(ipc_trigger.conditionid), condcfg);
						
						condcfg.conditionid = ipc_trigger.conditionid;
					}
					
					condcfg.severity = ipc_trigger.eventseverity;
					condcfg.startcompare = String.valueOf(ipc_trigger.startvalue);
					condcfg.endcompare = String.valueOf(ipc_trigger.stopvalue);
					condcfg.severity = ipc_trigger.eventseverity;
					
					// 设置设备对象中事件配置
					EventCfg equipEvtCfg = equipobj.htEventCfg.get(String.valueOf(ipc_trigger.eventid));
					if (null == equipEvtCfg) continue;
					equipEvtCfg.htConditions.put(String.valueOf(ipc_trigger.conditionid), condcfg);
				}
				
				// d. 获取信号配置信息
				List<ipc_cfg_signal> signal_cfg = service.get_signal_cfg_list(service.IP, service.PORT, equip.id);
				Iterator<ipc_cfg_signal> signal_cfg_it = signal_cfg.iterator();
				while(signal_cfg_it.hasNext())
				{
					ipc_cfg_signal ipc_signalcfg = signal_cfg_it.next();
					Signal signal = equipobj.htSignalData.get(String.valueOf(ipc_signalcfg.id));
					if (null == signal) 
					{
						signal = equipment.new Signal();
						equipobj.htSignalData.put(String.valueOf(ipc_signalcfg.id), signal);
					}
					
					signal.id = String.valueOf(ipc_signalcfg.id);
					signal.name = ipc_signalcfg.name;
					signal.unit = ipc_signalcfg.unit;
					signal.precision = ipc_signalcfg.precision;
					signal.description = ipc_signalcfg.description;
				}
				
				// e. 获取控制配置信息 && 控制参数含义信息
				List<ipc_cfg_control> command_cfg = service.get_control_cfg_list(service.IP, service.PORT, equip.id);
				Iterator<ipc_cfg_control> command_cfg_it = command_cfg.iterator();
				while(command_cfg_it.hasNext())
				{
					ipc_cfg_control ipc_commandcfg = command_cfg_it.next();
					CommandCfg command = equipobj.htCmdCfg.get(String.valueOf(ipc_commandcfg.id));
					if (null == command) 
					{
						command = equipment.new CommandCfg();
						equipobj.htCmdCfg.put(String.valueOf(ipc_commandcfg.id), command);
					}
					
					command.cmdid = ipc_commandcfg.id;
					command.name = ipc_commandcfg.name;
					command.unit = ipc_commandcfg.unit;
					command.fMaxValue = ipc_commandcfg.fMaxValue;
					command.fMinValue = ipc_commandcfg.fMinValue;
				}
				
				// 获取控制参数含义
				List<ipc_cfg_ctrl_parameaning> cmd_parameaning_cfg = service.get_control_parameaning_cfg_list(service.IP, service.PORT, equip.id);
				Iterator<ipc_cfg_ctrl_parameaning> cmd_parameaning_it = cmd_parameaning_cfg.iterator();
				while(cmd_parameaning_it.hasNext())
				{
					ipc_cfg_ctrl_parameaning ipc_cmdparamcfg = cmd_parameaning_it.next();
					CommandCfg command = equipobj.htCmdCfg.get(String.valueOf(ipc_cmdparamcfg.ctrlid));
					if (null == command) 
					{
						command = equipment.new CommandCfg();
						equipobj.htCmdCfg.put(String.valueOf(ipc_cmdparamcfg.ctrlid), command);
					}
					
					CmdParameaningCfg cmdparam = command.new  CmdParameaningCfg();
					cmdparam.value = ipc_cmdparamcfg.paramvalue;
					cmdparam.meaning = ipc_cmdparamcfg.parameaning;
					
					command.meanings.add(cmdparam);
				}
				
			}  // end of while equipment.hasnext 
			
			equipment.bEquipments = true;
			equipment.bEventCfg = true;

			break;
		}  /* end of while 设备列表、告警配置 */
		
		Equipment currEquipObj = null;
		
		// 实时数据初始化，预先将所有设备实时数据刷新一遍。TODO: 实时告警预加载
		Iterator<String> equipNameIt = equipment.hsEquipSet.iterator();
		while(equipNameIt.hasNext())
		{
			currEquipObj = equipment.htEquipmentData.get(equipNameIt.next());
			if (null == currEquipObj) continue;  // 不可能出现！！！
			
			proc_rtsignal(currEquipObj);
		}
		
		// 线程主循环，以一次循环 2 S （采集器更新时间） 为时间参照。  *** Master while ***
		while(true)
		{
			try
			{
				sleep(100);  // 保护sleep， 防止设备列表为空时死循环空转。
			} catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}
			
			// 请求所有告警信息
			proc_allrtalarm();
			
			// 请求活动页面相关各设备实时数据
			HashSet<String> equipSet = equipment.htPageEquipSet.get(currentPage);
			if (null == equipSet) continue;
			
			// fix java.util.ConcurrentModificationException
			if (bIsLoading)
				equipNameIt = new CopiedIterator<String>(equipSet.iterator( ));
			else
				equipNameIt = equipSet.iterator();
			
			while(equipNameIt.hasNext())
			{
				currEquipObj = equipment.htEquipmentData.get(equipNameIt.next());
				if (null == currEquipObj) continue;  // 不可能出现！！！
				
				proc_rtsignal(currEquipObj);
				currEquipObj.lUpdateTime = java.lang.System.currentTimeMillis();
				
				try
				{
					sleep(500);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			// 更新所有 超时未更新设备 实时数据
			// fix java.util.ConcurrentModificationException
			if (bIsLoading)
				equipNameIt = new CopiedIterator<String>(equipment.hsEquipSet.iterator());
			else
				equipNameIt = equipment.hsEquipSet.iterator();
			
			while(equipNameIt.hasNext())
			{
				currEquipObj = equipment.htEquipmentData.get(equipNameIt.next());
				if (null == currEquipObj) continue;  // 不可能出现！！！
				
				// 对于 30 S 内有更新的设备不进行操作
				if (java.lang.System.currentTimeMillis()-currEquipObj.lUpdateTime < 30000) continue;
				
				proc_rtsignal(currEquipObj);
				currEquipObj.lUpdateTime = java.lang.System.currentTimeMillis();
				
				try
				{
					sleep(500);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			// 手动 GC
			//System.gc();
		} // end of master while
	} // end of run()
	
	/**
	 * 获取设备信号集，线程安全。
	 * 
	 * @param String 设备实例ID
	 * @return 成功返回实例信号集Hashtable，否则返回null。
	 * @throws 
	 */
	public static Hashtable<String, Signal> getEquipSignalList(String equipmentid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return null;
		
		return equip.htSignalData;
	}
	
	/**
	 * Adapter getEquipSignalList
	 * 
	 * @param int 设备实例ID
	 * @return 成功返回实例信号集Hashtable，否则返回null。
	 * @throws 
	 */
	public static Hashtable<String, Signal> getEquipSignalList(int equipmentid)
	{
		return getEquipSignalList(String.valueOf(equipmentid));
	}
	
	/**
	 * 获取 全局实时告警列表，线程安全。
	 * 
	 * @param
	 * @return 全局告警信息Hashtable。
	 * @throws 
	 */
	public static Hashtable<String, Hashtable<String, Event>> getRTEventList()
	{
		return equipment.htEventData;
	}
	
	/**
	 * 获取 设备实时告警列表，线程安全。
	 * 
	 * @param
	 * @return 设备设备告警信息Hashtable。
	 * @throws 
	 */
	public static Hashtable<String, Hashtable<String, Event>> getEquipRTEventList(String equipid)
	{
		// TODO: 尚未完成
		Hashtable<String, Hashtable<String, Event>> hashtable = new Hashtable<String, Hashtable<String, Event>>();
		
		hashtable.put(equipid, equipment.htEventData.get(equipid));
		return hashtable;
	}
	
	/**
	 * Adapter of 获取 设备实时告警列表，线程安全。
	 * 
	 * @param
	 * @return 设备告警信息Hashtable。
	 * @throws 
	 */
	public static Hashtable<String, Hashtable<String, Event>> getEquipRTEventList(int nEquipId)
	{
		return getEquipRTEventList(String.valueOf(nEquipId));
	}
	
	/**
	 * 获取信号名， 线程安全。
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @return String 信号名
	 * @throws 
	 */
	public static String getSignalName(String equipmentid, String signalid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return "";
		
		Signal signal = equip.htSignalData.get(signalid);
		
		return (null == signal ? "" : signal.name);
	}
	
	/**
	 * Adapter getSignalName
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @return String 信号名
	 * @throws 
	 */
	public static String getSignalName(int equipmentid, int signalid)
	{
		return getSignalName(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * 设置信号名， 线程安全。
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @param String 信号名
	 * @return boolean true为成功
	 * @throws 
	 */
	public static boolean setSignalName(String equipmentid, String signalid, String signalname)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return false;
		
		Signal signal = equip.htSignalData.get(signalid);
		
		if (null == signal)
			return false;
		else
			signal.name = new String(signalname);
		
		// 通知名称有更新
		Iterator<IObject> regobj_it = signal.registedNameObj.iterator();
		while (regobj_it.hasNext())
		{
			regobj_it.next().needupdate(true);
		}
		
		return true;
	}
	
	/**
	 * Adapter setSignalName
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @param String 信号名
	 * @return boolean true为成功
	 * @throws 
	 */
	public static boolean setSignalName(int equipmentid, int signalid, String signalname)
	{
		return setSignalName(String.valueOf(equipmentid), String.valueOf(signalid), signalname);
	}
	
	/**
	 * 获取信号值， 线程安全。
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @return String 信号值
	 * @throws 
	 */
	public static String getSignalValue(String equipmentid, String signalid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return "";
		
		Signal signal = equip.htSignalData.get(signalid);
		
		return (null == signal ? "" : signal.value);
	}
	
	/**
	 * Adapter getSignalValue
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @return String 信号值
	 * @throws 
	 */
	public static String getSignalValue(int equipmentid, int signalid)
	{
		return getSignalValue(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * 获取信号含义， 线程安全。
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @return String 信号含义
	 * @throws 
	 */
	public static String getSignalMeaning(String equipmentid, String signalid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return "";
		
		Signal signal = equip.htSignalData.get(signalid);
		
		return (null == signal ? "" : signal.meaning);
	}
	
	/**
	 * Adapter getSignalMeaning
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @return String 信号含义
	 * @throws 
	 */
	public static String getSignalMeaning(int equipmentid, int signalid)
	{
		return getSignalMeaning(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * 获取信号描述， 线程安全。
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @return String 信号描述
	 * @throws 
	 */
	public static String getSignalDescription(String equipmentid, String signalid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return "";
		
		Signal signal = equip.htSignalData.get(signalid);
		
		return (null == signal ? "" : signal.description);
	}
	
	/**
	 * Adapter getSignalDescription
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @return String 信号描述
	 * @throws 
	 */
	public static String getSignalDescription(int equipmentid, int signalid)
	{
		return getSignalDescription(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * 获取信号告警级别， 线程安全。
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @return int 告警级别
	 * @throws 
	 */
	public static int getSignalSeverity(String equipmentid, String signalid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return 0;
		
		Signal signal = equip.htSignalData.get(signalid);
		
		return (null == signal ? 0 : signal.severity);
	}
	
	/**
	 * Adapter getSignalSeverity
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @return int 告警级别
	 * @throws 
	 */
	public static int getSignalSeverity(int equipmentid, int signalid)
	{
		return getSignalSeverity(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * 获取控制参数含义集合。
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @return List<CmdParameaningCfg> 告警参数配置信息集
	 * @throws 
	 */
	public static List<CmdParameaningCfg> getCtrlParameaning(String equipmentid, String commandid)
	{
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)  return null;
		
		CommandCfg commandcfg = equip.htCmdCfg.get(commandid);
		
		return (null == commandcfg ? null : commandcfg.meanings);
	}
	
	/**
	 * Adapter getSignalSeverity
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @return List<CmdParameaningCfg> 告警参数配置信息集
	 * @throws 
	 */
	public static List<CmdParameaningCfg> getCtrlParameaning(int equipmentid, int commandid)
	{
		return getCtrlParameaning(String.valueOf(equipmentid), String.valueOf(commandid));
	}
	
	/**
	 * 获取设备名称，若不存在则返回空字符串。
	 * 
	 * @param String 设备实例ID
	 * @return String 设备名称
	 * @throws 
	 */
	public static String getEquipmentName(String equipmentid)
	{
		String equipname = equipment.htEquipments.get(equipmentid);
		return (null == equipname ? "" : equipname);
	}
	
	/**
	 * Adapter getEquipmentName
	 * 
	 * @param int 设备实例ID
	 * @return String 设备名称
	 * @throws 
	 */
	public static String getEquipmentName(int equipmentid)
	{
		return getEquipmentName(String.valueOf(equipmentid));
	}
	
	/**
	 * 获取告警名称
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @return String 告警名称
	 * @throws 
	 */
	public static String getEventName(String equipmentid, String eventid)
	{
		Hashtable<String, EventCfg> equip_cfg = equipment.htEventCfg.get(equipmentid);
		if (null == equip_cfg) return "";
		
		EventCfg eventcfg = equip_cfg.get(eventid);
		if (null == eventcfg) return "";
		
		return eventcfg.eventname;
	}
	
	/**
	 * Adapter getEventName
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @return String 告警名称
	 * @throws 
	 */
	public static String getEventName(int equipmentid, int eventid)
	{
		return getEventName(String.valueOf(equipmentid), String.valueOf(eventid));
	}
	
	/**
	 * 设置告警名称
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @param String 告警名称
	 * @return boolean true为成功
	 * @throws 
	 */
	public static boolean setEventName(String equipmentid, String eventid, String eventname)
	{
		Hashtable<String, EventCfg> equip_cfg = equipment.htEventCfg.get(equipmentid);
		if (null == equip_cfg) return false;
		
		EventCfg eventcfg = equip_cfg.get(eventid);
		if (null == eventcfg) return false;
		
		eventcfg.eventname = new String(eventname);
		
		// 通知名称有更新
		Iterator<IObject> regobj_it = eventcfg.registedNameObj.iterator();
		while (regobj_it.hasNext())
		{
			regobj_it.next().needupdate(true);
		}
		
		return true;
	}
	
	/**
	 * Adapter setEventName
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @param String 告警名称
	 * @return boolean true为成功
	 * @throws 
	 */
	public static boolean setEventName(int equipmentid, int eventid, String eventname)
	{
		return setEventName(String.valueOf(equipmentid), String.valueOf(eventid), eventname);
	}
	
	/**
	 * 获取告警条件开始比较阈值
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @param String 条件ID
	 * @return String 起始阈值
	 * @throws 
	 */
	public static String getStartCmpValue(String equipmentid, String eventid, String conditionId)
	{
		Hashtable<String, EventCfg> equip_cfg = equipment.htEventCfg.get(equipmentid);
		if (null == equip_cfg) return "";
		
		EventCfg eventcfg = equip_cfg.get(eventid);
		if (null == eventcfg) return "";
		
		EventConditionCfg condcfg = eventcfg.htConditions.get(conditionId);
		if (null == condcfg) return "";
		
		return condcfg.startcompare;
	}
	
	/**
	 * Adapter getStartCmpValue
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @param int 条件ID
	 * @return String 起始阈值
	 * @throws 
	 */
	public static String getStartCmpValue(int equipmentid, int eventid, int conditionId)
	{
		return getStartCmpValue(String.valueOf(equipmentid), String.valueOf(eventid), String.valueOf(conditionId));
	}
	
	/**
	 * 获取告警使能状态
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @return String 使能状态, 未找到告警配置项则返回空字符串。
	 * @throws 
	 */
	public static String getEventState(String equipmentid, String eventid)
	{
		Hashtable<String, EventCfg> equip_cfg = equipment.htEventCfg.get(equipmentid);
		if (null == equip_cfg) return "";
		
		EventCfg eventcfg = equip_cfg.get(eventid);
		if (null == eventcfg) return "";
		
		return String.valueOf(eventcfg.enabled);
	}
	
	/**
	 * Adapter getEventState
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @return String 使能状态, 未找到告警配置项则返回空字符串。
	 * @throws 
	 */
	public static String getEventState(int equipmentid, int eventid)
	{
		return getEventState(String.valueOf(equipmentid), String.valueOf(eventid));
	}
	
	/**
	 * 设定告警使能状态
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @param String 使能状态， 1 告警， 0 不告警。
	 * @return boolean 设定成功返回 true, 未找到相关告警配置返回 false.
	 * @throws 
	 */
	public static boolean setEventState(String equipmentid, String eventid, String enable)
	{
		Hashtable<String, EventCfg> equip_cfg = equipment.htEventCfg.get(equipmentid);
		if (null == equip_cfg) return false;
		
		EventCfg eventcfg = equip_cfg.get(eventid);
		if (null == eventcfg) return false;
		
		eventcfg.enabled = "1".equals(enable) ? 1 : 0;
		
		return true;
	}
	
	/**
	 * Adapter setEventState
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @param int 使能状态， 1 告警， 0 不告警。
	 * @return boolean 设定成功返回 true, 未找到相关告警配置返回 false.
	 * @throws 
	 */
	public static boolean setEventState(int equipmentid, int eventid, int enable)
	{
		return setEventState(String.valueOf(equipmentid), String.valueOf(eventid), String.valueOf(enable));
	}
	
	/**
	 * 登记到信号名称， Attention： 非线程安全
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean regSignalName(String equipmentid, String signalid , String pagename, IObject object)
	{
		if (null == equipmentid || null == signalid || null == pagename) return false;
		
		// 添加到设备集合
		equipment.hsEquipSet.add(equipmentid);  // SET 会解决重复问题
		
		// 添加到组态页面对应的设备集
		HashSet<String> equipset = equipment.htPageEquipSet.get(pagename);
		
		if (null == equipset) 
		{
			equipset = new HashSet<String>();
			equipment.htPageEquipSet.put(pagename, equipset);
		}
		
		equipset.add(equipmentid);
		
		// 下面将注册设备信号
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)
		{
			equip = equipment.new Equipment();
			equip.m_equipid = equipmentid;
			
			// 获取请求协议包
			equip.signal_req = protocol.build_query_signal_list(Integer.parseInt(equipmentid));
			
			// 添加该设备
			equipment.htEquipmentData.put(equipmentid, equip);
		}
		
		boolean bPutSignal =  false;
		Signal signel = equip.htSignalData.get(signalid);
		
		if (null == signel)
		{
			bPutSignal =  true;
			signel = equipment.new Signal();
		}
		
		// 注册控件对象
		if (null != object)
		{
			// TODO: 处理可能出现的特殊情况下的重复添加。
			signel.registedNameObj.add(object);
		}
		
		if (bPutSignal) equip.htSignalData.put(signalid, signel);
		
		return true;
	}  /*  end of regSignalName  */

	/**
	 * Adapter登记到信号名称， Attention： 非线程安全
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean regSignalName(int nEquipId, int nSignalId , String pagename, IObject object) 
	{
		return regSignalName(String.valueOf(nEquipId), String.valueOf(nSignalId), pagename, object);
		
	}
	
	/**
	 * 登记信号， Attention： 非线程安全
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setSignal(String equipmentid, String signalid , String pagename, IObject object)
	{
		if (null == equipmentid || null == signalid || null == pagename) return false;
		
		// 添加到设备集合
		equipment.hsEquipSet.add(equipmentid);  // SET 会解决重复问题
		
		// 添加到组态页面对应的设备集
		HashSet<String> equipset = equipment.htPageEquipSet.get(pagename);
		
		if (null == equipset) 
		{
			equipset = new HashSet<String>();
			equipment.htPageEquipSet.put(pagename, equipset);
		}
		
		equipset.add(equipmentid);
		
		// 下面将注册设备信号
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)
		{
			equip = equipment.new Equipment();
			equip.m_equipid = equipmentid;
			
			// 获取请求协议包
			equip.signal_req = protocol.build_query_signal_list(Integer.parseInt(equipmentid));
			
			// 添加该设备
			equipment.htEquipmentData.put(equipmentid, equip);
		}
		
		boolean bPutSignal =  false;
		Signal signel = equip.htSignalData.get(signalid);
		
		if (null == signel)
		{
			bPutSignal =  true;
			signel = equipment.new Signal();
		}
		
		// 注册控件对象
		if (null != object)
		{
			// TODO: 处理可能出现的特殊情况下的重复添加。
			signel.registedObj.add(object);
		}
		
		if (bPutSignal) equip.htSignalData.put(signalid, signel);
		
		return true;
	}  /*  end of setSignal  */

	/**
	 * Adapter登记信号， Attention： 非线程安全
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setSignal(int nEquipId, int nSignalId , String pagename, IObject object) 
	{
		return setSignal(String.valueOf(nEquipId), String.valueOf(nSignalId), pagename, object);
		
	}
	
	/**
	 * 登记到告警名， Attention： 非线程安全
	 * 
	 * @param String 设备实例ID
	 * @param String 告警ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean regEventName(String equipmentid, String eventid , String pagename, IObject object)
	{
		// TODO: 尚未实现。 需实现告警名称改变时通知相关控件更新。
		return false;
	}
	
	/**
	 * Adapter 登记到告警名
	 * 
	 * @param int 设备实例ID
	 * @param int 告警ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean regEventName(int nEquipId, int nEventId , String pagename, IObject object)
	{
		return regEventName(String.valueOf(nEquipId), String.valueOf(nEventId), pagename, object);
	}
	
	/**
	 * 登记告警控件， Attention： 非线程安全
	 * 
	 * @param String 设备实例ID
	 * @param String 信号ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setAlarmSignal(String equipmentid, String signalid , String pagename, IObject object)
	{
		if (null == object) return false;
		
		if (!setSignal(equipmentid, signalid, pagename, null)) return false;
		
		equipment.htEquipmentData.get(equipmentid).htSignalData.get(signalid).registedAlarmObj.add(object);
		return true;
	}
	
	/**
	 * Adapter 登记告警控件
	 * 
	 * @param int 设备实例ID
	 * @param int 信号ID
	 * @param String 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setAlarmSignal(int nEquipId, int nSignalId , String pagename, IObject object)
	{
		return setAlarmSignal(String.valueOf(nEquipId), String.valueOf(nSignalId), pagename, object);
	}
	
	/**
	 * 注册信号列表控件， Attention： 非线程安全
	 * 
	 * @param String equipId 设备实例ID
	 * @param String pagename 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setSignalList(String equipId, String pagename, IObject object)
	{
		if (null == equipId || null == object) return false;
		
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipId);
		
		if (null == equip)
		{
			equip = equipment.new Equipment();
			equip.m_equipid = equipId;
			
			// 获取请求协议包
			equip.signal_req = protocol.build_query_signal_list(Integer.parseInt(equipId));
			
			// 添加该设备
			equipment.htEquipmentData.put(equipId, equip);
		}
		
		// 添加到组态页面对应的设备集
		HashSet<String> equipset = equipment.htPageEquipSet.get(pagename);
		
		if (null == equipset) 
		{
			equipset = new HashSet<String>();
			equipment.htPageEquipSet.put(pagename, equipset);
		}
		
		equipset.add(equipId);
		
		return equip.registedLstObj.add(object);
	}
	
	/**
	 * setSignalList Adapter
	 * 
	 * @param int nEquipId 设备实例ID
	 * @param String pagename 页面名称
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setSignalList(int nEquipId, String pagename, IObject object)
	{
		return setSignalList(String.valueOf(nEquipId), pagename, object);
	}
	
	/**
	 * 注册全局告警列表控件
	 * 
	 * @param IObject
	 * @return boolean true成功，false失败。
	 * @throws 
	 */
	public static boolean setMainAlarmList(IObject object)
	{
		if (null == object) return false;
		return equipment.lstRegistedMainAlarmList.add(object);
	}
	
	// 请求、处理实时信号
	private static boolean proc_rtsignal(Equipment equipObj) 
	{
		if (null == equipObj) return false;
		byte[] recv_buf = service.send_and_receive(equipObj.signal_req, service.IP, service.PORT);
		
		if (null == recv_buf) return false;
		
		msg_head head = protocol.parse_msg_head(recv_buf);
		if (head.cmd != msg_type.MSG_QUERY_SIG_LIST_RT_ACK) return false;
		
		int sig_no = head.para;
		int i = 0;

		// TODO: 寻求不拷贝方案
		byte[] body_buf = new byte[head.length];
		if (head.length > 1)
		{
			try
			{
				System.arraycopy(recv_buf, protocol.MSG_HEAD_LEN, body_buf, 0, head.length);
			}
			catch (Exception e)
			{
				return false;
			}
		}
		
		String body = new String(body_buf);
		
		Signal signal = null;
		String[] blocks = body.split("\\|");
		
		// 检测采集器有无更新，若无跟新直接返回。
		if (sig_no > 0)
		{
			String[] items = blocks[0].split("`");
			if (equipObj.strSampleUpdateTime.equals(items[2])) return true;
			else equipObj.strSampleUpdateTime = new String(items[2]);
		}
		
		// 更新
		boolean equipupdated = false; // 记录这个设备有无数据更新
		boolean haseveritychanged = false; // 记录该设备告警状态是否有更新
		for (i = 0; i < sig_no; ++i) 
		{
			String[] items = blocks[i].split("`");
			
			signal = equipObj.htSignalData.get(items[1]);
			if (null == signal) continue;
			
			try
			{
				int severity;
				String meaning = "";
				
				signal.freshtime = Integer.parseInt(items[2]);
				signal.is_invalid = Integer.parseInt(items[3]);
				signal.value_type = Integer.parseInt(items[4]);
				
				if (!items[5].equals(signal.value))
				{
					equipupdated = true;
					signal.value = new String(items[5]);
				}
				
				severity = Integer.parseInt(items[6]);
				if (signal.severity != severity)
				{
					haseveritychanged = true;
					signal.severity = severity;
					
					// 通知告警级别改变。
					Iterator<IObject> regalarmobj_it = signal.registedAlarmObj.iterator();
					while (regalarmobj_it.hasNext())
					{
						regalarmobj_it.next().needupdate(true);
					}
				}
				
				if (7 < items.length) meaning = items[7].trim();
				//else signal.meaning = "";
				
				// 精度处理
				if (meaning.isEmpty())
				{
					BigDecimal bd = new BigDecimal(signal.value);
					bd = bd.setScale(signal.precision, BigDecimal.ROUND_HALF_UP);
					meaning = bd.toString();
				}
				
				if (!meaning.equals(signal.meaning))
				{
					signal.meaning = new String(meaning);
					
					// 通知信号有更新
					Iterator<IObject> regobj_it = signal.registedObj.iterator();
					while (regobj_it.hasNext())
					{
						regobj_it.next().needupdate(true);
					}
				}
			}
			catch (Exception e)
			{
				// TODO: 异常日志处理
				;
			}
		}
		
		if (equipupdated)
		{
            // 处理该设备有任意更新时动作。
			Iterator<IObject> reglstobj_it = equipObj.registedLstObj.iterator();
			while (reglstobj_it.hasNext())
			{
				reglstobj_it.next().needupdate(true);
			}
		}
		
		/*  该代码尚未有运作价值
		if (haseveritychanged)
		{
            // 处理该设备告警信息有更新时动作。
			
			// 处理注册的 主告警列表(所有告警信息) 控件
			Iterator<IObject> reglstobj_it = equipment.lstRegistedMainAlarmList.iterator();
			while (reglstobj_it.hasNext())
			{
				reglstobj_it.next().needupdate(true);
			}
			
			// TODO: 处理该设备的告警列表控件
		}
		*/
		
		return true;
	}  // end of proc_rtsignal
	
	// 采集、处理 所有实时告警
	private static boolean proc_allrtalarm()
	{
		byte[] recv_buf = service.send_and_receive(equipment.rtalarm_req, service.IP, service.PORT);
		
		if (recv_buf == null)
		{
			return false;
		}
		
		msg_head head = protocol.parse_msg_head(recv_buf);
		
		if (head.cmd != msg_type.MSG_ID_QUERY_ALL_ACTIVE_ALARM_ACK)
		{
			return false;
		}
		
		int block_no = head.para;
		int i = 0;

		// TODO: 寻求不拷贝方案
		byte[] body_buf = new byte[head.length];
		if (head.length > 1)
		{
			try
			{
				System.arraycopy(recv_buf, protocol.MSG_HEAD_LEN, body_buf, 0, head.length);
			} catch (Exception e)
			{
				;
			}
		}
		
		//if (equipment.rtalarm_rspbody.length() == head.length) 
		//{
			//if (equipment.rtalarm_rspbody.equals(body_buf)) return true;
		//}
		
		String body =  new String(body_buf);
		if (equipment.rtalarm_rspbody.equals(body)) return true;
		
		equipment.rtalarm_rspbody = body;

		// TODO: 处理更新
		Event event = null;
		Hashtable<String, Event> equipevt = null;
		Hashtable<String, Hashtable<String, Event>> eventData = new Hashtable<String, Hashtable<String, Event>>();
		String[] blocks = equipment.rtalarm_rspbody.split("\\|");
		for (i = 0; i < block_no; ++i)
		{
			String[] items = blocks[i].split("`");
			ipc_active_event alarm = new ipc_active_event();

			try
			{
				alarm.equipid = Integer.parseInt(items[0]);
				alarm.eventid = Integer.parseInt(items[1]);
				
				// 获取告警设备
				//equipevt = equipment.htEventData.get(items[0]);
				equipevt = eventData.get(items[0]);
				if (null == equipevt)
				{
					equipevt = new Hashtable<String, Event>();
					eventData.put(new String(items[0]), equipevt);
					//equipment.htEventData.put(items[0], equipevt);
				}
				
				// 获取告警项
				event = equipevt.get(items[1]);
				if (null == event)
				{
					event = equipment.new Event();
					equipevt.put(new String(items[1]), event);
				}				
				
				event.name = getEventName(items[0], items[1]);
				// TODO: 实现getEventSignalId函数
				//event.value = getSignalValue(items[0], getEventSignalId(items[0], items[1]));
				event.starttime = Integer.parseInt(items[2]);
				event.stoptime = Integer.parseInt(items[3]);
				//alarm.is_active = Integer.parseInt(items[4]);
				event.grade = Integer.parseInt(items[5]);
				event.meaning = (null == items[6] ? "" : new String(items[6]));
			} catch (Exception e)
			{
				;
			}
		}
		
		equipment.htEventData = eventData;
		
		// 通知控件告警信息变更
		Iterator<IObject> reglstobj_it = equipment.lstRegistedMainAlarmList.iterator();
		while (reglstobj_it.hasNext())
		{
			reglstobj_it.next().needupdate(true);
		}
		
		return true;
	}  // end of proc_allrtalarm
	
	// 内部结构 ===========================================================
	// 解决读取迭代器时，其他线程增加项目导致的不可捕获型异常 ConcurrentModificationException 。
	public class CopiedIterator<T> implements Iterator<T>
	{
		private Iterator<T> iterator = null;

		public CopiedIterator(Iterator<T> itr)
		{
			LinkedList<T> list = new LinkedList<T>();
			while (itr.hasNext())
			{
				list.add(itr.next());
			}
			this.iterator = list.iterator();
		}

		public boolean hasNext()
		{
			return this.iterator.hasNext();
		}

		public void remove()
		{
			throw new UnsupportedOperationException("This is a read-only iterator.");
		}

		public T next()
		{
			return this.iterator.next();
		}
	}
	
	// 成员数据 ===========================================================
	public static EquipmentDataModel equipment = new EquipmentDataModel();
	public static String currentPage = "";
	public static boolean bIsLoading = false;
}
