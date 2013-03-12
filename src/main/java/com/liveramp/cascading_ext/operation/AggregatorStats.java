/**
 *  Copyright 2012 LiveRamp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.liveramp.cascading_ext.operation;

import cascading.flow.FlowProcess;
import cascading.operation.Aggregator;
import cascading.operation.AggregatorCall;
import cascading.tuple.TupleEntry;
import com.liveramp.cascading_ext.operation.forwarding.ForwardingAggregator;
import com.liveramp.cascading_ext.util.OperationStatsUtils;

/**
 * An AggregatorStats instance decorates an Aggregator instance and
 * automatically maintains input/output records counters in addition to
 * providing the functionality of the wrapped object.
 */
public class AggregatorStats extends ForwardingAggregator {

  private final ForwardingAggregatorCall wrapper = new ForwardingAggregatorCall();

  public static final String INPUT_RECORDS_COUNTER_NAME = "Input records";
  public static final String TOTAL_OUTPUT_RECORDS_COUNTER_NAME = "Total output records";

  private final String prefixInputRecords;
  private final String prefixTotalOutputRecords;

  public AggregatorStats(Aggregator aggregator) {
    this(OperationStatsUtils.getStackPosition(1) + " - " + aggregator.getClass().getSimpleName(), aggregator);
  }

  @SuppressWarnings("unchecked")
  public AggregatorStats(String name, Aggregator aggregator) {
    super(aggregator);
    this.prefixInputRecords = name + " - " + INPUT_RECORDS_COUNTER_NAME;
    this.prefixTotalOutputRecords = name + " - " + TOTAL_OUTPUT_RECORDS_COUNTER_NAME;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void start(FlowProcess process, AggregatorCall call) {
    super.start(process, call);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void aggregate(FlowProcess process, AggregatorCall call) {
    super.aggregate(process, wrapper);
    process.increment(OperationStatsUtils.COUNTER_CATEGORY, prefixInputRecords, 1);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void complete(FlowProcess process, AggregatorCall call) {
    wrapper.setDelegate(call);
    super.complete(process, wrapper);
    int output = wrapper.getOutputCollector().getCount();
    process.increment(OperationStatsUtils.COUNTER_CATEGORY, prefixTotalOutputRecords, output);
  }

  private static class ForwardingAggregatorCall<Context>
      extends OperationStatsUtils.ForwardingOperationCall<Context, AggregatorCall<Context>>
      implements AggregatorCall<Context> {

    @Override
    public TupleEntry getGroup() {
      return delegate.getGroup();
    }

    @Override
    public TupleEntry getArguments() {
      return delegate.getArguments();
    }

    @Override
    public void setDelegate(AggregatorCall<Context> delegate) {
      super.setDelegate(delegate);
      collector.setOutputCollector(delegate.getOutputCollector());
    }
  }
}
