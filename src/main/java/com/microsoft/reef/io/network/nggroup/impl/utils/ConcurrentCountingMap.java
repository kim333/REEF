/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.utils;

import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility map class that wraps a CountingMap
 * in a ConcurrentMap
 * Equivalent to Map<K,Map<V,Integer>>
 */
public class ConcurrentCountingMap<K, V> {

  private final ConcurrentMap<K, CountingMap<V>> map = new ConcurrentHashMap<>();

  public boolean remove (final K key, final V value) {
    if (!map.containsKey(key)) {
      return false;
    }
    final boolean retVal = map.get(key).remove(value);
    if (map.get(key).isEmpty()) {
      map.remove(key);
    }
    return retVal;
  }

  public void add (final K key, final V value) {
    map.putIfAbsent(key, new CountingMap<V>());
    map.get(key).add(value);
  }

  public CountingMap<V> get (final K key) {
    return map.get(key);
  }

  public boolean isEmpty () {
    return map.isEmpty();
  }

  public boolean containsKey (final K key) {
    return map.containsKey(key);
  }

  public boolean contains (final K key, final V value) {
    if (!map.containsKey(key)) {
      return false;
    }
    return map.get(key).containsKey(value);
  }

  public boolean notContains (final V value) {
    for (final CountingMap<V> innerMap : map.values()) {
      if (innerMap.containsKey(value)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString () {
    return map.toString();
  }

  public void clear () {
    for (final CountingMap<V> value : map.values()) {
      value.clear();
    }
    map.clear();
  }

  /**
   * @param args
   */
  public static void main (final String[] args) {
    // TODO Auto-generated method stub
    final ConcurrentCountingMap<Type, String> strMap = new ConcurrentCountingMap<>();
    System.out.println(strMap.isEmpty());
    strMap.add(Type.ChildAdd, "ST0");
    System.out.println(strMap);
    strMap.add(Type.ChildAdd, "ST1");
    System.out.println(strMap);
    strMap.add(Type.ChildDead, "ST0");
    System.out.println(strMap);
    strMap.add(Type.ChildDead, "ST1");
    System.out.println(strMap);
    strMap.add(Type.ChildAdd, "ST2");
    System.out.println(strMap);
    strMap.add(Type.ChildAdd, "ST3");
    System.out.println(strMap);
    strMap.add(Type.ChildAdd, "ST0");
    System.out.println(strMap);
    strMap.add(Type.ChildAdd, "ST1");
    System.out.println(strMap);
    strMap.remove(Type.ChildAdd, "ST2");
    System.out.println(strMap);
    strMap.remove(Type.ChildAdd, "ST3");
    System.out.println(strMap);
    strMap.remove(Type.ChildAdd, "ST0");
    System.out.println(strMap);
    System.out.println(strMap.get(Type.ChildAdd));
    System.out.println(strMap.get(Type.ChildDead));
    strMap.remove(Type.ChildAdd, "ST1");
    System.out.println(strMap);
    strMap.remove(Type.ChildAdd, "ST1");
    System.out.println(strMap);
    System.out.println(strMap.containsKey(Type.ChildAdd));
    System.out.println(strMap.containsKey(Type.ChildDead));
    System.out.println(strMap.contains(Type.ChildAdd, "ST0"));
    System.out.println(strMap.contains(Type.ChildAdd, "ST2"));
    strMap.remove(Type.ChildAdd, "ST0");
    System.out.println(strMap);
    System.out.println(strMap.containsKey(Type.ChildAdd));
    System.out.println(strMap.isEmpty());
  }

}
