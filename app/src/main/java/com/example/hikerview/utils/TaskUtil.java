package com.example.hikerview.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * @author reborn
 * @program hiker-view
 * @description 栈操作工具类
 * @create 2020-12-25 04:21
 **/
public class TaskUtil {

    /**
     * 将从 Category 列表中查找到的第一个符合 Category 的 Task 移到前台
     *
     * @param context     上下文
     * @param mCategories Intent-Filter 的 Category 列表，查找时有先后顺序
     */
    public static void moveTaskToFront(Context context, String[] mCategories) {
        moveCategoryTaskToFront(context, mCategories);
    }

    /**
     * 将 Task 列表中的第 0 个 Task 移到前台
     *
     * @param context 上下文
     */
    public static void moveTaskToFront(Context context) {
        moveTaskToFront(context, 0);
    }

    /**
     * 将 Task 列表中的第 index 个 Task 移到前台
     *
     * @param context 上下文
     * @param index   Task 列表的索引
     */
    public static void moveTaskToFront(Context context, int index) {
        moveIndexTaskToFront(context, index);
    }

    /**
     * 是否将 Task 列表中的第 index 个 Task 从多任务中排除
     *
     * @param context 上下文
     * @param index   Task 列表的索引
     * @param exclude 是否排除
     */
    public static void setTaskExcludeFromRecents(Context context, int index, boolean exclude) {
        setIndexTaskExcludeFromRecents(context, index, exclude);
    }

    /**
     * 是否将 Task 列表中的第 0 个 Task 从多任务中排除
     *
     * @param context 上下文
     * @param exclude 是否排除
     */
    public static void setTaskExcludeFromRecents(Context context, boolean exclude) {
        setTaskExcludeFromRecents(context, 0, exclude);
    }

    /**
     * 将 Task 列表中所有符合 mCategories 的 Task 从多任务中排除
     *
     * @param context     上下文
     * @param exclude     是否排除
     * @param mCategories Intent-Filter 的 Category 列表
     */
    public static void setTaskExcludeFromRecents(Context context, boolean exclude, String[] mCategories) {
        setCategoryTaskExcludeFromRecents(context, exclude, mCategories);
    }

    /**
     * 将 Task 列表中的第 index 个 Task 从多任务排除
     *
     * @param context 上下文
     * @param index   Task 列表的索引
     */
    public static void excludeIndexTaskFromRecents(Context context, int index) {
        setTaskExcludeFromRecents(context, index, true);
    }

    /**
     * 将 Task 列表中的第 index 个 Task 加入多任务
     *
     * @param context 上下文
     * @param index   Task 列表的索引
     */
    public static void acceptIndexTaskFromRecents(Context context, int index) {
        setTaskExcludeFromRecents(context, index, false);
    }

    public static void showDetailActivityFromRecents(Context context, boolean isVideoActivityRemoved) {
        int index = isVideoActivityRemoved ? 0 : 1;
        try {
            TaskUtil.acceptIndexTaskFromRecents(context, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideDetailActivityFromRecents(Context context) {
        try {
            TaskUtil.excludeIndexTaskFromRecents(context, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // { "hiker.intent.category.detail", Intent.CATEGORY_LAUNCHER }
    private static void moveCategoryTaskToFront(Context context, String[] mCategories) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        for (ActivityManager.AppTask task : appTasks) {
            final Intent baseIntent = task.getTaskInfo().baseIntent;
            final Set<String> categories = baseIntent.getCategories();
            if (categories != null) {
                boolean hasFront = false;
                for (String category : mCategories) {
                    if (hasFront = hasFront || categories.contains(category)) {
                        task.moveToFront();
                        return;
                    }
                }
            }
        }
    }

    private static void moveIndexTaskToFront(Context context, int index) {
        if (index < 0) {
            return;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        if (appTasks != null && appTasks.size() > 0 && appTasks.size() > index) {
            Timber.tag("TaskUtil").d("%s#moveIndexTaskToFront#TaskId#%s#index#%s", ((Activity) context).getLocalClassName(), appTasks.get(index).getTaskInfo().id, index);
            appTasks.get(index).moveToFront();
        }
    }

    public static void setExcludeFromRecentTasks(Context context, Class<?> exclude) {
        setExcludeFromRecentTasks(context, exclude, true);
    }

    public static void setExcludeFromRecentTasks(Context context, Class<?> clazz, boolean exclude) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        if (appTasks != null && appTasks.size() > 0) {
            for (ActivityManager.AppTask appTask : appTasks) {
                Intent baseIntent = appTask.getTaskInfo().baseIntent;
                if (baseIntent.getComponent() != null
                        && clazz.getName().equals(baseIntent.getComponent().getClassName())) {
                    appTask.setExcludeFromRecents(exclude);
                }
            }
        }
    }

    private static void setIndexTaskExcludeFromRecents(Context context, int index, boolean exclude) {
        if (index < 0) {
            return;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        if (appTasks != null && appTasks.size() > 0 && appTasks.size() > index) {
            Timber.tag("TaskUtil").d("%s#setIndexTaskExcludeFromRecents#TaskId#%s#index#%s#exclude#%s", ((Activity) context).getLocalClassName(), appTasks.get(index).getTaskInfo().id, index, exclude);
            appTasks.get(index).setExcludeFromRecents(exclude);
        }
    }

    // { "hiker.intent.category.player" }
    private static void setCategoryTaskExcludeFromRecents(Context context, boolean exclude, String[] mCategories) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        for (ActivityManager.AppTask task : appTasks) {
            final Intent baseIntent = task.getTaskInfo().baseIntent;
            final Set<String> categories = baseIntent.getCategories();
            if (categories != null) {
                boolean hasExclude = false;
                for (String category : mCategories) {
                    if (hasExclude = hasExclude || categories.contains(category)) {
                        task.setExcludeFromRecents(exclude);
                        break;
                    }
                }
            }
        }
    }
}
