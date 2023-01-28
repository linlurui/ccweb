package ccait.ccweb.model;


import entity.query.Queryable;
import entity.query.annotation.Exclude;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

@Component
@Scope("request")
@Tablename("${ccweb.table.userGroupRole}")
public class UserGroupRoleModel extends Queryable<UserGroupRoleModel> {

    private static final Logger log = LoggerFactory.getLogger( UserGroupRoleModel.class );

    @PrimaryKey
    @Fieldname("userGroupRoleId")
    private String userGroupRoleId;

    @Fieldname("userId")
    private Integer userId;

    @Fieldname("groupId")
    private Integer groupId;

    @Fieldname("roleId")
    private Integer roleId;

    @Fieldname("${ccweb.table.reservedField.userPath:userPath}")
    private String path;

    @Fieldname("${ccweb.table.reservedField.createOn:createOn}")
    private Date createOn;

    @Fieldname("${ccweb.table.reservedField.createBy:createBy}")
    private Integer createBy;

    @Fieldname("${ccweb.table.reservedField.modifyOn:modifyOn}")
    private Date modifyOn;

    @Fieldname("${ccweb.table.reservedField.modifyBy:modifyBy}")
    private Integer modifyBy;

    @Fieldname("${ccweb.table.reservedField.owner:owner}")
    private Integer owner;

    public Integer getOwner() {
        return owner;
    }

    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    public Date getCreateOn() {
        return createOn;
    }

    public void setCreateOn(Date createOn) {
        this.createOn = createOn;
    }

    public Integer getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Integer createBy) {
        this.createBy = createBy;
    }

    public Date getModifyOn() {
        return modifyOn;
    }

    public void setModifyOn(Date modifyOn) {
        this.modifyOn = modifyOn;
    }

    public Integer getModifyBy() {
        return modifyBy;
    }

    public void setModifyBy(Integer modifyBy) {
        this.modifyBy = modifyBy;
    }

    @Exclude
    private GroupModel group;
    public GroupModel getGroup() {

        if(group != null) {
            return group;
        }

        if(this.groupId == null) {
            return group;
        }

        group = new GroupModel();
        group.setGroupId(this.groupId);

        try {
            group = group.where("[groupId]=#{groupId}").first();
        } catch (SQLException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }

        return group;
    }

    @Exclude
    private RoleModel role;
    public RoleModel getRole() {

        if(role != null) {
            return role;
        }

        if(this.roleId == null) {
            return role;
        }

        role = new RoleModel();
        role.setRoleId(this.roleId);

        try {
            role = role.where("[roleId]=#{roleId}").first();
        } catch (SQLException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }

        return role;
    }

    public String getUserGroupRoleId() {
        return userGroupRoleId;
    }

    public void setUserGroupRoleId(String userGroupRoleId) {
        this.userGroupRoleId = userGroupRoleId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
