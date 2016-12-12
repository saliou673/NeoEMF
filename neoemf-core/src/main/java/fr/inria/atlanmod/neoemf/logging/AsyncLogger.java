/*
 * Copyright (c) 2013-2016 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.logging;

import com.google.common.util.concurrent.MoreExecutors;

import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AsyncLogger extends AbstractLogger {

    /**
     * The number of threads in the pool. Use a single thread to ensure the log order.
     */
    private static final int THREADS = 1;

    private static final int TERMINATION_TIMEOUT_MS = 100;

    private static final ExecutorService pool =
            MoreExecutors.getExitingExecutorService(
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADS),
                    TERMINATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

    public AsyncLogger(String name) {
        super(name);
    }

    @Override
    public void log(Level level, Throwable e, CharSequence message, Object... params) {
        execute(() -> {
            try {
                logger().log(level.level(), () -> MessageFormat.format(message.toString(), params), e);
            }
            catch (Exception ignore) {
            }
        });
    }

    private void execute(Runnable runnable) {
        try {
            // Asynchronous call
            pool.submit(runnable);
        }
        catch (RejectedExecutionException e) {
            // Synchronous call
            runnable.run();
        }
    }
}
