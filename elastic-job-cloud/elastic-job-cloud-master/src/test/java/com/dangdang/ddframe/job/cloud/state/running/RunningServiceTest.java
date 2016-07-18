/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.cloud.state.running;

import com.dangdang.ddframe.job.cloud.context.TaskContext;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class RunningServiceTest {
    
    @Mock
    private CoordinatorRegistryCenter regCenter;
    
    @InjectMocks
    private RunningService runningService;
    
    @Test
    public void assertAddWithRootNode() {
        when(regCenter.isExisted("/state/running/test_job/test_job@-@0@-@READY@-@00")).thenReturn(true);
        runningService.add(TaskContext.from("test_job@-@0@-@READY@-@00"));
        verify(regCenter).isExisted("/state/running/test_job/test_job@-@0@-@READY@-@00");
        verify(regCenter, times(0)).persist("/state/running/test_job/test_job@-@0@-@READY@-@00", "slave-S00");
    }
    
    @Test
    public void assertAddWithoutRootNode() {
        when(regCenter.isExisted("/state/running/test_job/test_job@-@0@-@READY@-@00")).thenReturn(false);
        runningService.add(TaskContext.from("test_job@-@0@-@READY@-@00"));
        verify(regCenter).isExisted("/state/running/test_job/test_job@-@0@-@READY@-@00");
        verify(regCenter).persist("/state/running/test_job/test_job@-@0@-@READY@-@00", "");
    }
    
    @Test
    public void assertRemoveWithoutRootNode() {
        when(regCenter.isExisted("/state/running/test_job")).thenReturn(false);
        runningService.remove(TaskContext.from("test_job@-@0@-@READY@-@00"));
        verify(regCenter, times(0)).remove("/state/running/test_job/test_job@-@0@-@READY@-@00");
    }
    
    @Test
    public void assertRemoveWithRootNode() {
        when(regCenter.isExisted("/state/running/test_job")).thenReturn(true);
        when(regCenter.getChildrenKeys("/state/running/test_job")).thenReturn(Arrays.asList("test_job@-@0@-@READY@-@00", "test_job@-@0@-@READY@-@11", "test_job@-@1@-@READY@-@00"));
        runningService.remove(TaskContext.from("test_job@-@0@-@READY@-@00"));
        verify(regCenter).remove("/state/running/test_job/test_job@-@0@-@READY@-@00");
        verify(regCenter).remove("/state/running/test_job/test_job@-@0@-@READY@-@11");
        verify(regCenter, times(0)).remove("/state/running/test_job/test_job@-@1@-@READY@-@00");
    }
    
    @Test
    public void assertIsJobRunning() {
        when(regCenter.getChildrenKeys("/state/running/running_job")).thenReturn(Collections.singletonList("running_job@-@0@-@READY@-@00"));
        assertTrue(runningService.isJobRunning("running_job"));
        assertFalse(runningService.isJobRunning("pending_job"));
        verify(regCenter).getChildrenKeys("/state/running/running_job");
        verify(regCenter).getChildrenKeys("/state/running/pending_job");
    }
    
    @Test
    public void assertIsTaskRunningWithoutRootNode() {
        when(regCenter.isExisted("/state/running/test_job")).thenReturn(false);
        assertFalse(runningService.isTaskRunning(TaskContext.from("test_job@-@1@-@READY@-@00")));
    }
    
    @Test
    public void assertIsTaskRunningWitRootNode() {
        when(regCenter.isExisted("/state/running/test_job")).thenReturn(true);
        when(regCenter.getChildrenKeys("/state/running/test_job")).thenReturn(Collections.singletonList("test_job@-@0@-@READY@-@00"));
        assertTrue(runningService.isTaskRunning(TaskContext.from("test_job@-@0@-@READY@-@11")));
        assertFalse(runningService.isTaskRunning(TaskContext.from("test_job@-@1@-@READY@-@00")));
    }
    @Test
    public void assertClear() {
        runningService.clear();
        verify(regCenter).remove("/state/running");
    }
}