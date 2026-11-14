package com.yzz.system.vo;

import java.util.List;

/**
 * 按分组返回的系统参数。
 */
public record ConfigGroupVO(String group, List<Item> items) {

    public record Item(String key, String value, String valueType, String remark) {
    }
}
