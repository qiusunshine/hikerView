package com.example.hikerview.utils

import android.os.Looper
import com.annimon.stream.function.Consumer
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 作者：By 15968
 * 日期：On 2021/10/24
 * 时间：At 20:29
 */
object ThreadTool {

    val scopeMap: MutableMap<Any, CoroutineScope> by lazy {
        HashMap()
    }

    fun executeNewTask(command: Runnable?): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            command?.run()
        }
    }

    fun newScope(): CoroutineScope {
        return CoroutineScope(EmptyCoroutineContext)
    }

    fun cancelScope(scope: CoroutineScope) {
        try {
            scope.cancel()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun launch(scope: CoroutineScope, task: Runnable) {
        try {
            scope.launch(Dispatchers.IO) {
                task.run()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun postDelayed(delayMillSec: Long, task: Runnable) {
        try {
            GlobalScope.launch(Dispatchers.IO) {
                delay(delayMillSec)
                task.run()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun runOnUI(task: Runnable) {
        GlobalScope.launch(Dispatchers.Main) {
            task.run()
        }
    }

    fun runOnUI(scope: CoroutineScope, task: Runnable) {
        try {
            scope.launch(Dispatchers.Main) {
                task.run()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun loadScheduleTask(holder: Any, scheduleGap: Long, runnable: Runnable): CoroutineScope {
        val scope = newScope()
        scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    runnable.run()
                    delay(scheduleGap)
                }
            } catch (e: Exception) {
            }
        }
        scopeMap[holder] = scope
        return scope
    }

    fun newScope(holder: Any): CoroutineScope {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scopeMap[holder] = scope
        return scope
    }

    fun cancelTasks(holder: Any) {
        try {
            if (scopeMap.containsKey(holder)) {
                scopeMap[holder]?.cancel()
                scopeMap.remove(holder)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getStrOnUIThread(runnable: Consumer<UrlHolder>): String? {
        val urlHolder = UrlHolder()
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            runnable.accept(urlHolder)
            return urlHolder.url
        }
        val countDownLatch = CountDownLatch(1)
        runOnUI {
            runnable.accept(urlHolder)
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return urlHolder.url
    }

    class UrlHolder {
        var url: String? = null
    }
}