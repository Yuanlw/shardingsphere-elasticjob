/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.internal.schedule;

import com.dangdang.ddframe.job.api.JobFacade;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.job.dataflow.DataflowType;
import com.dangdang.ddframe.job.lite.api.config.JobConfiguration;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.internal.config.ConfigurationService;
import com.dangdang.ddframe.job.lite.internal.execution.ExecutionContextService;
import com.dangdang.ddframe.job.lite.internal.execution.ExecutionService;
import com.dangdang.ddframe.job.lite.internal.failover.FailoverService;
import com.dangdang.ddframe.job.lite.internal.offset.OffsetService;
import com.dangdang.ddframe.job.lite.internal.server.ServerService;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;

import java.util.Collection;
import java.util.List;

/**
 * 为调度器提供内部服务的门面类.
 * 
 * @author zhangliang
 */
public class LiteJobFacade implements JobFacade {
    
    private final ConfigurationService configService;
    
    private final ShardingService shardingService;
    
    private final ServerService serverService;
    
    private final ExecutionContextService executionContextService;
    
    private final ExecutionService executionService;
    
    private final FailoverService failoverService;
    
    private final OffsetService offsetService;
    
    private final List<ElasticJobListener> elasticJobListeners;
    
    public LiteJobFacade(final CoordinatorRegistryCenter coordinatorRegistryCenter, final JobConfiguration jobConfiguration, final List<ElasticJobListener> elasticJobListeners) {
        configService = new ConfigurationService(coordinatorRegistryCenter, jobConfiguration);
        shardingService = new ShardingService(coordinatorRegistryCenter, jobConfiguration);
        serverService = new ServerService(coordinatorRegistryCenter, jobConfiguration);
        executionContextService = new ExecutionContextService(coordinatorRegistryCenter, jobConfiguration);
        executionService = new ExecutionService(coordinatorRegistryCenter, jobConfiguration);
        failoverService = new FailoverService(coordinatorRegistryCenter, jobConfiguration);
        offsetService = new OffsetService(coordinatorRegistryCenter, jobConfiguration);
        this.elasticJobListeners = elasticJobListeners;
    }
    
    @Override
    public String getJobName() {
        return configService.getJobName();
    }
    
    @Override
    public DataflowType getDataflowType() {
        return configService.getDataflowType();
    }
    
    @Override
    public int getConcurrentDataProcessThreadCount() {
        return configService.getConcurrentDataProcessThreadCount();
    }
    
    @Override
    public boolean isStreamingProcess() {
        return configService.isStreamingProcess();
    }
    
    @Override
    public String getScriptCommandLine() {
        return configService.getScriptCommandLine();
    }
    
    @Override
    public void checkMaxTimeDiffSecondsTolerable() {
        configService.checkMaxTimeDiffSecondsTolerable();
    }
    
    @Override
    public void failoverIfNecessary() {
        if (configService.isFailover() && !serverService.isJobPausedManually()) {
            failoverService.failoverIfNecessary();
        }
    }
    
    @Override
    public void registerJobBegin(final ShardingContext shardingContext) {
        executionService.registerJobBegin(shardingContext);
    }
    
    @Override
    public void registerJobCompleted(final ShardingContext shardingContext) {
        executionService.registerJobCompleted(shardingContext);
        if (configService.isFailover()) {
            failoverService.updateFailoverComplete(shardingContext.getShardingItems().keySet());
        }
    }
    
    @Override
    public ShardingContext getShardingContext() {
        boolean isFailover = configService.isFailover();
        if (isFailover) {
            List<Integer> failoverShardingItems = failoverService.getLocalHostFailoverItems();
            if (!failoverShardingItems.isEmpty()) {
                return executionContextService.getJobShardingContext(failoverShardingItems);
            }
        }
        shardingService.shardingIfNecessary();
        List<Integer> shardingItems = shardingService.getLocalHostShardingItems();
        if (isFailover) {
            shardingItems.removeAll(failoverService.getLocalHostTakeOffItems());
        }
        return executionContextService.getJobShardingContext(shardingItems);
    }
    
    @Override
    public boolean misfireIfNecessary(final Collection<Integer> shardingItems) {
        return executionService.misfireIfNecessary(shardingItems);
    }
    
    @Override
    public void clearMisfire(final Collection<Integer> shardingItems) {
        executionService.clearMisfire(shardingItems);
    }
    
    @Override
    public boolean isExecuteMisfired(final Collection<Integer> shardingItems) {
        return isEligibleForJobRunning() && configService.isMisfire() && !executionService.getMisfiredJobItems(shardingItems).isEmpty();
    }
    
    @Override
    public boolean isEligibleForJobRunning() {
        return !serverService.isJobPausedManually() && !shardingService.isNeedSharding() && configService.isStreamingProcess();
    }
    
    @Override
    public boolean isNeedSharding() {
        return shardingService.isNeedSharding();
    }
    
    @Override
    public void updateOffset(final int item, final String offset) {
        offsetService.updateOffset(item, offset);
    }
    
    @Override
    public void cleanPreviousExecutionInfo() {
        executionService.cleanPreviousExecutionInfo();
    }
    
    @Override
    public void beforeJobExecuted(final ShardingContext shardingContext) {
        for (ElasticJobListener each : elasticJobListeners) {
            each.beforeJobExecuted(shardingContext);
        }
    }
    
    @Override
    public void afterJobExecuted(final ShardingContext shardingContext) {
        for (ElasticJobListener each : elasticJobListeners) {
            each.afterJobExecuted(shardingContext);
        }
    }
}
