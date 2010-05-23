package org.apache.felix.dm.annotation.plugin.bnd;

/**
 * The type of parameters which can be found in a component descriptor.
 */
public enum EntryParam
{
    init, 
    start, 
    stop, 
    destroy, 
    impl, 
    provide, 
    properties, 
    composition, 
    service, 
    filter, 
    defaultImpl, 
    required, 
    added, 
    changed,
    removed,
    autoConfig, 
    pid, 
    factoryPid,
    propagate, 
    updated, 
    timeout,
    adapterService,
    adapterProperties,
    adapteeService,
    adapteeFilter,
    stateMask,
    ranking,
    factory,
    factoryConfigure,
    name
}
