/*
 * Copyright 2018 Broadband Forum
 *
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
 */

package org.broadband_forum.obbaa.netconf.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestScopeTest {

    @BeforeClass
    public static void setUpClass() {
        RequestScope.setEnableThreadLocalInUT(true);
    }

    @AfterClass
    public static void settearDownClass() {
        RequestScope.setEnableThreadLocalInUT(false);
    }

    @Test
    public void testGetCurrentScope() {
        assertNotNull(RequestScope.getCurrentScope());
    }

    @Test
    public void testTwoDifferentScopes() throws InterruptedException {
        final AtomicReference<RequestScope> t1Scope = new AtomicReference<RequestScope>(null);
        final AtomicReference<RequestScope> t2Scope = new AtomicReference<RequestScope>(null);
        final CountDownLatch startTest = new CountDownLatch(2);
        final CountDownLatch completeThreads = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                t1Scope.set(RequestScope.getCurrentScope());
                startTest.countDown();
                try {
                    completeThreads.await();
                } catch (InterruptedException e) {
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                t2Scope.set(RequestScope.getCurrentScope());
                startTest.countDown();
                try {
                    completeThreads.await();
                } catch (InterruptedException e) {
                }
            }
        }).start();
        startTest.await();
        assertNotSame(t1Scope.get(), t2Scope.get());
        completeThreads.countDown();
    }

}
