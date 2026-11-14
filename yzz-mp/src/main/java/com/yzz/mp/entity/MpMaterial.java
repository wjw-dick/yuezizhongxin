package com.yzz.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公众号素材。
 */
@Data
@TableName("mp_material")
public class MpMaterial {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识。 */
    private Long tenantId;

    /** 公众号标识。 */
    private Long accountId;

    /** 素材标识。 */
    private String mediaId;

    /** 素材名称。 */
    private String name;

    /** 类型：图片/语音/视频。 */
    private String materialType;

    /** 大小（字节）。 */
    private Long sizeBytes;

    /** 是否永久素材。 */
    private Integer permanent;

    /** 视频缩略图标识。 */
    private String thumbMediaId;

    /** 对象存储键。 */
    private String fileKey;

    /** 创建时间。 */
    private LocalDateTime createdTime;
}
