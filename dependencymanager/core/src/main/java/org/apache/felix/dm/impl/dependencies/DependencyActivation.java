package org.apache.felix.dm.impl.dependencies;

public interface DependencyActivation
{
  public void start(DependencyService service);
  public void stop(DependencyService service);
}
