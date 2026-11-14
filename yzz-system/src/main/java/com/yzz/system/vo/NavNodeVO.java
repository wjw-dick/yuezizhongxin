package com.yzz.system.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单节点树。
 */
public record NavNodeVO(Long id, Long parentId, String title, String linkType, String path,
                        String icon, Integer sort, Integer visible, List<NavNodeVO> children) {

    public NavNodeVO withChildren(List<NavNodeVO> nodes) {
        return new NavNodeVO(id, parentId, title, linkType, path, icon, sort, visible,
                nodes == null ? new ArrayList<>() : nodes);
    }
}
