package wangdaye.com.geometricweather.common.utils.helpers

import kotlinx.coroutines.*
import wangdaye.com.geometricweather.GeometricWeather
import java.util.concurrent.Executor

class AsyncHelper {

    companion object {

        @JvmStatic
        fun <T> runOnIO(task: Task<T>, callback: Callback<T>) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(Dispatchers.IO) {
                    task.execute(Emitter(this, callback))
                }
        )

        @JvmStatic
        fun runOnIO(runnable: Runnable) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(Dispatchers.IO) {
                    runnable.run()
                }
        )

        @JvmStatic
        fun <T> runOnExecutor(task: Task<T>, callback: Callback<T>, executor: Executor) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(executor.asCoroutineDispatcher()) {
                    task.execute(Emitter(this, callback))
                }
        )

        @JvmStatic
        fun runOnExecutor(runnable: Runnable, executor: Executor) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(executor.asCoroutineDispatcher()) {
                    runnable.run()
                }
        )

        @JvmStatic
        fun <T> delayRunOnIO(runnable: Runnable, milliSeconds: Long) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(Dispatchers.IO) {
                    delay(milliSeconds)
                    runnable.run()
                }
        )

        @JvmStatic
        fun delayRunOnUI(runnable: Runnable, milliSeconds: Long) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(Dispatchers.Main) {
                    delay(milliSeconds)
                    runnable.run()
                }
        )

        @JvmStatic
        fun intervalRunOnUI(runnable: Runnable,
                            intervalMilliSeconds: Long, initDelayMilliSeconds: Long) = Controller(
                GeometricWeather.instance!!.applicationScope.launch(Dispatchers.Main) {

                    delay(initDelayMilliSeconds)

                    while (true) {
                        isActive
                        runnable.run()
                        delay(intervalMilliSeconds)
                    }
                }
        )
    }

    class Controller internal constructor(private val job: Job) {

        fun cancel() {
            job.cancel()
        }
    }

    class Data<T> internal constructor(val t: T?, val done: Boolean)

    class Emitter<T> internal constructor(private val scope: CoroutineScope,
                                          private val inner: Callback<T>) {

        fun send(t: T?, done: Boolean) = scope.run {
            inner.call(t, done)
        }
    }

    interface Task<T> {
        fun execute(emitter: Emitter<T>)
    }

    interface Callback<T> {
        fun call(t: T?, done: Boolean)
    }
}