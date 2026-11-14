package com.yzz.common.tenant;

import java.util.Collections;
import java.util.Set;

/**
 * 租户与登录上下文。
 *
 * <p>一次请求内的租户标识、登录主体与角色集合全部存放在 ThreadLocal，
 * 请求结束由过滤器无条件清理，避免线程复用导致串租户。</p>
 */
public final class TenantContext {

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void initTenant(Long tenantId, String tenantCode) {
        Holder holder = current();
        holder.tenantId = tenantId;
        holder.tenantCode = tenantCode;
    }

    public static void initLogin(Long userId, String username, boolean platform, String roleCode) {
        Holder holder = current();
        holder.userId = userId;
        holder.username = username;
        holder.platform = platform;
        holder.roleCode = roleCode;
    }

    public static void setRoles(Set<String> roles) {
        current().roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
    }

    public static Holder holder() {
        return current();
    }

    public static Long getTenantId() {
        return current().tenantId;
    }

    public static String getTenantCode() {
        return current().tenantCode;
    }

    public static Long getUserId() {
        return current().userId;
    }

    public static String getUsername() {
        return current().username;
    }

    public static boolean isPlatform() {
        return current().platform;
    }

    public static String getRoleCode() {
        return current().roleCode;
    }

    public static Set<String> getRoles() {
        return current().roles;
    }

    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new TenantAccessDeniedException("缺少租户上下文，拒绝访问");
        }
        return tenantId;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static Holder current() {
        Holder holder = HOLDER.get();
        if (holder == null) {
            holder = new Holder();
            HOLDER.set(holder);
        }
        return holder;
    }

    /**
     * 请求级上下文载体。字段可变，仅在当前线程内使用。
     */
    public static final class Holder {
        private Long tenantId;
        private String tenantCode;
        private Long userId;
        private String username;
        private boolean platform;
        private String roleCode;
        private Set<String> roles = Collections.emptySet();
        private String tenantState;

        public Long getTenantId() {
            return tenantId;
        }

        public String getTenantCode() {
            return tenantCode;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public boolean isPlatform() {
            return platform;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public String getTenantState() {
            return tenantState;
        }

        public void setTenantState(String tenantState) {
            this.tenantState = tenantState;
        }
    }
}
