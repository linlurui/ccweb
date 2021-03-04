CCWEB是基于springboot设计的CQRS敏捷web api开发框架，CCWEB提倡动态向前端提供基础数据，由前端根据基础数据组装业务来提高开发效率;内置用户管理、权限设置 等安全模块，启动服务后无需添加任何后端代码前端便可以通过默认接口直接访问到自己在数据库建的表和查询视图；底层orm采用entityQueryable访问数据，支持SpringCloud微服务扩展；支持elasticSerach搜索引擎；在横向扩展方面ccweb兼容了多种数据库系统，包括主流的mysql、mariadb、sqlserver、oracle、postgresql、derby、sqlite、db2、sybase和大数据存储的hadoop等，有易于数据集成及高度扩展的能力，可以让数据自由地穿梭于各种数据存储系统之间：项目包含ccweb-core，ccweb-api，ccweb-start，ccweb-auth(2.0)，ccweb-config(2.0)，ccweb-office(2.0)，ccweb-gateway(2.0)，ccweb-logs(2.0)，ccweb-iot(2.0)，ccweb-socket(2.0)，ccweb-admin(2.0)
</p>
    <img align="right" src="https://github.com/linlurui/entityQueryable/raw/master/pay5.jpg" alt="捐赠给作者"  width="200">
    <p align="right">
        <em>捐赠给作者</em>
    </p>
</p>

# ccweb-start
ccweb-start是ccweb-api的启动包，其中包含了springcloud的微服务组件与springboos2.0

## 运行环境
* jdk1.8

## 文件结构
* ccweb-start-2.0.0-SNAPSHOT.jar 【ccweb默认服务启动包 [下载](https://github.com/linlurui/ccweb/raw/master/release/ccweb-start-2.0.0-SNAPSHOT.jar)】
* application.yml 【应用程序主配置文件 [详情](https://github.com/linlurui/ccweb/blob/master/release/application.yml)】
* db-config.xml 【数据库连接配置文件 [详情](https://github.com/linlurui/ccweb/blob/master/release/db-config.xml)】
* entity.queryable-2.0-SNAPSHOT.jar【动态查询依赖包 [下载](https://github.com/linlurui/ccweb/raw/master/release/libs/entity.queryable-2.0-SNAPSHOT.jar)】
* rxjava-2.1.10.jar【查询结果异步IO依赖包 [下载](https://github.com/linlurui/ccweb/raw/master/release/libs/rxjava-2.1.10.jar)】
* spring-context-5.0.4.RELEASE.jar【动态实体注入依赖包 [下载](https://github.com/linlurui/ccweb/raw/master/release/libs/spring-context-5.0.4.RELEASE.jar)】
* install.sh【linux系统依赖包安装脚本，需要先安装JDK1.8并且使用JDK自带的JRE，windows下需要安装cygwin来运行该脚本 [详情](https://github.com/linlurui/ccweb/blob/master/release/install.sh)】
* log4j2.xml 【可选，log4j2日志配置文件，记录ccweb服务异常信息 [详情](https://github.com/linlurui/ccweb/blob/master/release/log4j2.xml)】

## 服务启动命令
***java -jar ccweb-start-2.0.0-SNAPSHOT.jar***

## 接口说明
ccweb-start内置了默认的api接口可以让前端直接通过表名操作数据，需要限制访问的可以设置系统默认创建的用户权限表进行控制，接口的请求类型同时支持json和表单提交，表单中存在文件上传的会自动上传到表的字段中，字段类型必须为blob。

### 1. 新增 (可批量)
* URL：/api/{datasource}/{table} 
* 请求方式：PUT
* URL参数：{datasource},{table}为数据库表名称
* POST参数：
```javascript
[
    {
      "字段名": "值",
      ...
    }
    ...
]
```


### 2. 删除
* URL：/api/{datasource}/{table}/{id} 
* 请求方式：DELETE
* URL参数：{table}为数据库表名称，{id}为主键
* POST参数：无


### 3. 修改
* URL：/api/{datasource}/{table}/{id} 
* 请求方式：PUT
* URL参数：{table}为数据库表名称，{id}为主键
* POST参数：
```javascript
{
  "字段名": "值", 
  ...
}
```


### 4. 查询
* URL：/api/{datasource}/{table} 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
    }, ... ]
}
```


### 5. 查询总数
* URL：/api/{datasource}/{table}/count 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
    }, ... ]
}
```



### 6. 查询是否存在数据
* URL：/api/{datasource}/{table}/exist 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
    }, ... ]
}
```


### 7. 联表查询
* URL：/api/{datasource}/join 
* 请求方式：POST
* URL参数：{datasource}为数据源ID
* POST参数：
```javascript
{
    "joinTables": [{
        "tablename": "salary",
        "alias": "a",
        "joinMode": "inner"
    }, {
        "tablename": "archives",
        "alias": "b",
        "joinMode": "Inner",
        "onList": [{ 
            "name": "b.id",   
            "value": "a.archives_id",   
            "algorithm": "EQ"
        }]
    }, ...],
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
    }, ... ]
}
```


### 8. 联表查询统计
* URL：/api/{datasource}/join/count 
* 请求方式：POST
* URL参数：{datasource}为数据源ID
* POST参数：
```javascript
{
    "joinTables": [{
        "tablename": "salary",
        "alias": "a",
        "joinMode": "inner"
    }, {
        "tablename": "archives",
        "alias": "b",
        "joinMode": "Inner",
        "onList": [{ 
            "name": "b.id",   
            "value": "a.archives_id",   
            "algorithm": "EQ"
        }]
    }, ...],
    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "groupList" : [ //分组条件
        "id", //字段名 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...]
}
```


### 9. ID查询
查询与联合查询加密的字段不会解密显示，多用于列表，而ID查询的结果可以显示解密后内容，可用于保密详情。
* URL：/api/{datasource}/{table}/{id} 
* 请求方式：GET
* URL参数：{table}为数据库表名称，{id}为主键
* POST参数：无


### 10. 登录
* URL：/api/{datasource}/login 
* 注：如果引用了ccweb-auth模块和redis，URL可以使用 /api/login做分布式登录
* 请求方式：POST
* POST参数：
```javascript
{
  "username": "用户名",
  "password": "密码",
}
```


### 11. 登出
* URL：/api/{datasource}/logout 
* 注：如果引用了ccweb-auth模块和redis，URL可以使用 /api/logout做分布式登出
* 请求方式：GET


### 12. 下载文件
* URL：/api/{datasource}/download/{table}/{field}/{id} 
* 请求方式：GET
* URL参数：{table}为数据库表名称，{field}为字段名，{id}为主键
* POST参数：无


### 13. 文件预览（支持预览图片、视频、PPT）
* URL：/api/{datasource}/preview/{table}/{field}/{id}/{page} 
* 请求方式：GET
* URL参数：{table}为数据库表名称，{field}为字段名，{id}为主键，{page}为可选入参，可指定页码
* POST参数：无


### 14. 上传
* URL：/api/{datasource}/{table}/{field}/upload 
* 请求方式：POST
* URL参数：{table}为数据库表名称，{field}为字段名
* POST参数：
```javascript
表单：
    name1: 文件1
    name2: 文件2
    name3: 文件3
    ...
```
* 返回：
```javascript
{
    name1: 相对路径1
    name2: 相对路径2
    name3: 相对路径3
}
```


### 15. 批量查询更新
* URL：/api/{datasource}/{table}/update 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "data": {
        "字段名": "值",
        ...
    },
    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...]
}
```


### 16. 批量删除
* URL：/api/{datasource}/{table}/delete 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
[id1, id2, ...]
```



### 17. 导出excel
* URL：/api/{datasource}/{table}/export 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "name",    //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
        "alias": "姓名",    //别名，导出字段的表头名称，可以是中文
    }, ... ]
}
```


### 18. 联表查询导出excel
* URL：/api/{datasource}/export/join 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "joinTables": [{
        "tablename": "salary",
        "alias": "a",
        "joinMode": "inner"
    }, {
        "tablename": "archives",
        "alias": "b",
        "joinMode": "Inner",
        "onList": [{ 
            "name": "b.id",   
            "value": "a.archives_id",   
            "algorithm": "EQ"
        }]
    }, ...],

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2, "="), GT(3, ">"), LT(4, "<"), GTEQ(5, ">="), LTEQ(6, "<="), NOT(7, "<>"), NOTEQ(8, "!="), LIKE(9), START(10), END(11), IN(12), NOTIN(13)
    }, ... ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值
    }, ...],

    "selectList": [{ //显示字段
        "field": "id",  //字段名 
        "function": "MAX",  //数据库相关函数：MAX, MIN, UPPER, LOWER, LENGTH, AVG, COUNT, SUM, GROUP_CONCAT等; 
        "alias": "姓名",    //别名，导出字段的表头名称，可以是中文
    }, ... ]
}
```

### 19. 新增(返回指定字段的最大值)
* URL：/api/{datasource}/{table}/max/{field} 
* 请求方式：PUT
* URL参数：{datasource}数据源,{table}为数据库表名称,{field}为要返回的字段名,接口会返回该字段最后插入的值
* POST参数：
```javascript
[
    {
      "字段名": "值",
      ...
    }
    ...
]
```

### 20. 视频播放
* URL：/api/{datasource}/play/{table}/{field}/{id} 
* 请求方式：GET
* URL参数：{table}为数据库表名称，{field}为字段名，{id}为主键
* POST参数：无

### 21. 导入excel
* URL：/api/{datasource}/{table}/import 
* 请求方式：POST
* URL参数：{datasource}数据源,{table}为数据库表名称,{field}为要返回的字段名,接口会返回该字段最后插入的值
* POST参数：
```javascript
表单：
    文件名1: 文件1
    文件名2: 文件2
    文件名3: 文件3
    ...
```
* Excel文件格式：
 1. 需要导入的excel文件中新增一个名称为schema的sheet
 2. schema的第一行为需要导入的表格表头
 3. schema的第二行为对应数据库的字段名

### 22. 获取当前登录用户
* URL：/api/{datasource}/session/user 
* 请求方式：GET

### 23. Websocket消息推送（企业版2.0功能，需要引用ccweb-socket）
* URL：/api/message/send 
* 请求方式：POST
* URL参数：无
* POST参数：
* 注意：该接口需要引入ccweb-socket包
```javascript
表单：
{
    "message": "my message", //消息内容
    "receiver": {  //接收人
        "groupId": "",  //组ID
        "roleId": "",   //角色ID
        "usernames": [] //用户名
    },
    "sendMode": "ALL"   //发送方式: ALL(0, "ALL"), USER(1, "USER"), GROUP(2, "GROUP"), ROLE(3, "ROLE")
}
```

### 24. 通过es搜索引擎查询数据（企业版2.0功能）
* URL：/api/{datasource}/search/{table} 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
* 注意：使用该接口需要在application.yml配置中将elasticSearch.enable设为true，然后新增或修改数据时才会创建索引
```javascript
{
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2), GT(3), LT(4), GTEQ(5), LTEQ(6), NOT(7), LIKE(9), IN(12), NOTIN(13)
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "max(id) as maxId", //格式类SQL的select子句写法，聚合函数参考Elasticsearch 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值(可写通配符*，中文通配符查询效果以分词准)
    }, ...]
}
```

### 25. 数据滚动接口（企业版2.0功能）
* URL：/api/{datasource}/{table}/stream 
* 请求方式：POST
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
{
    "pageInfo" : {
        "pageIndex": 1, //页码
        "pageSize": 50  //每页条数
    },

    "conditionList": [{ //查询条件
        "name": "id",   //字段名
        "value": "1",   //值
        "algorithm": "EQ",   //条件: EQ(2), GT(3), LT(4), GTEQ(5), LTEQ(6), NOT(7), LIKE(9), IN(12), NOTIN(13)
    }, ... ],

    "sortList": [{ //排序条件
        "name": "id", //字段名 
        "desc": true  //true为降序，false为升序
    }, ... ],

    "groupList" : [ //分组条件
        "max(id) as maxId", //格式类SQL的select子句写法，聚合函数参考Elasticsearch 
        ...
    ],

    "keywords" : [{ //关键词模糊查询条件
        "name": "id",   //字段名
        "value": "1"   //值(可写通配符*，中文通配符查询效果以分词准)
    }, ...]
}
```


### 26. 导入文件并转为PDF接口（企业版2.0功能，需要引用ccweb-office）
* URL：/api/{datasource}/import/to/pdf 
* 请求方式：POST|PUT
* URL参数：{table}为数据库表名称
* POST参数：
```javascript
表单：
    save_full_text: true //是否全文索引，可选项
    字段: 文件
    ...
```

### 27. 发布消息到MQTT服务器（企业版2.0功能，需要引用ccweb-iot）
* URL：/api/mqtt/{datasource}/publish/{table}/{topic}/{qos}/{retained} 
* 请求方式：POST
* URL参数：{datasource}=数据源；{table}=数据库表名称；{topic}发布主题；{qos}=0：最多一次的传输，1：至少一次的传输，2：只有一次的传输；{retained}是否保留消息
* POST参数（要发布的消息，JSON格式）：
```javascript
{
    "字段名" : "数据",
    ...
}
```

## 系统用户/权限表结构说明
用户权限相关表在服务启动时会自动创建，目的在于使用系统服务控制数据库表的访问权限，用户组是扁平结构的，需要更复杂的权限控制功能建议通过二次开发实现。
* 用户表 (user, 主键userId, username[用户名], password[密码], type[用户类型], status[状态])
* 用户组 (group, 主键groupId, groupName[组名], description[描述])
* 角色表 (role, 主键roleId, roleName[角色名])
* 用户/组/角色关联关系表 (userGroupRole, 主键userGroupRoleId, 外键关联userId、groupId、roleId, userPath[用户层级路径,每个组下创建的用户ID组成])
* 数据访问控制表 (acl, 主键aclId, 外键关联groupId, tableName[需要控制访问的表名])
* 操作权限表 (privilege, 主键privilegeId, 外键关联groupId、roleId、aclId, scope[数据权限控制范围])
### privilege表scope字段说明：
* SELF=0(自己的数据)
* NO_GROUP=1(无分组数据)
* GROUP=2(同组数据)
* CHILD=3(子组数据)
* PARENT_AND_CHILD=4(父与子)
* ALL=5(所有)
### privilege表其它字段说明：
* canAdd (允许新增，1为允许，默认值：0)
* canDelete (允许删除)
* canUpdate (允许修改)
* canView (允许查看详情)
* canDownload (允许下载)
* canPreview (允许预览)
* canPlayVideo (允许播放视频)
* canUpload (允许上传)
* canExport (允许导出Excel)
* canImport (允许Excel导入)
* canDecrypt (允许解密加密的内容)
* canList (允许浏览列表)
* canQuery (允许查询数据)


# 二次开发
ccweb的二次开发实际就是自定义ccweb-start包的过程，springboot的启动类注解需要加上@SpringBootApplication(scanBasePackages = "ccait.ccweb")才会去扫描ccweb-core的bean。
## jar包介绍
* ccweb-core: ccweb的核心公共库
* ccweb-api: 提供RESTful接口服务和websocket服务，内置ccweb-core，不能直接起动，需要在ccweb-start中提供入口启动jar包。

## Maven仓库中引入jar包
```xml
    <repositories>
        <repository>
            <id>ccweb</id>
            <url>https://raw.github.com/linlurui/ccweb/release</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>ccait.cn</groupId>
            <artifactId>ccweb-api</artifactId>
            <version>2.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
```

## Ccweb启动方法
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        CcwebAppliction.run(Application.class, args);
    }
}
```
## 生成实体类
* ccweb虽然支持通过请求动态生成数据查询实体类，但推荐在二次开发的时候通过实体生成器生成数据查询的实体以提高访问的性能，实体生在器在ccweb-core包里，包路径为package ccait.ccweb.generator，启动类EntitesGenerator，生成的路径与包名可在application.yml中设置。

## 编写控制器
```java
@RestController
public class ApiController extends BaseController {

    @ResponseBody
    @RequestMapping( value = "login", method = RequestMethod.POST )
    public Mono loginByPassword(@RequestBody UserModel user) {
        try {

            user = super.logoin(user);

            return successAs(user);

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return errorAs(150, e);
        }
    }

}
```

## BaseContoller
BaseContoller规范了ResponseData返回数据的格式，并为用户封装了后端http请求数据获取校验等方法提供给自定义的rest控制器继承使用。

* getLoginUser()【获取当前登录用户】
* getCurrentMaxPrivilegeScope(table)【获取当前用户对表的操作权限】
* getTablename()【获取当前访问的表名】
* md5(text)text【md5加密】
* encrypt(data)【加密数据或查询条件字段值】
* decrypt(data)【解密数据或查询条件字段值】
* base64Encode(text)【base64编码】
* base64Decode(text)【base64解码】
* checkDataPrivilege(table, data)【检查当前用户对数据的访问权限】
* success(data)【成功返回方法】
* error(message)【错误返回方法】
* successAs(data)【异步IO成功返回方法】
* errorAs(message)【异步IO错误返回方法】
* ResponseData【数据响应封装类】

## 事件触发器Tagger
为了方便二次开发可以拦截及响应请求，框架提供了触发器能力，可以针对不同请求事件嵌入自定义的逻辑，示例如下：

```java
@Component
@Scope("prototype")
@Trigger(tablename = "${entity.table.privilege}") //触发器注解,tablename为表名,可选参数
public final class DefaultTrigger {

    /***
     * 新增数据事件
     * @param data （提交的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnInsert
    public void onInsert(Map<String, Object> data, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 更新数据事件
     * @param data （提交的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnUpdate
    public void onUpdate(Map<String, Object> data, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 删除数据事件
     * @param id （要删除的数据ID）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnDelete
    @Order(-55555)
    void onDelete(String id, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 建表事件
     * @param columns （字段内容列表）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnBuildTable
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 列出数据事件，当queryInfo没有查询条件时触发
     * @param queryInfo （分页/分组/排序条件）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnList
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 查询数据事件，queryInfo存在查询条件时触发
     * @param queryInfo （查询/分页/分组/排序条件）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnQuery
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 浏览数据事件，ID查询时触发
     * @param id （要浏览的数据ID）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnView
    public void onView(String id, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 成功返回数据时触发
     * @param responseData （响应的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnSuccess
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 返回错误数据时触发
     * @param ex （Exception异常类）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnError
    public void onError(Exception ex, HttpServletRequest request) {
        //TODO
    }

    /***
     * 响应数据流时触发
     * @param response （响应对象）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnResponse
    void onResponse(HttpServletResponse response, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 下载文件时触发
     * @param request （当前请求）
     * @throws Exception
     */
    @OnDownload
    void onDownload(BaseController.DownloadData data, HttpServletRequest request) throws Exception {
        //TODO
    }

    /***
     * 预览文档时触发
     * @param data （文件对象）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnPreviewDoc
    void onPreviewDoc(BaseController.DownloadData data, HttpServletRequest request) throws Exception {
        //TODO
    }
}
```

## 数据响应说明
### 1. ResponseData
```java
    private int code; //0=成功
    private String message; //code不等于零时返回错误消息
    private T data; //code等于0返回查询的结果
    private PageInfo pageInfo; //分页信息
    private UUID uuid; //该次请求唯一识别码
```
### 2. PageInfo
```java
    //private int pageCount; //总页数（已放弃，前端根据总记录数和每页显示记录数计算）
    //private long totalRecords; //总记录数（已放弃，前端通过count接口获取）
    private int pageIndex; //当前页
    private int pageSize;  //每页显示记录数
```

## 打包说明
目前只支持jar包启动，要使用动态查询功能需要将rxjava-2.1.10.jar、spring-context-5.0.4.RELEASE.jar、entity.queryable-2.0-SNAPSHOT.jar复制到jar包同级路径的libs下，建议使用EntitesGenerator生成实体类。

## 注意
使用动态查询的表在设计阶段需要加上以下字段：
```yaml
  createOn: createOn #数据创建时间
  createBy: createBy #数据创建者
  modifyOn: modifyOn #数据修改时间
  modifyBy: modifyOn #数据修改人
```

## 企业版2.0功能模块介绍
* ccweb-admin [开发中]
* ccweb-office (办公室文档处理功能包，如Word、Excel、PowerPoint、PDF)
* ccweb-socket (websocket消息推送功能包)
* ccweb-auth (用户鉴权功能包，分布式登录需搭载redis)
* ccweb-iot (mqtt消息传输功能包，内置一个定阅器和一个简单的服务)
* ccweb-config (分布式配置中心，可将每个服务的配置存至统一的数据库进行管理)
* ccweb-logs (内置一个自定议的log4j过滤器)
* ccweb-gateway（springCloud+ccweb网关）
