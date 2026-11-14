package com.yzz.platform.service;

import com.yzz.platform.entity.PlatformMenu;
import com.yzz.platform.mapper.PlatformMenuMapper;
import com.yzz.platform.vo.MenuNodeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台菜单树。按角色取可见菜单，与租户菜单树物理隔离（不同表、不同接口）。
 */
@Service
public class PlatformMenuService {

    private final PlatformMenuMapper menuMapper;

    public PlatformMenuService(PlatformMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public List<MenuNodeVO> treeOf(String roleCode) {
        List<PlatformMenu> menus = menuMapper.selectByRole(roleCode);
        Map<Long, MenuNodeVO> nodes = new LinkedHashMap<>();
        for (PlatformMenu menu : menus) {
            MenuNodeVO node = new MenuNodeVO();
            node.setId(menu.getId());
            node.setTitle(menu.getTitle());
            node.setPath(menu.getPath());
            node.setIcon(menu.getIcon());
            node.setSort(menu.getSort());
            nodes.put(menu.getId(), node);
        }
        List<MenuNodeVO> roots = new ArrayList<>();
        for (PlatformMenu menu : menus) {
            MenuNodeVO node = nodes.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0L || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodes.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }
}
