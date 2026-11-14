package com.yzz.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzz.platform.entity.PlatformMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PlatformMenuMapper extends BaseMapper<PlatformMenu> {

    /** 按平台角色取可见菜单，平台菜单树与租户菜单树互不引用。 */
    @Select("select m.* from platform_menu m "
            + "join platform_role_menu rm on rm.menu_id = m.id "
            + "where rm.role_code = #{roleCode} and m.visible = 1 "
            + "order by m.sort asc")
    List<PlatformMenu> selectByRole(@Param("roleCode") String roleCode);
}
