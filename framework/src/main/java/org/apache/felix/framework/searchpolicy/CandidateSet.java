package org.apache.felix.framework.searchpolicy;

import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;

class CandidateSet
{
    public static final int NORMAL = 0;
    public static final int FRAGMENT = 1;
    public static final int HOST = 2;
    public final int m_type;
    public final IModule m_module;
    public final IRequirement m_requirement;
    public final PackageSource[] m_candidates;
    public final IModule[] m_modules;
    public int m_idx = 0;
    public int m_rotated = 0;

    public CandidateSet(int type, IModule module, IRequirement requirement, PackageSource[] candidates)
    {
        super();
        m_type = type;
        m_module = module;
        m_requirement = requirement;
        m_candidates = candidates;
        m_modules = null;
    }

    public CandidateSet(int type, IModule module, IRequirement requirement, IModule[] fragments)
    {
        super();
        m_type = type;
        m_module = module;
        m_requirement = requirement;
        m_candidates = null;
        m_modules = fragments;
    }
}