package com.example.hikerview.convertor;

import com.example.hikerview.model.MovieRule;
import com.example.hikerview.model.MovieRuleDO;
import com.example.hikerview.utils.PinyinUtil;

/**
 * 作者：By 15968
 * 日期：On 2019/10/2
 * 时间：At 10:43
 */
public class MovieRuleConvertor {
    public static MovieRule fromDO(MovieRuleDO movieRuleDO){
        MovieRule movieRule = new MovieRule();
        movieRule.setId(movieRuleDO.getId());
        movieRule.setTitle(movieRuleDO.getTitle());
        movieRule.setBaseUrl(movieRuleDO.getBaseUrl());
        movieRule.setSearchUrl(movieRuleDO.getSearchUrl());
        movieRule.setSearchFind(movieRuleDO.getSearchFind());
        movieRule.setChapterFind(movieRuleDO.getChapterFind());
        movieRule.setMovieFind(movieRuleDO.getMovieFind());
        movieRule.setPinYinTitle(PinyinUtil.instance().getPinyin(movieRuleDO.getTitle()));
        return movieRule;
    }

    public static MovieRuleDO toDO(MovieRule movieRule){
        MovieRuleDO movieRuleDO = new MovieRuleDO();
        movieRuleDO.setId(movieRule.getId());
        movieRuleDO.setTitle(movieRule.getTitle());
        movieRuleDO.setBaseUrl(movieRule.getBaseUrl());
        movieRuleDO.setSearchUrl(movieRule.getSearchUrl());
        movieRuleDO.setSearchFind(movieRule.getSearchFind());
        movieRuleDO.setChapterFind(movieRule.getChapterFind());
        movieRuleDO.setMovieFind(movieRule.getMovieFind());
        return movieRuleDO;
    }
}
