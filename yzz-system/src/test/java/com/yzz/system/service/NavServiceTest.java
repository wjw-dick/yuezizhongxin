package com.yzz.system.service;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.system.dto.NavTreeRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 菜单可视化层级规则（T22）。
 */
class NavServiceTest {

    private final NavService navService = new NavService(null);

    private NavTreeRequest.Node root(String title) {
        return new NavTreeRequest.Node(null, 0L, title, "站内页", "/p", null, 0, 1);
    }

    @Test
    void tabBarNeedsTwoToFiveItems() {
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(List.of(root("首页")), "小程序tabBar"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(List.of(root("1"), root("2"), root("3"),
                root("4"), root("5"), root("6")), "小程序tabBar"));
        assertDoesNotThrow(() -> navService.validate("小程序tabBar",
                List.of(root("首页"), root("房型"), root("我的"))));
    }

    @Test
    void officialAccountAllowsAtMostThreeRootsAndTwoLevels() {
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(List.of(root("a"), root("b"), root("c"), root("d")), "公众号"));

        List<NavTreeRequest.Node> nodes = new ArrayList<>();
        NavTreeRequest.Node first = new NavTreeRequest.Node(1L, 0L, "了解我们", "站内页", "/a", null, 0, 1);
        nodes.add(first);
        nodes.add(new NavTreeRequest.Node(2L, 0L, "房型套餐", "站内页", "/b", null, 1, 1));
        nodes.add(new NavTreeRequest.Node(3L, 0L, "预约参观", "站内页", "/c", null, 2, 1));
        for (int i = 0; i < 5; i++) {
            nodes.add(new NavTreeRequest.Node(10L + i, 1L, "子菜单" + i, "站内页", "/a/" + i, null, i, 1));
        }
        assertDoesNotThrow(() -> navService.validate("公众号", nodes));

        // 二级菜单下再挂三级，公众号不允许
        nodes.add(new NavTreeRequest.Node(30L, 10L, "三级菜单", "站内页", "/a/0/x", null, 0, 1));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(nodes, "公众号"));
    }

    @Test
    void h5AllowsAtMostFiveItemsAndNoChildren() {
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(List.of(root("1"), root("2"), root("3"),
                root("4"), root("5"), root("6")), "H5底部导航"));
        assertDoesNotThrow(() -> navService.validate("H5底部导航",
                List.of(root("首页"), root("我的"))));
    }

    @Test
    void parentMustExistInSubmittedTree() {
        NavTreeRequest.Node orphan =
                new NavTreeRequest.Node(9L, 999L, "孤儿菜单", "站内页", "/x", null, 0, 1);
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(List.of(orphan), "PC官网"));
    }

    @Test
    void emptyRootIsRejected() {
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), errorCodeOf(List.of(), "PC后台"));
    }

    private int errorCodeOf(List<NavTreeRequest.Node> nodes, String endType) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> navService.validate(endType, nodes));
        return ex.getErrorCode().getCode();
    }
}
