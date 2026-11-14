package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.entity.SysMenu;
import com.yzz.system.entity.SysRole;
import com.yzz.system.entity.SysRoleMenu;
import com.yzz.system.mapper.SysMenuMapper;
import com.yzz.system.mapper.SysRoleMapper;
import com.yzz.system.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色权限与菜单（T50）。
 *
 * <p>权限模型：角色 → 菜单/按钮（sys_role_menu）。租户管理员对除审计外的全部模块可增删改查；
 * 审计日志只提供只读查询，任何角色都没有写接口。</p>
 */
@Service
public class RbacService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public RbacService(SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                       SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public List<Map<String, Object>> roles() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .orderByAsc(SysRole::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysRole role : roles) {
            List<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                            .eq(SysRoleMenu::getRoleId, role.getId()))
                    .stream().map(SysRoleMenu::getMenuId).toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", role);
            item.put("menuIds", menuIds);
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Long saveRole(SysRole input) {
        if (input.getRoleName() == null || input.getRoleName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "角色名称不能为空");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getId() == null) {
            if (input.getRoleCode() == null || input.getRoleCode().isBlank()) {
                input.setRoleCode("custom_" + System.currentTimeMillis());
            }
            roleMapper.insert(input);
        } else {
            roleMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "角色不存在");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds.stream().distinct().toList()) {
            SysRoleMenu rel = new SysRoleMenu();
            rel.setTenantId(role.getTenantId());
            rel.setRoleId(roleId);
            rel.setMenuId(menuId);
            roleMenuMapper.insert(rel);
        }
    }

    public List<SysMenu> menus() {
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
    }

    public List<Map<String, Object>> menuTree() {
        List<SysMenu> rows = menus();
        Map<Long, List<SysMenu>> byParent = new LinkedHashMap<>();
        for (SysMenu row : rows) {
            byParent.computeIfAbsent(row.getParentId() == null ? 0L : row.getParentId(),
                    key -> new ArrayList<>()).add(row);
        }
        return build(0L, byParent);
    }

    /** 当前登录角色的菜单与按钮权限。 */
    public Map<String, Object> myPermissions() {
        String roleCode = TenantContext.getRoleCode();
        SysRole role = roleCode == null ? null : roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode).last("limit 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleCode", roleCode);
        result.put("roleName", role == null ? null : role.getRoleName());
        if (role == null) {
            result.put("menus", List.of());
            result.put("perms", List.of());
            return result;
        }
        Set<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, role.getId()))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        List<SysMenu> granted = menus().stream()
                .filter(m -> menuIds.contains(m.getId()) && !Integer.valueOf(0).equals(m.getVisible()))
                .toList();
        List<String> perms = granted.stream().map(SysMenu::getPerm)
                .filter(p -> p != null && !p.isBlank()).toList();
        result.put("menus", granted);
        result.put("perms", perms);
        return result;
    }

    private List<Map<String, Object>> build(Long parentId, Map<Long, List<SysMenu>> byParent) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (SysMenu menu : byParent.getOrDefault(parentId, List.of())) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", menu.getId());
            node.put("parentId", menu.getParentId());
            node.put("title", menu.getTitle());
            node.put("path", menu.getPath());
            node.put("icon", menu.getIcon());
            node.put("menuType", menu.getMenuType());
            node.put("perm", menu.getPerm());
            node.put("sort", menu.getSort());
            node.put("children", build(menu.getId(), byParent));
            nodes.add(node);
        }
        return nodes;
    }
}
