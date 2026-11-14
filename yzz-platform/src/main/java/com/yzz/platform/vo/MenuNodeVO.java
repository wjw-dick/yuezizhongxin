package com.yzz.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "菜单树节点")
@Data
public class MenuNodeVO {

    private Long id;

    private String title;

    private String path;

    private String icon;

    private Integer sort;

    private List<MenuNodeVO> children = new ArrayList<>();
}
