package com.example.hikerview.utils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.example.hikerview.model.ViewCollection;

import org.litepal.LitePal;

import java.util.Collections;
import java.util.List;

/**
 * @author reborn
 * @program hiker-view
 * @description 用来处理数据源
 * @create 2021-06-08 22:02
 **/
public class DataSourceUtil {
    public static List<String> getCollectionGroups() {
        // 查询所有分组（不重复）
        List<ViewCollection> allCollections;
        // SELECT DISTINCT group_lpcolumn FROM viewcollection WHERE group_lpcolumn NOT NULL
        allCollections = LitePal.select("group_lpcolumn").where("group_lpcolumn NOT NULL").find(ViewCollection.class);
        List<String> groups = Stream.of(allCollections)
                .map(viewCollection -> StringUtil.isEmpty(viewCollection.getGroup()) ? null : viewCollection.getGroup())
                .filter(group -> group != null)
                .distinct()
                .map(String::new)
                .collect(Collectors.toList());
        Collections.sort(groups);
        return groups;
    }
}
