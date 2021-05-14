package com.example.hikerview.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/1/31
 * 时间：At 11:09
 */

public class StringFindUtil {

    public static void findAllAsync(SearchFindResult findResult, String all, String find, OnFindListener listener) {
        HeavyTaskUtil.executeNewTask(() -> {
            findResult.setFindKey(find);
            findResult.setSelectPos(0);
            if (StringUtil.isEmpty(find) || StringUtil.isEmpty(all)) {
                findResult.setIndexList(new ArrayList<>());
                listener.updateByFindResult(findResult);
                return;
            }
            List<Integer> indexes = new ArrayList<>();
            int index = StringUtils.indexOf(all, find);
            while (index != -1) {
                if (index >= 0 && !indexes.contains(index)) {
                    indexes.add(index);
                }
                index = StringUtils.indexOf(all, find, index + 1);
            }
            findResult.setIndexList(indexes);
            listener.updateByFindResult(findResult);
        });
    }


    public static class SearchFindResult {
        private String findKey;
        private List<Integer> indexList;
        private Integer selectPos;

        public String getFindKey() {
            return findKey;
        }

        public void setFindKey(String findKey) {
            this.findKey = findKey;
        }

        public List<Integer> getIndexList() {
            return indexList;
        }

        public void setIndexList(List<Integer> indexList) {
            this.indexList = indexList;
        }

        public Integer getSelectPos() {
            return selectPos;
        }

        public void setSelectPos(Integer selectPos) {
            this.selectPos = selectPos;
        }
    }

    public interface OnFindListener {
        void updateByFindResult(SearchFindResult findResult);
    }
}
