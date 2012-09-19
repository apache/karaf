/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.exam.options;

import static java.lang.String.format;

import org.apache.karaf.tooling.exam.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.extra.VMOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Final class to provide an easy and intuitive way to configure the specific Karaf distribution options.
 */
public final class KarafDistributionOption {

    /**
     * By default, the folder pax-exam is deleting the test directories after a test is over. If you want to keep those
     * directories (for later evaluation) simply set this option.
     *
     * @return the exam option.
     */
    public static Option keepRuntimeFolder() {
        return new KeepRuntimeFolderOption();
    }

    /**
     * Provides an option to configure the internals of the PaxExamKaraf subsystem runner.
     *
     * @param invoker the invoker to use.
     * @return the exam option.
     */
    public static Option useOwnKarafExamSystemConfiguration(String invoker) {
        return new KarafExamSystemConfigurationOption(invoker);
    }

    /**
     * The Karaf pax-logging configuration is typically not a file manipulated very often. Therefore we take a freedom
     * of adding a console logger and changing the log level directly. If you want to configure the file manually (or
     * add your own file in your distribution), use this option.
     *
     * @return the exam option.
     */
    public static Option doNotModifyLogConfiguration() {
        return new DoNotModifyLogOption();
    }

    /**
     * This option allows to configure the start level of the bundles in the exam features descriptor.
     *
     * @param startLevel set the start level.
     * @return the exam option.
     */
    public static Option useOwnExamBundlesStartLevel(int startLevel) {
        return new ExamBundlesStartLevel(startLevel);
    }

    /**
     * Return an option object which can be used to configure the <code>-Dkaraf.startLocalConsole</code> and
     * <code>-Dkaraf.startRemoteShell</code> options. By default, both are true.
     *
     * @return the exam option.
     */
    public static KarafDistributionConfigurationConsoleOption configureConsole() {
        return new KarafDistributionConfigurationConsoleOption(null, null);
    }

    /**
     * Configure the distribution options to use. Relevant are the frameworkUrl, the frameworkName and the Karaf
     * version, since all of those parameters are relevant to decide which wrapper configurations to use.
     *
     * @return the exam option.
     */
    public static KarafDistributionBaseConfigurationOption karafDistributionConfiguration(String frameworkUrl, String name, String karafVersion) {
        return new KarafDistributionConfigurationOption(frameworkUrl, name, karafVersion);
    }

    /**
     * Configure the distribution options to use. Relevant are the frameworkUrl, the frameworkName and the Karaf
     * version, since all of those parameters are relevant to decide which wrapper configurations to use.
     *
     * @return the exam option.
     */
    public static KarafDistributionBaseConfigurationOption karafDistributionConfiguration() {
        return new KarafDistributionConfigurationOption();
    }

    /**
     * This option allows to configure each configuration file based on the <code>karaf.home</code> location.
     * The value is "put", which means it is either replaced or added.
     *
     * If you want to extend a value (e.g. make a=b to a=b,c), please use the {@link KarafDistributionConfigurationFileExtendOption}
     *
     * @param configurationFilePath the configuration file path.
     * @param key the key in the configuration file.
     * @param value the value in the configuration file.
     * @return the exam option.
     */
    public static Option editConfigurationFilePut(String configurationFilePath, String key, String value) {
        return new KarafDistributionConfigurationFilePutOption(configurationFilePath, key, value);
    }

    /**
     * This option allows to configure each configuration file based on the <code>karaf.home</code> location.
     * The value is "put", which means it is either replaced or added.
     *
     * If you want to extend a value (e.g. make a=b to a=b,c), please use the {@link KarafDistributionConfigurationFileExtendOption}
     *
     * @param pointer the configuration pointer.
     * @param value the value in the configuration file.
     * @return the exam option.
     */
    public static Option editConfigurationFilePut(ConfigurationPointer pointer, String value) {
        return new KarafDistributionConfigurationFilePutOption(pointer, value);
    }

    /**
     * This option allows to configure each configuration file based on <code>karaf.home</code> location. The value is "put"
     * which means it is either replaced or added. For simple configuration, you can add a file source. If you want to
     * put all values from this file, do not configure any keysToUseFromSource; otherwise define them to use only those
     * specific values.
     *
     * @param configurationFilePath the configuration file path.
     * @param source the source file.
     * @param keysToUseFromSource the keys to use in the source file.
     * @return the exam option.
     */
    public static Option[] editConfigurationFilePut(final String configurationFilePath, File source, String... keysToUseFromSource) {
        return createOptionListFromFile(source, new FileOptionFactory() {
            @Override
            public Option createOption(String key, String value) {
                return new KarafDistributionConfigurationFilePutOption(configurationFilePath, key, value);
            }
        }, keysToUseFromSource);
    }

    private static interface FileOptionFactory {
        Option createOption(String key, String value);
    }

    private static Option[] createOptionListFromFile(File source, FileOptionFactory optionFactory, String... keysToUseFromSource) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(source));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        List<Option> options = new ArrayList<Option>();
        if (keysToUseFromSource == null || keysToUseFromSource.length == 0) {
            Set<Object> keySet = props.keySet();
            for (Object key : keySet) {
                Object value = props.get(key);
                options.add(optionFactory.createOption((String) key, (String) value));
            }
        } else {
            for (String key : keysToUseFromSource) {
                Object value = props.get(key);
                options.add(optionFactory.createOption(key, (String) value));
            }
        }
        return options.toArray(new Option[]{ });
    }

    /**
     * This option allows to extend configurations in each configuration file based on <code>karaf.home</code> location.
     * The value extends the current value (e.g. a=b to a=a,b) instead of replacing it. If there is no current value
     * it is added.
     *
     * If you want to add or replace functionality, please use the {@link KarafDistributionConfigurationFilePutOption}
     * option.
     *
     * @param configurationFilePath the configuration file path.
     * @param key the key in the configuration file.
     * @param value the value in the configuration file.
     * @return the exam option.
     */
    public static Option editConfigurationFileExtend(String configurationFilePath, String key, String value) {
        return new KarafDistributionConfigurationFileExtendOption(configurationFilePath, key, value);
    }

    /**
     * This option allows to extend configurations in each configuration file based on <code>karaf.home</code> location.
     * The value extends the current value (e.g. a=b to a=a,b) instead of replacing it. If there is no current value
     * it is added.
     *
     * If you want to add or replace functionality, please use the {@link KarafDistributionConfigurationFilePutOption}
     * option.
     *
     * @param pointer the configuration pointer to use.
     * @param value the value in the configuration file.
     * @return the exam option.
     */
    public static Option editConfigurationFileExtend(ConfigurationPointer pointer, String value) {
        return new KarafDistributionConfigurationFileExtendOption(pointer, value);
    }

    /**
     * This option allows to configure each configuration file based on the <code>karaf.home</code> location. The value
     * is "extend", which means it is either replaced or added. For simpler configuration you can add a source file.
     *
     * @param configurationFilePath the configuration file path.
     * @param source the file source.
     * @param keysToUseFromSource the keys to use from source.
     * @return the exam options.
     */
    public static Option[] editConfigurationFileExtend(final String configurationFilePath, File source, String... keysToUseFromSource) {
        return createOptionListFromFile(source, new FileOptionFactory() {
            @Override
            public Option createOption(String key, String value) {
                return new KarafDistributionConfigurationFileExtendOption(configurationFilePath, key, value);
            }
        }, keysToUseFromSource);
    }

    /**
     * This option allows to replace a whole configuration file with your own one. Simply point to the configuration
     * file you want to replace and define the source file.
     *
     * @param configurationFilePath the configuration file path.
     * @param source the source file.
     * @return the exam option.
     */
    public static Option replaceConfigurationFile(String configurationFilePath, File source) {
        return new KarafDistributionConfigurationFileReplacementOption(configurationFilePath, source);
    }

    /**
     * Activate debugging on the embedded Karaf container using the standard 5005 and hold the JVM till you've
     * attached the debugger.
     *
     * @return the exam option.
     */
    public static Option debugConfiguration() {
        return debugConfiguration("5005", true);
    }

    /**
     * A very simple and convenient method to set a specific log level without the need of configure the specific
     * option itself.
     *
     * @param logLevel the new log level.
     * @return the exam option.
     */
    public static Option logLevel(LogLevel logLevel) {
        return new LogLevelOption(logLevel);
    }


    /**
     * A very simple and convinient method to set a specific log level without the need of configure the specific option
     * itself.
     */
    public static LogLevelOption logLevel() {
        return new LogLevelOption();
    }

    /**
     * Returns an easy option to activate and configure remote debugging for the Karaf container.
     */
    public static Option debugConfiguration(String port, boolean hold) {
        return new VMOption(format("-Xrunjdwp:transport=dt_socket,server=y,suspend=%s,address=%s", hold ? "y" : "n",
                port));
    }

}
