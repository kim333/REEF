/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.faulty;

import java.util.Set;

import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.util.StringIdentifierFactory;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.wake.IdentifierFactory;
import com.microsoft.wake.remote.Codec;

public class BroadReduceConfig {
  public static final String defaultValue = "NULL";

  @NamedParameter(default_class = StringIdentifierFactory.class)
  public static class IdFactory implements Name<IdentifierFactory> {
  }

  public static class BroadcastConfig {
    @NamedParameter()
    public static class DataCodec implements Name<Codec<?>> {
    }

    public static class Sender {

      @NamedParameter(doc = "Task ID of the operator")
      public static class SelfId implements Name<String> {
      }

      @NamedParameter(doc = "Task ID of the parent of the operator", default_value = defaultValue)
      public static class ParentId implements Name<String> {
      }

      @NamedParameter(doc = "List of child Identifiers that the operator sends to", default_value = defaultValue)
      public static class ChildIds implements Name<Set<String>> {
      }
    }

    public static class Receiver {

      @NamedParameter(doc = "Task ID of the operator")
      public static class SelfId implements Name<String> {
      }

      @NamedParameter(doc = "Task ID of the parent of the operator", default_value = defaultValue)
      public static class ParentId implements Name<String> {
      }

      @NamedParameter(doc = "List of child Identifiers that the operator sends to", default_value = defaultValue)
      public static class ChildIds implements Name<Set<String>> {
      }
    }
  }

  public static class ReduceConfig {
    @NamedParameter()
    public static class DataCodec implements Name<Codec<?>> {
    }

    @NamedParameter()
    public static class ReduceFunction implements
        Name<Reduce.ReduceFunction<?>> {
    }

    public static class Sender {

      @NamedParameter(doc = "Task ID of the operator")
      public static class SelfId implements Name<String> {
      }

      @NamedParameter(doc = "Task ID of the parent of the operator", default_value = defaultValue)
      public static class ParentId implements Name<String> {
      }

      @NamedParameter(doc = "List of child Identifiers that the operator sends to", default_value = defaultValue)
      public static class ChildIds implements Name<Set<String>> {
      }

      @NamedParameter(doc="Whether gradient from failed task needs to be approximated", default_value="true")
      public static class ApproximateGradient implements Name<Boolean> {

      }
    }

    public static class Receiver {

      @NamedParameter(doc = "Task ID of the operator")
      public static class SelfId implements Name<String> {
      }

      @NamedParameter(doc = "Task ID of the parent of the operator", default_value = defaultValue)
      public static class ParentId implements Name<String> {
      }

      @NamedParameter(doc = "List of child Identifiers that the operator sends to", default_value = defaultValue)
      public static class ChildIds implements Name<Set<String>> {
      }
    }
  }
}