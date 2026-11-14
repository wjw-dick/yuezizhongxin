package com.yzz.room.job;

import com.yzz.room.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 销售预留超时释放（T32）。
 *
 * <p>每分钟扫描一次过期的 LOCKED 档期并释放；单轮量级可控（预留默认 30 分钟）。
 * 多实例部署时由数据库行锁保证幂等：删除已不存在的档期不会影响结果。</p>
 */
@Component
public class ScheduleReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduleReleaseJob.class);
    private final ScheduleService scheduleService;

    public ScheduleReleaseJob(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void releaseExpired() {
        try {
            int released = scheduleService.releaseExpiredHolds();
            if (released > 0) {
                log.info("超时销售预留已释放 {} 条", released);
            }
        } catch (Exception e) {
            log.warn("销售预留释放任务执行失败", e);
        }
    }
}
