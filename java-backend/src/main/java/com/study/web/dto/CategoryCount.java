package com.study.web.dto;

/** 分类统计投影(手写 SQL 的 GROUP BY 结果)。MyBatis 按列名 setter 映射。 */
public class CategoryCount {

    private String category;
    private Long count;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
