package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 多端菜单可视化节点（T22）。层级上限按端区分，保存时统一校验。
 */
@Data
@TableName("sys_nav_node")
public class SysNavNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 公众号 / PC官网 / PC后台 / 小程序tabBar / H5底部导航。 */
    private String endType;

    private Long parentId;

    private String title;

    /** 站内页 / 外链 / 小程序页 / 关键词回复。 */
    private String linkType;

    private String path;

    private String icon;

    private Integer sort;

    private Integer visible;
}
