package com.example.hikerview.constants;

import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;

/**
 * 作者：By 15968
 * 日期：On 2019/11/5
 * 时间：At 21:14
 */
public class JSONPreFilter {

    public static SimplePropertyPreFilter getSimpleFilter() {
        SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
        filter.getExcludes().add("associatedModelsMapForJoinTable");
        filter.getExcludes().add("associatedModelsMapWithFK");
        filter.getExcludes().add("associatedModelsMapWithoutFK");
        filter.getExcludes().add("fieldsToSetToDefault");
        filter.getExcludes().add("listToClearAssociatedFK");
        filter.getExcludes().add("listToClearSelfFK");
        filter.getExcludes().add("saved");
        filter.getExcludes().add("order");
        filter.getExcludes().add("id");
        filter.getExcludes().add("gmtCreate");
        filter.getExcludes().add("gmtModified");
        filter.getExcludes().add("lastUseTime");
        return filter;
    }

    public static SimplePropertyPreFilter getSimpleOnlyRulesFilter() {
        SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
        filter.getExcludes().add("associatedModelsMapForJoinTable");
        filter.getExcludes().add("associatedModelsMapWithFK");
        filter.getExcludes().add("associatedModelsMapWithoutFK");
        filter.getExcludes().add("fieldsToSetToDefault");
        filter.getExcludes().add("listToClearAssociatedFK");
        filter.getExcludes().add("listToClearSelfFK");
        filter.getExcludes().add("saved");
        filter.getExcludes().add("order");
        filter.getExcludes().add("id");
        filter.getExcludes().add("pinYin");
        filter.getExcludes().add("firstHeader");
        return filter;
    }
}
