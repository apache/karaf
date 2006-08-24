/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.tool.mangen;

import java.io.IOException;
import java.io.PrintStream;

import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import java.util.jar.Attributes;

/**
 *
 * @version $Revision: 31 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */ 
public class RuleHandler
        extends GenericHandler
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    /** Classname prefix for basic rule classes */ 
    public static final String  RULE_PACKAGE = "org.apache.felix.tool.mangen.rule";
    /** Rule set property **/
    public static final String  RULESET_KEY = "mangen.rulesets";
    /** Default rule property prefix **/
    public static final String  RULE_KEY = "mangen-rule-";
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    /**
     * Initialise a set of rules to be run, based on the value of the
     * mangen.rule property if present, or the default rule key if not.
     */
    public static RuleHandler[] initRuleSets()
    {
        String rulesetStr = PropertyManager.getProperty(RULESET_KEY, RULE_KEY);
        
        StringTokenizer tok = new StringTokenizer(rulesetStr, ",");
        RuleHandler[] handlers = new RuleHandler[tok.countTokens()];
        for (int ix = 0; ix < handlers.length; ix++)
        {
            handlers[ix] = new RuleHandler(tok.nextToken().trim());
        }
        
        return handlers;
    }
     
    /**
     * Run each specified ruleset.
     */
    public static void runRuleSets(RuleHandler[] handlers, List jarList)
    {
        for (int ix = 0; ix < handlers.length; ix++)
        {
            handlers[ix].executeRules(jarList);
        }
    }
    
    /**
     *
     * Run reports for each specified ruleset.
     */
    public static void runRuleSetReports(RuleHandler[] handlers, PrintStream rpt, boolean header)
            throws IOException
    {
        for (int ix = 0; ix < handlers.length; ix++)
        {
            handlers[ix].runReports(rpt, header);
        }
    }
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    public boolean  isGlobalRule;
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    /**
     * Initialise a rule handler usng the default RULE_KEY. 
     */
    public RuleHandler()
    {
        this(RULE_KEY);
    }

    /**
     * Initialise the set of rules based on the RULE_KEY.<n> properties defined.
     * Each rule is either a full classname implementing the Rule interface, or a 
     * classname in RULE_PACKAGE. The actual work to process the properties and
     * create the rule objects is done by our parent class, GenericHandler. 
     */
    public RuleHandler(String key)
    {
        super(key, Rule.class, RULE_PACKAGE);
        // for now, safe to assume Property set based rules are global
        this.isGlobalRule = true;
    }

    /**
     * Initialise a rule handler based on a set of Manifest attributes.
     */
    public RuleHandler(Attributes atts)
    {
        super(atts, RULE_KEY, Rule.class, RULE_PACKAGE);
        // for now, safe to assume Attribute set based rules are local
        this.isGlobalRule = false;
    }
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    /**
     * Retrieve each defined rule and apply it. At present we take the approach that 
     * each rule will be applied in it's entirety before proceeding to the next rule. 
     * For simple rules, this will result in multiple passes over 
     * the jarList set for simowhich obviously incurs a processing overhead, but it is felt 
     * that this simpler approach will have fewer nasty side-effects for more complex 
     * rules that span multiple bundle jars.
     */
    public void executeRules(List jarList)
    {
        for(Iterator i = handlerList.iterator(); i.hasNext(); )
        {
            Rule rule = (Rule) i.next();
            
            if (isGlobalRule && !rule.isUsableGlobally())
            {
                throw new IllegalArgumentException("rule cannot be used globally: " 
                        + rule.getClass().getName());
            }
            
            if (!isGlobalRule && !rule.isUsableLocally())
            {
                throw new IllegalArgumentException("rule cannot be used locally: " 
                        + rule.getClass().getName());
            }
            
            rule.execute(jarList);
        }
    }
        
    /**
     * Run report for each rule. Bit of a hack for now! 
     */
    public void runReports(PrintStream rpt, boolean header)
            throws IOException
    {
        for(Iterator i = handlerList.iterator(); i.hasNext(); )
        {
            Rule rule = (Rule) i.next();
            
            if (header)
            {
                rpt.println("");
                rpt.println("============================================================");
                rpt.println("Rule:  " + rule.getClass().getName());        
                rpt.println("============================================================");
                rpt.println("");
            }
            else
            {
                rpt.println("Rule:  " + rule.getClass().getName());        
            }

            rule.report(rpt);
        }
    }
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    
}
