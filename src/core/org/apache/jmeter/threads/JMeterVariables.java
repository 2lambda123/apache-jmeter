package org.apache.jmeter.threads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author default
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class JMeterVariables
{
    private Map variables = new HashMap();
    private int iteration = 0;

    public JMeterVariables()
    {}

    public String getThreadName()
    {
        return Thread.currentThread().getName();
    }

    public int getIteration()
    {
        return iteration;
    }

    public void incIteration()
    {
        iteration++;
    }

    public void initialize()
    {
        variables.clear();
    }

    public void put(String key, String value)
    {
        variables.put(key, value);
    }

    public void putAll(Map vars)
    {
        Iterator iter = vars.keySet().iterator();
        while (iter.hasNext())
        {
            String item = (String) iter.next();
            put(item, (String) vars.get(item));

        }
    }
    
    public void putAll(JMeterVariables vars)
    {
        putAll(vars.variables);
    }

    public String get(String key)
    {
        String val = (String) variables.get(key);
        if (val == null)
        {
            return "";
        }
        return val;
    }

}