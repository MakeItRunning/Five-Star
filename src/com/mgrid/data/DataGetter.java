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
		// TODO: ��ʼ���澯������Ϣ��IPC���޷���ȡ�����澯������Ϣ��
		
		// ��ʼ���豸�б� �豸�澯������Ϣ
		while(true)
		{
			//if (equipment.bEquipments) break;
			
			List<ipc_equipment> equip_list = service.get_equipment_list(service.IP, service.PORT);
			
			// �����ȡ�����豸�б�Ϊ�գ��ȴ�������ظ���ȡ��
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
				
				// a. �����豸��Ϣ
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
				
				// b. ��ȡ���豸�澯������Ϣ
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
				
				// c. ��ȡ�¼��澯��Ϣ & �澯��ֵ��Ϣ
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
					
					// �����豸�������¼�����
					EventCfg equipEvtCfg = equipobj.htEventCfg.get(String.valueOf(ipc_trigger.eventid));
					if (null == equipEvtCfg) continue;
					equipEvtCfg.htConditions.put(String.valueOf(ipc_trigger.conditionid), condcfg);
				}
				
				// d. ��ȡ�ź�������Ϣ
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
				
				// e. ��ȡ����������Ϣ && ���Ʋ���������Ϣ
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
				
				// ��ȡ���Ʋ�������
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
		}  /* end of while �豸�б��澯���� */
		
		Equipment currEquipObj = null;
		
		// ʵʱ���ݳ�ʼ����Ԥ�Ƚ������豸ʵʱ����ˢ��һ�顣TODO: ʵʱ�澯Ԥ����
		Iterator<String> equipNameIt = equipment.hsEquipSet.iterator();
		while(equipNameIt.hasNext())
		{
			currEquipObj = equipment.htEquipmentData.get(equipNameIt.next());
			if (null == currEquipObj) continue;  // �����ܳ��֣�����
			
			proc_rtsignal(currEquipObj);
		}
		
		// �߳���ѭ������һ��ѭ�� 2 S ���ɼ�������ʱ�䣩 Ϊʱ����ա�  *** Master while ***
		while(true)
		{
			try
			{
				sleep(100);  // ����sleep�� ��ֹ�豸�б�Ϊ��ʱ��ѭ����ת��
			} catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}
			
			// �������и澯��Ϣ
			proc_allrtalarm();
			
			// ����ҳ����ظ��豸ʵʱ����
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
				if (null == currEquipObj) continue;  // �����ܳ��֣�����
				
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
			
			// �������� ��ʱδ�����豸 ʵʱ����
			// fix java.util.ConcurrentModificationException
			if (bIsLoading)
				equipNameIt = new CopiedIterator<String>(equipment.hsEquipSet.iterator());
			else
				equipNameIt = equipment.hsEquipSet.iterator();
			
			while(equipNameIt.hasNext())
			{
				currEquipObj = equipment.htEquipmentData.get(equipNameIt.next());
				if (null == currEquipObj) continue;  // �����ܳ��֣�����
				
				// ���� 30 S ���и��µ��豸�����в���
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
			
			// �ֶ� GC
			//System.gc();
		} // end of master while
	} // end of run()
	
	/**
	 * ��ȡ�豸�źż����̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @return �ɹ�����ʵ���źż�Hashtable�����򷵻�null��
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
	 * @param int �豸ʵ��ID
	 * @return �ɹ�����ʵ���źż�Hashtable�����򷵻�null��
	 * @throws 
	 */
	public static Hashtable<String, Signal> getEquipSignalList(int equipmentid)
	{
		return getEquipSignalList(String.valueOf(equipmentid));
	}
	
	/**
	 * ��ȡ ȫ��ʵʱ�澯�б��̰߳�ȫ��
	 * 
	 * @param
	 * @return ȫ�ָ澯��ϢHashtable��
	 * @throws 
	 */
	public static Hashtable<String, Hashtable<String, Event>> getRTEventList()
	{
		return equipment.htEventData;
	}
	
	/**
	 * ��ȡ �豸ʵʱ�澯�б��̰߳�ȫ��
	 * 
	 * @param
	 * @return �豸�豸�澯��ϢHashtable��
	 * @throws 
	 */
	public static Hashtable<String, Hashtable<String, Event>> getEquipRTEventList(String equipid)
	{
		// TODO: ��δ���
		Hashtable<String, Hashtable<String, Event>> hashtable = new Hashtable<String, Hashtable<String, Event>>();
		
		hashtable.put(equipid, equipment.htEventData.get(equipid));
		return hashtable;
	}
	
	/**
	 * Adapter of ��ȡ �豸ʵʱ�澯�б��̰߳�ȫ��
	 * 
	 * @param
	 * @return �豸�澯��ϢHashtable��
	 * @throws 
	 */
	public static Hashtable<String, Hashtable<String, Event>> getEquipRTEventList(int nEquipId)
	{
		return getEquipRTEventList(String.valueOf(nEquipId));
	}
	
	/**
	 * ��ȡ�ź����� �̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @return String �ź���
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
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @return String �ź���
	 * @throws 
	 */
	public static String getSignalName(int equipmentid, int signalid)
	{
		return getSignalName(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * �����ź����� �̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @param String �ź���
	 * @return boolean trueΪ�ɹ�
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
		
		// ֪ͨ�����и���
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
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @param String �ź���
	 * @return boolean trueΪ�ɹ�
	 * @throws 
	 */
	public static boolean setSignalName(int equipmentid, int signalid, String signalname)
	{
		return setSignalName(String.valueOf(equipmentid), String.valueOf(signalid), signalname);
	}
	
	/**
	 * ��ȡ�ź�ֵ�� �̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @return String �ź�ֵ
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
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @return String �ź�ֵ
	 * @throws 
	 */
	public static String getSignalValue(int equipmentid, int signalid)
	{
		return getSignalValue(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * ��ȡ�źź��壬 �̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @return String �źź���
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
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @return String �źź���
	 * @throws 
	 */
	public static String getSignalMeaning(int equipmentid, int signalid)
	{
		return getSignalMeaning(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * ��ȡ�ź������� �̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @return String �ź�����
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
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @return String �ź�����
	 * @throws 
	 */
	public static String getSignalDescription(int equipmentid, int signalid)
	{
		return getSignalDescription(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * ��ȡ�źŸ澯���� �̰߳�ȫ��
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @return int �澯����
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
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @return int �澯����
	 * @throws 
	 */
	public static int getSignalSeverity(int equipmentid, int signalid)
	{
		return getSignalSeverity(String.valueOf(equipmentid), String.valueOf(signalid));
	}
	
	/**
	 * ��ȡ���Ʋ������弯�ϡ�
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @return List<CmdParameaningCfg> �澯����������Ϣ��
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
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @return List<CmdParameaningCfg> �澯����������Ϣ��
	 * @throws 
	 */
	public static List<CmdParameaningCfg> getCtrlParameaning(int equipmentid, int commandid)
	{
		return getCtrlParameaning(String.valueOf(equipmentid), String.valueOf(commandid));
	}
	
	/**
	 * ��ȡ�豸���ƣ����������򷵻ؿ��ַ�����
	 * 
	 * @param String �豸ʵ��ID
	 * @return String �豸����
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
	 * @param int �豸ʵ��ID
	 * @return String �豸����
	 * @throws 
	 */
	public static String getEquipmentName(int equipmentid)
	{
		return getEquipmentName(String.valueOf(equipmentid));
	}
	
	/**
	 * ��ȡ�澯����
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @return String �澯����
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
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @return String �澯����
	 * @throws 
	 */
	public static String getEventName(int equipmentid, int eventid)
	{
		return getEventName(String.valueOf(equipmentid), String.valueOf(eventid));
	}
	
	/**
	 * ���ø澯����
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @param String �澯����
	 * @return boolean trueΪ�ɹ�
	 * @throws 
	 */
	public static boolean setEventName(String equipmentid, String eventid, String eventname)
	{
		Hashtable<String, EventCfg> equip_cfg = equipment.htEventCfg.get(equipmentid);
		if (null == equip_cfg) return false;
		
		EventCfg eventcfg = equip_cfg.get(eventid);
		if (null == eventcfg) return false;
		
		eventcfg.eventname = new String(eventname);
		
		// ֪ͨ�����и���
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
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @param String �澯����
	 * @return boolean trueΪ�ɹ�
	 * @throws 
	 */
	public static boolean setEventName(int equipmentid, int eventid, String eventname)
	{
		return setEventName(String.valueOf(equipmentid), String.valueOf(eventid), eventname);
	}
	
	/**
	 * ��ȡ�澯������ʼ�Ƚ���ֵ
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @param String ����ID
	 * @return String ��ʼ��ֵ
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
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @param int ����ID
	 * @return String ��ʼ��ֵ
	 * @throws 
	 */
	public static String getStartCmpValue(int equipmentid, int eventid, int conditionId)
	{
		return getStartCmpValue(String.valueOf(equipmentid), String.valueOf(eventid), String.valueOf(conditionId));
	}
	
	/**
	 * ��ȡ�澯ʹ��״̬
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @return String ʹ��״̬, δ�ҵ��澯�������򷵻ؿ��ַ�����
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
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @return String ʹ��״̬, δ�ҵ��澯�������򷵻ؿ��ַ�����
	 * @throws 
	 */
	public static String getEventState(int equipmentid, int eventid)
	{
		return getEventState(String.valueOf(equipmentid), String.valueOf(eventid));
	}
	
	/**
	 * �趨�澯ʹ��״̬
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @param String ʹ��״̬�� 1 �澯�� 0 ���澯��
	 * @return boolean �趨�ɹ����� true, δ�ҵ���ظ澯���÷��� false.
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
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @param int ʹ��״̬�� 1 �澯�� 0 ���澯��
	 * @return boolean �趨�ɹ����� true, δ�ҵ���ظ澯���÷��� false.
	 * @throws 
	 */
	public static boolean setEventState(int equipmentid, int eventid, int enable)
	{
		return setEventState(String.valueOf(equipmentid), String.valueOf(eventid), String.valueOf(enable));
	}
	
	/**
	 * �Ǽǵ��ź����ƣ� Attention�� ���̰߳�ȫ
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean regSignalName(String equipmentid, String signalid , String pagename, IObject object)
	{
		if (null == equipmentid || null == signalid || null == pagename) return false;
		
		// ��ӵ��豸����
		equipment.hsEquipSet.add(equipmentid);  // SET �����ظ�����
		
		// ��ӵ���̬ҳ���Ӧ���豸��
		HashSet<String> equipset = equipment.htPageEquipSet.get(pagename);
		
		if (null == equipset) 
		{
			equipset = new HashSet<String>();
			equipment.htPageEquipSet.put(pagename, equipset);
		}
		
		equipset.add(equipmentid);
		
		// ���潫ע���豸�ź�
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)
		{
			equip = equipment.new Equipment();
			equip.m_equipid = equipmentid;
			
			// ��ȡ����Э���
			equip.signal_req = protocol.build_query_signal_list(Integer.parseInt(equipmentid));
			
			// ��Ӹ��豸
			equipment.htEquipmentData.put(equipmentid, equip);
		}
		
		boolean bPutSignal =  false;
		Signal signel = equip.htSignalData.get(signalid);
		
		if (null == signel)
		{
			bPutSignal =  true;
			signel = equipment.new Signal();
		}
		
		// ע��ؼ�����
		if (null != object)
		{
			// TODO: ������ܳ��ֵ���������µ��ظ���ӡ�
			signel.registedNameObj.add(object);
		}
		
		if (bPutSignal) equip.htSignalData.put(signalid, signel);
		
		return true;
	}  /*  end of regSignalName  */

	/**
	 * Adapter�Ǽǵ��ź����ƣ� Attention�� ���̰߳�ȫ
	 * 
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean regSignalName(int nEquipId, int nSignalId , String pagename, IObject object) 
	{
		return regSignalName(String.valueOf(nEquipId), String.valueOf(nSignalId), pagename, object);
		
	}
	
	/**
	 * �Ǽ��źţ� Attention�� ���̰߳�ȫ
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean setSignal(String equipmentid, String signalid , String pagename, IObject object)
	{
		if (null == equipmentid || null == signalid || null == pagename) return false;
		
		// ��ӵ��豸����
		equipment.hsEquipSet.add(equipmentid);  // SET �����ظ�����
		
		// ��ӵ���̬ҳ���Ӧ���豸��
		HashSet<String> equipset = equipment.htPageEquipSet.get(pagename);
		
		if (null == equipset) 
		{
			equipset = new HashSet<String>();
			equipment.htPageEquipSet.put(pagename, equipset);
		}
		
		equipset.add(equipmentid);
		
		// ���潫ע���豸�ź�
		EquipmentDataModel.Equipment equip = equipment.htEquipmentData.get(equipmentid);
		
		if (null == equip)
		{
			equip = equipment.new Equipment();
			equip.m_equipid = equipmentid;
			
			// ��ȡ����Э���
			equip.signal_req = protocol.build_query_signal_list(Integer.parseInt(equipmentid));
			
			// ��Ӹ��豸
			equipment.htEquipmentData.put(equipmentid, equip);
		}
		
		boolean bPutSignal =  false;
		Signal signel = equip.htSignalData.get(signalid);
		
		if (null == signel)
		{
			bPutSignal =  true;
			signel = equipment.new Signal();
		}
		
		// ע��ؼ�����
		if (null != object)
		{
			// TODO: ������ܳ��ֵ���������µ��ظ���ӡ�
			signel.registedObj.add(object);
		}
		
		if (bPutSignal) equip.htSignalData.put(signalid, signel);
		
		return true;
	}  /*  end of setSignal  */

	/**
	 * Adapter�Ǽ��źţ� Attention�� ���̰߳�ȫ
	 * 
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean setSignal(int nEquipId, int nSignalId , String pagename, IObject object) 
	{
		return setSignal(String.valueOf(nEquipId), String.valueOf(nSignalId), pagename, object);
		
	}
	
	/**
	 * �Ǽǵ��澯���� Attention�� ���̰߳�ȫ
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �澯ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean regEventName(String equipmentid, String eventid , String pagename, IObject object)
	{
		// TODO: ��δʵ�֡� ��ʵ�ָ澯���Ƹı�ʱ֪ͨ��ؿؼ����¡�
		return false;
	}
	
	/**
	 * Adapter �Ǽǵ��澯��
	 * 
	 * @param int �豸ʵ��ID
	 * @param int �澯ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean regEventName(int nEquipId, int nEventId , String pagename, IObject object)
	{
		return regEventName(String.valueOf(nEquipId), String.valueOf(nEventId), pagename, object);
	}
	
	/**
	 * �ǼǸ澯�ؼ��� Attention�� ���̰߳�ȫ
	 * 
	 * @param String �豸ʵ��ID
	 * @param String �ź�ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
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
	 * Adapter �ǼǸ澯�ؼ�
	 * 
	 * @param int �豸ʵ��ID
	 * @param int �ź�ID
	 * @param String ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean setAlarmSignal(int nEquipId, int nSignalId , String pagename, IObject object)
	{
		return setAlarmSignal(String.valueOf(nEquipId), String.valueOf(nSignalId), pagename, object);
	}
	
	/**
	 * ע���ź��б�ؼ��� Attention�� ���̰߳�ȫ
	 * 
	 * @param String equipId �豸ʵ��ID
	 * @param String pagename ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
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
			
			// ��ȡ����Э���
			equip.signal_req = protocol.build_query_signal_list(Integer.parseInt(equipId));
			
			// ��Ӹ��豸
			equipment.htEquipmentData.put(equipId, equip);
		}
		
		// ��ӵ���̬ҳ���Ӧ���豸��
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
	 * @param int nEquipId �豸ʵ��ID
	 * @param String pagename ҳ������
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean setSignalList(int nEquipId, String pagename, IObject object)
	{
		return setSignalList(String.valueOf(nEquipId), pagename, object);
	}
	
	/**
	 * ע��ȫ�ָ澯�б�ؼ�
	 * 
	 * @param IObject
	 * @return boolean true�ɹ���falseʧ�ܡ�
	 * @throws 
	 */
	public static boolean setMainAlarmList(IObject object)
	{
		if (null == object) return false;
		return equipment.lstRegistedMainAlarmList.add(object);
	}
	
	// ���󡢴���ʵʱ�ź�
	private static boolean proc_rtsignal(Equipment equipObj) 
	{
		if (null == equipObj) return false;
		byte[] recv_buf = service.send_and_receive(equipObj.signal_req, service.IP, service.PORT);
		
		if (null == recv_buf) return false;
		
		msg_head head = protocol.parse_msg_head(recv_buf);
		if (head.cmd != msg_type.MSG_QUERY_SIG_LIST_RT_ACK) return false;
		
		int sig_no = head.para;
		int i = 0;

		// TODO: Ѱ�󲻿�������
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
		
		// ���ɼ������޸��£����޸���ֱ�ӷ��ء�
		if (sig_no > 0)
		{
			String[] items = blocks[0].split("`");
			if (equipObj.strSampleUpdateTime.equals(items[2])) return true;
			else equipObj.strSampleUpdateTime = new String(items[2]);
		}
		
		// ����
		boolean equipupdated = false; // ��¼����豸�������ݸ���
		boolean haseveritychanged = false; // ��¼���豸�澯״̬�Ƿ��и���
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
					
					// ֪ͨ�澯����ı䡣
					Iterator<IObject> regalarmobj_it = signal.registedAlarmObj.iterator();
					while (regalarmobj_it.hasNext())
					{
						regalarmobj_it.next().needupdate(true);
					}
				}
				
				if (7 < items.length) meaning = items[7].trim();
				//else signal.meaning = "";
				
				// ���ȴ���
				if (meaning.isEmpty())
				{
					BigDecimal bd = new BigDecimal(signal.value);
					bd = bd.setScale(signal.precision, BigDecimal.ROUND_HALF_UP);
					meaning = bd.toString();
				}
				
				if (!meaning.equals(signal.meaning))
				{
					signal.meaning = new String(meaning);
					
					// ֪ͨ�ź��и���
					Iterator<IObject> regobj_it = signal.registedObj.iterator();
					while (regobj_it.hasNext())
					{
						regobj_it.next().needupdate(true);
					}
				}
			}
			catch (Exception e)
			{
				// TODO: �쳣��־����
				;
			}
		}
		
		if (equipupdated)
		{
            // ������豸���������ʱ������
			Iterator<IObject> reglstobj_it = equipObj.registedLstObj.iterator();
			while (reglstobj_it.hasNext())
			{
				reglstobj_it.next().needupdate(true);
			}
		}
		
		/*  �ô�����δ��������ֵ
		if (haseveritychanged)
		{
            // ������豸�澯��Ϣ�и���ʱ������
			
			// ����ע��� ���澯�б�(���и澯��Ϣ) �ؼ�
			Iterator<IObject> reglstobj_it = equipment.lstRegistedMainAlarmList.iterator();
			while (reglstobj_it.hasNext())
			{
				reglstobj_it.next().needupdate(true);
			}
			
			// TODO: ������豸�ĸ澯�б�ؼ�
		}
		*/
		
		return true;
	}  // end of proc_rtsignal
	
	// �ɼ������� ����ʵʱ�澯
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

		// TODO: Ѱ�󲻿�������
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

		// TODO: �������
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
				
				// ��ȡ�澯�豸
				//equipevt = equipment.htEventData.get(items[0]);
				equipevt = eventData.get(items[0]);
				if (null == equipevt)
				{
					equipevt = new Hashtable<String, Event>();
					eventData.put(new String(items[0]), equipevt);
					//equipment.htEventData.put(items[0], equipevt);
				}
				
				// ��ȡ�澯��
				event = equipevt.get(items[1]);
				if (null == event)
				{
					event = equipment.new Event();
					equipevt.put(new String(items[1]), event);
				}				
				
				event.name = getEventName(items[0], items[1]);
				// TODO: ʵ��getEventSignalId����
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
		
		// ֪ͨ�ؼ��澯��Ϣ���
		Iterator<IObject> reglstobj_it = equipment.lstRegistedMainAlarmList.iterator();
		while (reglstobj_it.hasNext())
		{
			reglstobj_it.next().needupdate(true);
		}
		
		return true;
	}  // end of proc_allrtalarm
	
	// �ڲ��ṹ ===========================================================
	// �����ȡ������ʱ�������߳�������Ŀ���µĲ��ɲ������쳣 ConcurrentModificationException ��
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
	
	// ��Ա���� ===========================================================
	public static EquipmentDataModel equipment = new EquipmentDataModel();
	public static String currentPage = "";
	public static boolean bIsLoading = false;
}
