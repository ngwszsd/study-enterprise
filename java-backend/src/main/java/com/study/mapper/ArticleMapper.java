package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.domain.Article;
import com.study.web.dto.CategoryCount;
import java.util.List;
import org.apache.ibatis.annotations.Select;

/** 文章 Mapper。分页用 selectPage + 分页插件;统计用手写 SQL(@Select)。 */
public interface ArticleMapper extends BaseMapper<Article> {

    /** 手写 SQL:按分类统计文章数(演示 MyBatis 原生 SQL,MyBatis-Plus 之外的能力)。 */
    @Select("SELECT category AS category, COUNT(*) AS count FROM articles "
            + "WHERE category IS NOT NULL GROUP BY category ORDER BY count DESC")
    List<CategoryCount> countByCategory();
}
