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
import entity.query.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

@Component
@Scope("request")
@Tablename("${ccweb.table.user}")
public class UserModel extends Queryable<UserModel> {

//  @Autowired
//  @JsonIgnore
//  @JSONField(serialize = false)
//  protected HttpServletRequest request;

  @AutoIncrement
  @Fieldname("${ccweb.table.reservedField.userId:userId}")
  private Integer userId;

  @PrimaryKey
  @Fieldname("username")
  private String username;

  @Fieldname("password")
  private String password;

  @Fieldname("${ccweb.table.reservedField.createOn:createOn}")
  private Date createOn;

  @Fieldname("${ccweb.table.reservedField.createBy:createBy}")
  private Integer createBy;

  @Fieldname("${ccweb.table.reservedField.owner:owner}")
  private Integer owner;

  public Integer getOwner() {
    return owner;
  }

  public void setOwner(Integer owner) {
    this.owner = owner;
  }

  @Fieldname("${ccweb.table.reservedField.modifyOn:modifyOn}")
  private Date modifyOn;

  @Fieldname("${ccweb.table.reservedField.modifyBy:modifyBy}")
  private Integer modifyBy;

  @Fieldname("type")
  private String type;

  @Fieldname("status")
  private Integer status;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Fieldname("key")
  private String key;

  @Exclude
  private String jwtToken;

  public String getAesToken() {
    return aesToken;
  }

  public void setAesToken(String aesToken) {
    this.aesToken = aesToken;
  }

  @Exclude
  private String aesToken;


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

  public void setUserGroupRoleModels(List<UserGroupRoleModel> userGroupRoleModels) {
    this.userGroupRoleModels = userGroupRoleModels;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Exclude
  private List<UserGroupRoleModel> userGroupRoleModels;

  public List<UserGroupRoleModel> getUserGroupRoleModels() {
    return userGroupRoleModels;
  }

  public Integer getUserId() {
    return this.userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Integer getStatus() {
    return this.status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getJwtToken() {
    return jwtToken;
  }

  public void setJwtToken(String jwtToken) {
    this.jwtToken = jwtToken;
  }

}
