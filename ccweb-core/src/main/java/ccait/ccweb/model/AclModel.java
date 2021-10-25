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
@Tablename("${ccweb.table.acl}")
public class AclModel extends Queryable<AclModel> {
  @PrimaryKey
  @Fieldname("aclId")
  private Integer aclId;

  @Fieldname("groupId")
  private Integer groupId;

  @Fieldname("tableName")
  private String tableName;

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

  @Fieldname("${ccweb.table.reservedField.createOn:createOn}")
  private Date createOn;

  @Fieldname("${ccweb.table.reservedField.createBy:createBy}")
  private Integer createBy;

  @Fieldname("${ccweb.table.reservedField.modifyOn:modifyOn}")
  private Date modifyOn;

  @Fieldname("${ccweb.table.reservedField.modifyBy:modifyBy}")
  private Integer modifyBy;

  public Integer getAclId() {
    return this.aclId;
  }

  public void setAclId(Integer aclId) {
    this.aclId = aclId;
  }

  public Integer getGroupId() {
    return this.groupId;
  }

  public void setGroupId(Integer groupId) {
    this.groupId = groupId;
  }

  public String getTableName() {
    return this.tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
}
