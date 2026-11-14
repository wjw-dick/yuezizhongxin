package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.dto.NavTreeRequest;
import com.yzz.system.entity.SysNavNode;
import com.yzz.system.mapper.SysNavNodeMapper;
import com.yzz.system.vo.NavNodeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多端菜单可视化（T22）。
 *
 * <p>整树保存：前端拖拽后一次性提交扁平节点列表，服务端校验层级上限后整体替换，
 * 避免逐个节点保存出现中间态。层级规则按端区分：</p>
 * <ul>
 *   <li>公众号：一级 ≤3，二级 ≤5，最多两级；</li>
 *   <li>小程序 tabBar：2~5 项，仅一级；</li>
 *   <li>H5 底部导航：≤5 项，仅一级；</li>
 *   <li>PC 官网 / PC 后台：最多三级，同级 ≤12。</li>
 * </ul>
 */
@Service
public class NavService {

    private static final Set<String> FLAT_ENDS = Set.of("小程序tabBar", "H5底部导航");

    private final SysNavNodeMapper navNodeMapper;

    public NavService(SysNavNodeMapper navNodeMapper) {
        this.navNodeMapper = navNodeMapper;
    }

    public List<NavNodeVO> tree(String endType) {
        List<SysNavNode> rows = navNodeMapper.selectList(Wrappers.<SysNavNode>lambdaQuery()
                .eq(SysNavNode::getEndType, endType)
                .orderByAsc(SysNavNode::getSort)
                .orderByAsc(SysNavNode::getId));
        Map<Long, List<NavNodeVO>> byParent = new LinkedHashMap<>();
        for (SysNavNode row : rows) {
            NavNodeVO node = new NavNodeVO(row.getId(), row.getParentId(), row.getTitle(),
                    row.getLinkType(), row.getPath(), row.getIcon(), row.getSort(),
                    row.getVisible(), new ArrayList<>());
            byParent.computeIfAbsent(row.getParentId() == null ? 0L : row.getParentId(),
                    key -> new ArrayList<>()).add(node);
        }
        return build(byParent, 0L);
    }

    @Transactional
    public List<NavNodeVO> saveTree(NavTreeRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        List<NavTreeRequest.Node> nodes = request.nodes();
        validate(request.endType(), nodes);

        List<SysNavNode> existing = navNodeMapper.selectList(Wrappers.<SysNavNode>lambdaQuery()
                .eq(SysNavNode::getEndType, request.endType()));
        Map<Long, SysNavNode> existingById = new HashMap<>();
        for (SysNavNode row : existing) {
            existingById.put(row.getId(), row);
        }

        // 按层级从浅到深处理：父节点先落库，子节点才能拿到真实主键
        Map<Long, Integer> levelCache = new HashMap<>();
        List<NavTreeRequest.Node> ordered = new ArrayList<>(nodes);
        ordered.sort((a, b) -> Integer.compare(levelOf(a, nodes, levelCache), levelOf(b, nodes, levelCache)));

        Map<Long, Long> tempToReal = new HashMap<>();
        Set<Long> keep = new HashSet<>();
        int index = 0;
        for (NavTreeRequest.Node node : ordered) {
            SysNavNode entity;
            if (node.id() != null && node.id() > 0) {
                entity = existingById.get(node.id());
                if (entity == null) {
                    throw BusinessException.of(ErrorCode.BAD_REQUEST,
                            "菜单「" + node.title() + "」不属于当前端，请刷新后重试");
                }
            } else {
                entity = new SysNavNode();
                entity.setTenantId(tenantId);
                entity.setEndType(request.endType());
            }
            Long parentId = 0L;
            if (node.parentId() != null && node.parentId() != 0L) {
                parentId = node.parentId() > 0 ? node.parentId() : tempToReal.getOrDefault(node.parentId(), 0L);
            }
            entity.setParentId(parentId);
            entity.setTitle(node.title().trim());
            entity.setLinkType(node.linkType() == null ? "站内页" : node.linkType());
            entity.setPath(node.path());
            entity.setIcon(node.icon());
            entity.setSort(node.sort() == null ? index : node.sort());
            entity.setVisible(node.visible() == null ? 1 : node.visible());
            if (entity.getId() == null) {
                navNodeMapper.insert(entity);
                if (node.id() != null && node.id() < 0) {
                    tempToReal.put(node.id(), entity.getId());
                }
            } else {
                navNodeMapper.updateById(entity);
                keep.add(entity.getId());
            }
            index++;
        }
        for (SysNavNode row : existing) {
            if (!keep.contains(row.getId())) {
                navNodeMapper.deleteById(row.getId());
            }
        }
        return tree(request.endType());
    }

    /** 计算节点层级，仅用于保存顺序，不参与校验结论。 */
    private int levelOf(NavTreeRequest.Node node, List<NavTreeRequest.Node> nodes,
                        Map<Long, Integer> cache) {
        if (node.parentId() == null || node.parentId() == 0L) {
            return 0;
        }
        if (node.id() != null && cache.containsKey(node.id())) {
            return cache.get(node.id());
        }
        for (NavTreeRequest.Node candidate : nodes) {
            if (candidate.id() != null && candidate.id().equals(node.parentId())) {
                int level = levelOf(candidate, nodes, cache) + 1;
                if (node.id() != null) {
                    cache.put(node.id(), level);
                }
                return level;
            }
        }
        return 0;
    }

    /**
     * 校验层级与节点完整性。任何一条不满足都整单拒绝，不落半棵树。
     */
    void validate(String endType, List<NavTreeRequest.Node> nodes) {
        Map<Long, NavTreeRequest.Node> byId = new HashMap<>();
        for (NavTreeRequest.Node node : nodes) {
            if (node.id() != null) {
                byId.put(node.id(), node);
            }
        }
        List<NavTreeRequest.Node> roots = new ArrayList<>();
        Map<Long, List<NavTreeRequest.Node>> children = new HashMap<>();
        for (NavTreeRequest.Node node : nodes) {
            Long parentId = node.parentId() == null ? 0L : node.parentId();
            if (parentId != 0L && !byId.containsKey(parentId)) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST,
                        "菜单「" + node.title() + "」的上级不存在，请刷新后重试");
            }
            if (parentId == 0L) {
                roots.add(node);
            } else {
                children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
            }
        }
        if (roots.isEmpty()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "至少保留一个一级菜单");
        }
        for (NavTreeRequest.Node root : roots) {
            if (depth(root, children, new HashSet<>()) > 3) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "菜单层级最多三级");
            }
        }

        switch (endType) {
            case "公众号" -> {
                if (roots.size() > 3) {
                    throw BusinessException.of(ErrorCode.BAD_REQUEST, "公众号一级菜单最多 3 个");
                }
                for (NavTreeRequest.Node root : roots) {
                    List<NavTreeRequest.Node> sub = children.getOrDefault(root.id(), List.of());
                    if (sub.size() > 5) {
                        throw BusinessException.of(ErrorCode.BAD_REQUEST,
                                "公众号「" + root.title() + "」下二级菜单最多 5 个");
                    }
                    for (NavTreeRequest.Node child : sub) {
                        if (!children.getOrDefault(child.id(), List.of()).isEmpty()) {
                            throw BusinessException.of(ErrorCode.BAD_REQUEST, "公众号菜单最多两级");
                        }
                    }
                }
            }
            case "小程序tabBar" -> {
                if (FLAT_ENDS.contains(endType) && (roots.size() < 2 || roots.size() > 5)) {
                    throw BusinessException.of(ErrorCode.BAD_REQUEST, "小程序 tabBar 需 2~5 项");
                }
                requireFlat(children);
            }
            case "H5底部导航" -> {
                if (roots.size() > 5) {
                    throw BusinessException.of(ErrorCode.BAD_REQUEST, "H5 底部导航最多 5 项");
                }
                requireFlat(children);
            }
            default -> {
                for (NavTreeRequest.Node root : roots) {
                    if (children.getOrDefault(root.id(), List.of()).size() > 12) {
                        throw BusinessException.of(ErrorCode.BAD_REQUEST,
                                "「" + root.title() + "」下同级菜单最多 12 个");
                    }
                }
            }
        }
    }

    private void requireFlat(Map<Long, List<NavTreeRequest.Node>> children) {
        if (!children.isEmpty()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "该端菜单只支持一级，请移除子菜单");
        }
    }

    private int depth(NavTreeRequest.Node node, Map<Long, List<NavTreeRequest.Node>> children,
                      Set<Long> path) {
        if (node.id() == null) {
            return 1;
        }
        if (!path.add(node.id())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "菜单层级存在循环引用");
        }
        int max = 1;
        for (NavTreeRequest.Node child : children.getOrDefault(node.id(), List.of())) {
            max = Math.max(max, 1 + depth(child, children, path));
        }
        path.remove(node.id());
        return max;
    }

    private List<NavNodeVO> build(Map<Long, List<NavNodeVO>> byParent, Long parentId) {
        List<NavNodeVO> nodes = byParent.getOrDefault(parentId, List.of());
        List<NavNodeVO> result = new ArrayList<>(nodes.size());
        for (NavNodeVO node : nodes) {
            result.add(node.withChildren(build(byParent, node.id())));
        }
        return result;
    }
}
