package com.yzz.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分页查询基类。列表页筛选参数继承此类，统一分页口径与上限。
 */
@Schema(description = "分页查询参数")
public class PageQuery {

    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，最大 100", defaultValue = "10")
    private Integer pageSize = 10;

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
