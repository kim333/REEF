/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import com.microsoft.reef.examples.nggroup.bgd.data.Example;
import com.microsoft.reef.examples.nggroup.bgd.data.parser.Parser;
import com.microsoft.reef.io.data.loading.api.DataSet;
import com.microsoft.reef.io.network.util.Pair;

/**
 *
 */
public class ExampleList {

  private static final Logger LOG = Logger.getLogger(ExampleList.class.getName());

  private final List<Example> examples = new ArrayList<>();
  private final DataSet<LongWritable, Text> dataSet;
  private final Parser<String> parser;

  @Inject
  public ExampleList(final DataSet<LongWritable, Text> dataSet, final Parser<String> parser) {
    this.dataSet = dataSet;
    this.parser = parser;
  }

  /**
   * @return the examples
   */
  public List<Example> getExamples() {
    if(examples.isEmpty()) {
      loadData();
    }
    return examples;
  }

  private void loadData() {
    LOG.info("Loading data");
    int i = 0;
    for (final Pair<LongWritable, Text> examplePair : dataSet) {
      final Example example = parser.parse(examplePair.second.toString());
      examples.add(example);
      if (++i % 2000 == 0) {
        LOG.log(Level.FINE, "Done parsing {0} lines", i);
      }
    }
  }
}
