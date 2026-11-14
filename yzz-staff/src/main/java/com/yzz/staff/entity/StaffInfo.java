package com.yzz.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 人员档案。
 */
@Data
@TableName("staff_info")
public class StaffInfo {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 姓名。 */
    private String name;

    /** 手机号（加密）。 */
    private String phone;

    /** 岗位。 */
    private String jobType;

    /** 关联登录账号。 */
    private Long userId;

    /** 状态：在职/离职。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdTime;

    /** 更新时间。 */
    private LocalDateTime updatedTime;

    /** 逻辑删除。 */
    @TableLogic
    private Integer deleted;
}
