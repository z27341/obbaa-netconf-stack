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

package org.broadband_forum.obbaa.netconf.server.ssh;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by sgs on 3/17/16.
 */
public class NamedThreadFactoryTest {

    public static final String TEST_POOL_NAME = "test-pool-name";

    @Test
    public void testThreadName() {
        NamedThreadFactory factory = new NamedThreadFactory(TEST_POOL_NAME);
        Thread t1 = factory.newThread(new Runnable() {
            @Override
            public void run() {

            }
        });
        assertTrue(t1.getName().contains(TEST_POOL_NAME));
    }
}
