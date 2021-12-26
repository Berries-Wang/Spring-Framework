/*
 * Copyright 2006-2007 the original author or authors.
 *
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
 */

package org.springframework.retry.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.retry.context.RetryContextSupport;

public class SoftReferenceMapRetryContextCacheTests {

	SoftReferenceMapRetryContextCache cache = new SoftReferenceMapRetryContextCache();
	
	@Test
	public void testPut() {
		RetryContextSupport context = new RetryContextSupport(null);
		cache.put("foo", context);
		assertEquals(context, cache.get("foo"));
	}

	@Test(expected=RetryCacheCapacityExceededException.class)
	public void testPutOverLimit() {
		RetryContextSupport context = new RetryContextSupport(null);
		cache.setCapacity(1);
		cache.put("foo", context);
		cache.put("foo", context);
	}

	@Test
	public void testRemove() {
		assertFalse(cache.containsKey("foo"));
		RetryContextSupport context = new RetryContextSupport(null);
		cache.put("foo", context);
		assertTrue(cache.containsKey("foo"));
		cache.remove("foo");
		assertFalse(cache.containsKey("foo"));
	}

}
