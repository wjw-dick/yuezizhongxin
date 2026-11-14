package com.yzz.system.vo;

/**
 * 字典下拉选项：value 为内部编码，label 为中文名称（页面唯一展示值）。
 */
public record DictOptionVO(String value, String label, String color) {
}
