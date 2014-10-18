/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.faulty;

public class NetworkFault extends RuntimeException {
  private static final long serialVersionUID = 3085665402551118349L;
  
  private static final String MSG = "Induced network fault";
  
  public String getMessage() {
    return MSG;
  };

}
