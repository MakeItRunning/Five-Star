package com.sg.common;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

public class UtIniReader {
    protected HashMap<String, Properties> sections = new HashMap<String, Properties>();
    private transient String currentSecion;
    private transient Properties current;
    public UtIniReader(String filename) throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "gb2312"));
	    read(reader);
	    reader.close();
    }

    protected void read(BufferedReader reader) throws IOException {
    	String line;
        while ((line = reader.readLine()) != null) {
        	parseLine(line);
        }
    }

    protected void parseLine(String line) {
    	line = line.trim();
    	
    	if (line.isEmpty() || '#' == line.charAt(0)) return;    // 使配置文件支持 # 注释  -- CharlesChen TODO: 尚未验证
    	
    	if (line.matches("\\[.*\\]")) {
    		currentSecion = line.replaceFirst("\\[(.*)\\]", "$1");
    		current = new Properties();
    		sections.put(currentSecion, current);
    	} else if (line.matches(".*=.*")) {
    		if (current != null) {
    			int i = line.indexOf('=');
    			String name = line.substring(0, i);
    			String value = line.substring(i + 1);
    			current.setProperty(name, value);
    		}
        }
    }

    public String getValue(String section, String name) {
    	Properties p = (Properties) sections.get(section);
    	if (p == null) {
    		return null;
        }
    	String value = p.getProperty(name);
        return value;
    }
    
    public String getValue(String section, String name, String defualt) {
    	Properties p = (Properties) sections.get(section);
    	if (p == null) {
    		return defualt;
        }
    	String value = p.getProperty(name);
        return null==value?defualt:value;
    }
        
}


/*使用方法：
IniReader iniReader = new IniReader(strFileName);
String strTmp = iniReader.getValue("Section1", "Key1");*/