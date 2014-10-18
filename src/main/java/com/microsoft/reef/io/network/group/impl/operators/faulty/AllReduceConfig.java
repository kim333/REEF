/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.faulty;

import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.util.StringIdentifierFactory;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.wake.IdentifierFactory;
import com.microsoft.wake.remote.Codec;

import java.util.Set;

public class AllReduceConfig {
  public static final String defaultValue = "NULL";
  
  @NamedParameter(default_class=StringIdentifierFactory.class)
  public static class IdFactory implements Name<IdentifierFactory> { }
  
  @NamedParameter()
  public static class DataCodec implements Name<Codec<?>> { }
  
  @NamedParameter()
  public static class ReduceFunction implements Name<Reduce.ReduceFunction<?>> { }

  @NamedParameter(doc = "Task ID of the operator")
  public static class SelfId implements Name<String> {  }

  @NamedParameter(doc = "Task ID of the parent of the operator", default_value = defaultValue)
  public static class ParentId implements Name<String> {  }
  
  @NamedParameter(doc = "List of child Identifiers that the operator sends to", default_value=defaultValue)
  public static class ChildIds implements Name<Set<String>> {  }

}
