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

import ccait.ccweb.enums.PrivilegeScope;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Tablename("${ccweb.table.privilege}")
public class PrivilegeModel extends Queryable<PrivilegeModel> {
  @PrimaryKey
  @Fieldname("privilegeId")
  private Integer privilegeId;

  @Fieldname("groupId")
  private Integer groupId;

  @Fieldname("roleId")
  private Integer roleId;

  public Integer getCanView() {
    return canView;
  }

  public void setCanView(Integer canView) {
    this.canView = canView;
  }

  public Integer getCanDownload() {
    return canDownload;
  }

  public void setCanDownload(Integer canDownload) {
    this.canDownload = canDownload;
  }

  public Integer getCanPreview() {
    return canPreview;
  }

  public void setCanPreview(Integer canPreview) {
    this.canPreview = canPreview;
  }

  public Integer getCanPlayVideo() {
    return canPlayVideo;
  }

  public void setCanPlayVideo(Integer canPlayVideo) {
    this.canPlayVideo = canPlayVideo;
  }

  public Integer getCanUpload() {
    return canUpload;
  }

  public void setCanUpload(Integer canUpload) {
    this.canUpload = canUpload;
  }

  public Integer getCanExport() {
    return canExport;
  }

  public void setCanExport(Integer canExport) {
    this.canExport = canExport;
  }

  public Integer getCanImport() {
    return canImport;
  }

  public void setCanImport(Integer canImport) {
    this.canImport = canImport;
  }

  public Integer getCanDecrypt() {
    return canDecrypt;
  }

  public void setCanDecrypt(Integer canDecrypt) {
    this.canDecrypt = canDecrypt;
  }

  public Integer getCanList() {
    return canList;
  }

  public void setCanList(Integer canList) {
    this.canList = canList;
  }

  @Fieldname("aclId")
  private Integer aclId;

  @Fieldname("canAdd")
  private Integer canAdd;

  @Fieldname("canDelete")
  private Integer canDelete;

  @Fieldname("canUpdate")
  private Integer canUpdate;

  @Fieldname("canQuery")
  private Integer canQuery;

  @Fieldname("canView")
  private Integer canView;

  @Fieldname("canDownload")
  private Integer canDownload;

  @Fieldname("canPreview")
  private Integer canPreview;

  @Fieldname("canPlayVideo")
  private Integer canPlayVideo;

  @Fieldname("canUpload")
  private Integer canUpload;

  @Fieldname("canExport")
  private Integer canExport;

  @Fieldname("canImport")
  private Integer canImport;

  @Fieldname("canDecrypt")
  private Integer canDecrypt;

  @Fieldname("canList")
  private Integer canList;

  @Fieldname("scope")
  private PrivilegeScope scope;

  @Fieldname("${ccweb.table.reservedField.createOn:createOn}")
  private Date createOn;

  @Fieldname("${ccweb.table.reservedField.createBy:createBy}")
  private Integer createBy;

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

  @Fieldname("${ccweb.table.reservedField.modifyOn:modifyOn}")
  private Date modifyOn;

  @Fieldname("${ccweb.table.reservedField.modifyBy:modifyBy}")
  private Integer modifyBy;

  public Integer getPrivilegeId() {
    return this.privilegeId;
  }

  public void setPrivilegeId(Integer privilegeId) {
    this.privilegeId = privilegeId;
  }

  public Integer getRoleId() {
    return this.roleId;
  }

  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
  }

  public Integer getAclId() {
    return this.aclId;
  }

  public void setAclId(Integer aclId) {
    this.aclId = aclId;
  }

  public Integer getCanAdd() {
    return this.canAdd;
  }

  public void setCanAdd(Integer canAdd) {
    this.canAdd = canAdd;
  }

  public Integer getCanDelete() {
    return this.canDelete;
  }

  public void setCanDelete(Integer canDelete) {
    this.canDelete = canDelete;
  }

  public Integer getCanUpdate() {
    return this.canUpdate;
  }

  public void setCanUpdate(Integer canUpdate) {
    this.canUpdate = canUpdate;
  }

  public Integer getCanQuery() {
    return this.canQuery;
  }

  public void setCanQuery(Integer canQuery) {
    this.canQuery = canQuery;
  }

  public Integer getGroupId() {
    return groupId;
  }

  public void setGroupId(Integer groupId) {
    this.groupId = groupId;
  }

  public PrivilegeScope getScope() {
    return scope;
  }

  public void setScope(PrivilegeScope scope) {
    this.scope = scope;
  }

  @Override
  public String getExpression() {
    return null;
  }
}