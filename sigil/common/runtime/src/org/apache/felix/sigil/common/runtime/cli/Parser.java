/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.felix.sigil.common.runtime.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

/**
 * <code>Parser</code> creates {@link CommandLine}s.
 *
 * @author John Keyes (john at integralsource.com)
 * @version $Revision: 680644 $, $Date: 2008-07-29 01:13:48 -0700 (Tue, 29 Jul 2008) $
 */
public abstract class Parser implements CommandLineParser
{
    /** commandline instance */
    protected CommandLine cmd;

    /** current Options */
    private Options options;

    /** list of required options strings */
    private List requiredOptions;

    protected void setOptions(final Options options)
    {
        this.options = options;
        this.requiredOptions = new ArrayList(options.getRequiredOptions());
    }

    protected Options getOptions()
    {
        return options;
    }

    protected List getRequiredOptions()
    {
        return requiredOptions;
    }

    /**
     * Subclasses must implement this method to reduce
     * the <code>arguments</code> that have been passed to the parse method.
     *
     * @param opts The Options to parse the arguments by.
     * @param arguments The arguments that have to be flattened.
     * @param stopAtNonOption specifies whether to stop
     * flattening when a non option has been encountered
     * @return a String array of the flattened arguments
     */
    protected abstract String[] flatten(Options opts, String[] arguments, boolean stopAtNonOption);

    /**
     * Parses the specified <code>arguments</code> based
     * on the specifed {@link Options}.
     *
     * @param options the <code>Options</code>
     * @param arguments the <code>arguments</code>
     * @return the <code>CommandLine</code>
     * @throws ParseException if an error occurs when parsing the
     * arguments.
     */
    public CommandLine parse(Options options, String[] arguments) throws ParseException
    {
        return parse(options, arguments, null, false);
    }

    /**
     * Parse the arguments according to the specified options and properties.
     *
     * @param options    the specified Options
     * @param arguments  the command line arguments
     * @param properties command line option name-value pairs
     * @return the list of atomic option and value tokens
     * @throws ParseException if there are any problems encountered
     *                        while parsing the command line tokens.
     *
     * @since 1.1
     */
    public CommandLine parse(Options options, String[] arguments, Properties properties) throws ParseException
    {
        return parse(options, arguments, properties, false);
    }

    /**
     * Parses the specified <code>arguments</code>
     * based on the specifed {@link Options}.
     *
     * @param options         the <code>Options</code>
     * @param arguments       the <code>arguments</code>
     * @param stopAtNonOption specifies whether to stop interpreting the
     *                        arguments when a non option has been encountered
     *                        and to add them to the CommandLines args list.
     * @return the <code>CommandLine</code>
     * @throws ParseException if an error occurs when parsing the arguments.
     */
    public CommandLine parse(Options options, String[] arguments, boolean stopAtNonOption) throws ParseException
    {
        return parse(options, arguments, null, stopAtNonOption);
    }

    /**
     * Parse the arguments according to the specified options and
     * properties.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param properties command line option name-value pairs
     * @param stopAtNonOption stop parsing the arguments when the first
     * non option is encountered.
     *
     * @return the list of atomic option and value tokens
     *
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     *
     * @since 1.1
     */
    public CommandLine parse(Options options, String[] arguments, Properties properties, boolean stopAtNonOption)
            throws ParseException
    {
        // clear out the data in options in case it's been used before (CLI-71)
        for (Iterator it = options.helpOptions().iterator(); it.hasNext();)
        {
            Option opt = (Option) it.next();
            opt.clearValues();
        }

        // initialise members
        setOptions(options);

        cmd = new CommandLine();

        boolean eatTheRest = false;

        if (arguments == null)
        {
            arguments = new String[0];
        }

        List tokenList = Arrays.asList(flatten(getOptions(), arguments, stopAtNonOption));

        ListIterator iterator = tokenList.listIterator();

        // process each flattened token
        while (iterator.hasNext())
        {
            String t = (String) iterator.next();

            // the value is the double-dash
            if ("--".equals(t))
            {
                eatTheRest = true;
            }

            // the value is a single dash
            else if ("-".equals(t))
            {
                if (stopAtNonOption)
                {
                    eatTheRest = true;
                }
                else
                {
                    cmd.addArg(t);
                }
            }

            // the value is an option
            else if (t.startsWith("-"))
            {
                if (stopAtNonOption && !getOptions().hasOption(t))
                {
                    eatTheRest = true;
                    cmd.addArg(t);
                }
                else
                {
                    processOption(t, iterator);
                }
            }

            // the value is an argument
            else
            {
                cmd.addArg(t);

                if (stopAtNonOption)
                {
                    eatTheRest = true;
                }
            }

            // eat the remaining tokens
            if (eatTheRest)
            {
                while (iterator.hasNext())
                {
                    String str = (String) iterator.next();

                    // ensure only one double-dash is added
                    if (!"--".equals(str))
                    {
                        cmd.addArg(str);
                    }
                }
            }
        }

        processProperties(properties);
        checkRequiredOptions();

        return cmd;
    }

    /**
     * Sets the values of Options using the values in <code>properties</code>.
     *
     * @param properties The value properties to be processed.
     */
    protected void processProperties(Properties properties)
    {
        if (properties == null)
        {
            return;
        }

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();)
        {
            String option = e.nextElement().toString();

            if (!cmd.hasOption(option))
            {
                Option opt = getOptions().getOption(option);

                // get the value from the properties instance
                String value = properties.getProperty(option);

                if (opt.hasArg())
                {
                    if (opt.getValues() == null || opt.getValues().length == 0)
                    {
                        try
                        {
                            opt.addValueForProcessing(value);
                        }
                        catch (RuntimeException exp)
                        {
                            // if we cannot add the value don't worry about it
                        }
                    }
                }
                else if (!("yes".equalsIgnoreCase(value)
                        || "true".equalsIgnoreCase(value)
                        || "1".equalsIgnoreCase(value)))
                {
                    // if the value is not yes, true or 1 then don't add the
                    // option to the CommandLine
                    break;
                }

                cmd.addOption(opt);
            }
        }
    }

    /**
     * Throws a {@link MissingOptionException} if all of the required options
     * are not present.
     *
     * @throws MissingOptionException if any of the required Options
     * are not present.
     */
    protected void checkRequiredOptions() throws MissingOptionException
    {
        // if there are required options that have not been processsed
        if (!getRequiredOptions().isEmpty())
        {
            throw new MissingOptionException(getRequiredOptions());
        }
    }

    /**
     * <p>Process the argument values for the specified Option
     * <code>opt</code> using the values retrieved from the
     * specified iterator <code>iter</code>.
     *
     * @param opt The current Option
     * @param iter The iterator over the flattened command line
     * Options.
     *
     * @throws ParseException if an argument value is required
     * and it is has not been found.
     */
    public void processArgs(Option opt, ListIterator iter) throws ParseException
    {
        // loop until an option is found
        while (iter.hasNext())
        {
            String str = (String) iter.next();

            // found an Option, not an argument
            if (getOptions().hasOption(str) && str.startsWith("-"))
            {
                iter.previous();
                break;
            }

            // found a value
            try
            {
                opt.addValueForProcessing(Util.stripLeadingAndTrailingQuotes(str));
            }
            catch (RuntimeException exp)
            {
                iter.previous();
                break;
            }
        }

        if (opt.getValues() == null && !opt.hasOptionalArg())
        {
            throw new MissingArgumentException(opt);
        }
    }

    /**
     * Process the Option specified by <code>arg</code> using the values
     * retrieved from the specfied iterator <code>iter</code>.
     *
     * @param arg The String value representing an Option
     * @param iter The iterator over the flattened command line arguments.
     *
     * @throws ParseException if <code>arg</code> does not represent an Option
     */
    protected void processOption(String arg, ListIterator iter) throws ParseException
    {
        boolean hasOption = getOptions().hasOption(arg);

        // if there is no option throw an UnrecognisedOptionException
        if (!hasOption)
        {
            throw new UnrecognizedOptionException("Unrecognized option: " + arg, arg);
        }

        // get the option represented by arg
        Option opt = (Option) getOptions().getOption(arg).clone();

        // if the option is a required option remove the option from
        // the requiredOptions list
        if (opt.isRequired())
        {
            getRequiredOptions().remove(opt.getKey());
        }

        // if the option is in an OptionGroup make that option the selected
        // option of the group
        if (getOptions().getOptionGroup(opt) != null)
        {
            OptionGroup group = getOptions().getOptionGroup(opt);

            if (group.isRequired())
            {
                getRequiredOptions().remove(group);
            }

            group.setSelected(opt);
        }

        // if the option takes an argument value
        if (opt.hasArg())
        {
            processArgs(opt, iter);
        }

        // set the option on the command line
        cmd.addOption(opt);
    }
}
