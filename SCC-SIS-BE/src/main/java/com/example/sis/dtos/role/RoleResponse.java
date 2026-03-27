package com.example.sis.dtos.role;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.Map;

public class RoleResponse {
    private Integer roleId;
    private String code;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userCount;
    private Set<Integer> permissionIds;
    private Map<String, Object> summary;

    public RoleResponse() {}

    public RoleResponse(Integer roleId, String code, String name, boolean active,
                       LocalDateTime createdAt, LocalDateTime updatedAt, Long userCount,
                       Set<Integer> permissionIds, Map<String, Object> summary) {
        this.roleId = roleId;
        this.code = code;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userCount = userCount;
        this.permissionIds = permissionIds;
        this.summary = summary;
    }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }

    public Set<Integer> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<Integer> permissionIds) { this.permissionIds = permissionIds; }

    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
}
