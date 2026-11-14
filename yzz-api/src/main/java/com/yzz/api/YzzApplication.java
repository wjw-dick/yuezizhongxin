package com.yzz.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 月子中心数字化多端运营平台启动类。
 *
 * <p>模块化单体：一个后端进程装配全部业务域模块，对外只暴露 REST 接口；
 * 多前端壳（平台端、租户后台、官网、小程序双端、H5、App）共用本服务。</p>
 */
@SpringBootApplication(scanBasePackages = "com.yzz")
@MapperScan("com.yzz.**.mapper")
@EnableAsync
@EnableScheduling
public class YzzApplication {

    public static void main(String[] args) {
        SpringApplication.run(YzzApplication.class, args);
    }
}
