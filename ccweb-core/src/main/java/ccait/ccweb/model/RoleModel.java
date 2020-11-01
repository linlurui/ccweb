/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.model;

import entity.query.Queryable;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@Scope("prototype")
@Tablename("${entity.table.role}")
public class RoleModel extends Queryable<RoleModel> {
  @PrimaryKey
  @Fieldname("roleId")
  private Integer roleId;

  @Fieldname("roleName")
  private String roleName;

  @Fieldname("description")
  private String description;

  @Fieldname("${entity.table.reservedField.createOn:createOn}")
  private Date createOn;

  @Fieldname("${entity.table.reservedField.createBy:createBy}")
  private Integer createBy;

  @Fieldname("${entity.table.reservedField.modifyOn:modifyOn}")
  private Date modifyOn;

  @Fieldname("${entity.table.reservedField.modifyBy:modifyBy}")
  private Integer modifyBy;

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

  public Integer getRoleId() {
    return this.roleId;
  }

  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
  }

  public String getRoleName() {
    return this.roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
