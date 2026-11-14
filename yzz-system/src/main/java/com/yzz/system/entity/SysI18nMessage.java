package com.yzz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 界面语言包词条（全局）。
 */
@Data
@TableName("sys_i18n_message")
public class SysI18nMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String langCode;

    private String msgKey;

    private String msgValue;

    private LocalDateTime updatedTime;
}
