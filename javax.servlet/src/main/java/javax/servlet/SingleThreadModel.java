/*
 * Copyright 1999,2005 The Apache Software Foundation.
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

package javax.servlet;

/**
 * Ensures that servlets handle
 * only one request at a time. This interface has no methods.
 *
 * <p>If a servlet implements this interface, you are <i>guaranteed</i>
 * that no two threads will execute concurrently in the
 * servlet's <code>service</code> method. The servlet container
 * can make this guarantee by synchronizing access to a single
 * instance of the servlet, or by maintaining a pool of servlet
 * instances and dispatching each new request to a free servlet.
 *
 * <p>This interface does not prevent
 * synchronization problems that result from servlets accessing shared
 * resources such as static class variables or classes outside
 * the scope of the servlet.
 *
 *
 * @author	Various
 * @version	$Version$
 *
 */

public interface SingleThreadModel {
}
