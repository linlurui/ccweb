
                      c
                     c#
                  /c++)
                  python
                  #VB^
                 ccait'
                 java   ccait    ccweb  En       ti #SPRING  Babel^_^~
                node   CC" '''  CC" ''' ty  Que  ry MVC      TS   `VUE
              delphi  CCC      CCC      ab  /le go  Docker   Electron@,
        javascript,   CC       CC       pg/   pay   AOP      ES      66,
     2019@copyright     v1.0.0   HTTP:  //    ///   CCAIT.CN FREAMEWORK

    =========================================================================
    :: CCWEB :: (v1.0.0-SNAPSHOT)  Author: linlurui 2019@copyright

CCWEB是基于springboot设计的CQRS敏捷web api开发框架，项目由深圳市春蚕智能信息技术有限公司启动于2018底，2019年发布第一个版本，经过多次迭代现已升级到2.0，CCWEB提倡动态向前端提供基础数据，由前端根据基础数据组装业务来提高开发效率;内置用户管理、权限设置 等安全模块，启动服务后无需添加任何后端代码前端便可以通过默认接口直接访问到自己在数据库建的表和查询视图；底层orm采用entityQueryable访问数据，支持SpringCloud微服务扩展；支持elasticSerach搜索引擎；在横向扩展方面ccweb兼容了多种数据库系统，包括主流的mysql、sqlserver、oralce、MariaDB、PostgreSQL、db2、sybase等，有易于数据集成及高度扩展的能力，可以让数据自由地穿梭于各种数据存储系统之间：项目包含ccweb-core、ccweb-api、ccweb-start、ccweb-socket、ccweb-auth、ccweb-config、ccweb-gateway、ccweb-logs、ccweb-office、ccweb-webagent、ccweb-iot，整个2.0版本除ccweb-core之外，其余均选择了开源。
</p>
    <img align="right" src="https://gitee.com/ccait/dapperq/raw/master/pay5.jpg" alt="捐赠给作者"  width="200">
    <p align="right">
        <em>捐赠给作者</em>
    </p>
</p>

# ccweb-start
ccweb-start是ccweb的启动包，其中包含了springcloud的微服务组件与springboos2.0

## 运行环境
* jdk1.8

## release文件结构
* cc-service.jar 【ccweb默认服务启动包】
* application.yml 【应用程序主配置文件】
* Dockerfile 【docker容器部署脚本】
* install.sh【linux系统依赖包安装脚本，需要先安装JDK1.8并且使用JDK自带的JRE，windows下需要安装cygwin来运行该脚本】
* kill.sh【jar包启动时的kill掉进程的脚本】
* run.sh【jar包启动脚本】
* ccait.db【sqlite默认数据库】
* lang.yml【语言配置文件，可配置多语言】
* libs/entity.queryable-2.0-SNAPSHOT.jar【动态查询依赖包 [项目地址](https://github.com/linlurui/entityQueryable)】
* libs/rxjava-2.1.10.jar【查询结果异步IO依赖包】
* libs/spring-context-5.2.5.RELEASE.jar【动态实体注入依赖包)】
* libs/spring-context-support-5.2.5.RELEASE.jar【动态实体注入依赖包)】
* libs/easyexcel-2.1.3.jar【excel数据导入依赖包】


## 开发包说明
* ccweb-start (一个集成了ccweb-api的一键启动服务包)
* ccweb-api (rest接口基础服务包)
* ccweb-office (办公室文档处理功能包，如Word、Excel、PowerPoint、PDF)
* ccweb-socket (websocket消息推送功能包)
* ccweb-auth (用户鉴权功能包，分布式登录需搭载redis)
* ccweb-iot (mqtt消息传输功能包，内置一个定阅器和一个简单的服务)
* ccweb-config (分布式配置中心，可将每个服务的配置存至统一的数据库进行管理)
* ccweb-logs (一个基于kafka的分布式日志系统，如用作消息队列需要引用该项目做二次开发消费端)
* ccweb-repo (数据仓库模式操作类，通在用于二次开发时简化数据库操作)
* ccweb-gateway（zuul网关）
* ccweb-webagent（第三方平台接口转发功能包，可通过application.yml配置什么样的请求需要转发到第三方平台接口处理）

## 服务启动命令
***java -jar cc-service.jar***

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


### 8. 联表查询总数
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
* 注：上传多文件字段可在URL后追加：/index/{index}指定要下载第几个文件，{index}为文件下标


### 13. 文件预览（支持预览图片、视频、PPT）
* URL：/api/{datasource}/preview/{table}/{field}/{id}/{page} 
* 请求方式：GET
* URL参数：{table}为数据库表名称，{field}为字段名，{id}为主键，{page}为可选入参，可指定页码
* POST参数：无
* 注：上传多文件字段可在URL后追加：/index/{index}指定要预览第几个文件，{index}为文件下标

### 14. 上传文件
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


### 15. 批量更新
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



### 17. 导出Excel
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


### 18. 联表导出Excel
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

### 19. 新增并返回ID(返回指定字段的最大值)
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
* 注：上传多文件字段可在URL后追加：/index/{index}指定要播放第几个文件，{index}为文件下标

### 21. 导入Excel
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

### 22. 获取登录用户
* URL：/api/{datasource}/session/user 
* 请求方式：GET

### 23. 消息推送
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

### 24. 搜索引擎查询（需要配置Elasticsearch服务器）
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

### 25. 获取数据流
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


### 26. 导入为PDF（需要引用ccweb-office）
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


## 系统用户/权限表结构说明
用户权限相关表在服务启动时会自动创建，目的在于使用系统服务控制数据库表的访问权限，用户组是扁平结构的，需要更复杂的权限控制功能建议通过二次开发实现。
* 用户表 (user, 主键userId, username[用户名], password[密码], type[用户类型], status[状态])
* 用户组 (group, 主键groupId, groupName[组名], description[描述])
* 角色表 (role, 主键roleId, roleName[角色名])
* 用户/组/角色关联关系表 (userGroupRole, 主键userGroupRoleId, 外键关联userId、groupId、roleId、userPath[用户层级路径,每个组下创建的用户ID组成]、createBy[创建人,数据权限检查的依据]、createOn[创建时间]、modifyBy[修改人]、modifyOn[修改时间])
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
            <url>https://gitee.com/ccait/ccweb/raw/2.0</url>
            <releases>
                <updatePolicy>always</updatePolicy>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <updatePolicy>always</updatePolicy>
                <enabled>true</enabled>
                <checksumPolicy>fail</checksumPolicy>
            </snapshots>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>ccait.cn</groupId>
            <artifactId>ccweb-core</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>ccait.cn</groupId>
            <artifactId>ccweb-api</artifactId>
            <version>1.0.0-SNAPSHOT</version>
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
            getLogger().error(LOG_PRE_SUFFIX + "ERROR=====> ", e);

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
@Scope("request")
@Trigger(tablename = "privilege") //触发器注解,tablename为表名,可选参数
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

## ccweb-repo数据仓储类
### 引入依赖包
```xml
<dependency>
    <groupId>ccait.cn</groupId>
    <artifactId>ccweb-repo</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```
### 创建关系映射实体类
```java
@Entity(table = "user")
public class UserModel extends Queryable<UserModel> {

    @AutoIncrement
    @Fieldname("id")
    private Integer userId;

    @PrimaryKey
    @Fieldname("username")
    private String username;

    @Fieldname("password")
    private String password;
}
```

### 操作数据库
```java
@Autowired
CCRepository repo;

void Test() {
    try {
        /** 开启事务 **/
        repo.openSession();
        
        repo.get(User.class)
        /** 新增 **/
        repo.get(new UserModel(){{
        setUsername("admin");
        setPassword("123456");
        }}).insert();

        /** 查询（支持联表、聚合函数、子查询，因篇幅关系不作更多示例） **/
        UserModel user = repo.get(UserModel.class).where("username='admin'").first();
        List<UserModel> userList = repo.get(UserModel.class).where("1=1").query();

        /** 修改 **/
        user.setUsername("admin");
        user.setPassword("654321");
        repo.get(user).where("username=#{username}").update("password=#{password}");

        /** 删除 **/
        repo.get(user).where("username=#{username}").delete();

        /** 提交事务 **/
        repo.commit();
    } catch(Exexption e) {
        /** 回滚事务 **/
        repo.rollback();    
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
目前只支持jar包启动，要使用动态查询功能需要将rxjava-2.1.10.jar、spring-context-5.2.5.RELEASE.jar、entity.queryable-2.0-SNAPSHOT.jar复制到jar包同级路径的libs下，要使用数据导入还需要easyexcel-2.1.3.jar，建议使用EntitesGenerator生成实体类。

## 注意
使用动态查询的表在设计阶段需要加上以下字段供权限模块检查数据权限：
```yaml
  createOn: createOn #数据创建时间
  createBy: createBy #数据创建者
  modifyOn: modifyOn #数据修改时间
  modifyBy: modifyOn #数据修改人
```

## 数据源配置
```yaml
entity: 
  datasource:
    activated: mydb #可通过url的{datasource}参数访问到该数据库，没有在此设置的environment不能通过url访问到
    environments:
      mydb:
        default: true
        driver: com.mysql.cj.jdbc.Driver
        url: jdbc:localhost:3306/dbname?characterEncoding=utf8&useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true
        username: root
        password: 123456
```

## 配置中心数据表
引用ccweb-config包后框架会在启动时自动从指定的配置表读取配置信息，如表不存在会自动创建
```yaml
ccweb:
    app-config:
        table: appConfig #表名

```

## 实体类生成配置
```yaml
ccweb:
    package: ccait.ccweb.entites #实体类生成包路径
    suffix: Entity #实体类生成类名后辍

```

## 安全配置选项
```yaml
ccweb:
  security: #安全配置选项
    encrypt:
      MD5: #MD5加密配置
        fields: user.password #需要使用MD5加密的字段，逗号分隔
        publicKey: 123456 #MD5加密公钥
      AES: #AES加密配置
        publicKey: 123456 #AES加密公钥
    admin: #超级管理员账号密码(必填)
      username: admin
      password: admin
```

## 鉴权方式配置
```yaml
ccweb:
  auth:
    header: Authorization #鉴权字串请求头
    user:
      jwt: 
        millis: 43200000 #时效
        enable: true #是否开启jwt鉴权
      aes:
        enable: true #是否开启aes加密鉴权

```

## 参数校验配置
```yaml
ccweb:
  validation: #数据提交字段校验器，以下email、mobile为字段名，也可以精确校验:[表名].[字段名]
    email:
      match: ^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$
      message: 请输入正确的电子邮箱格式
    mobile:
      match: ^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(17[0,3,5-8])|(18[0-9])|166|198|199|(147))\\d{8}$
      message: 请输入正确的手机号码
```

## 输出格式配置
```yaml
ccweb:
  formatter: #查询结果输出格式，createOn、modifyOn，也可以精确格式化:[表名].[字段名]
    createOn: yyyy-MM-dd HH:mm
    modifyOn: yyyy-MM-dd HH:mm
    start_time: yyyy-MM-dd HH:mm
    end_time: yyyy-MM-dd HH:mm
    startTime: yyyy-MM-dd HH:mm
    endTime: yyyy-MM-dd HH:mm
    evaluateTime: yyyy-MM-dd HH:mm
    appraise_time: yyyy-MM-dd HH:mm

```

## 系统保留表与保留字段设置
```yaml
ccweb:
  table:
    reservedField: #系统保留字段(必填)
      userPath: userPath #创建者所属路径，体现父子关系，用于like查询
      createOn: createOn #数据创建时间
      createBy: createBy #数据创建者
      modifyOn: modifyOn #数据修改时间
      groupId: groupId  #群组ID
      roleId: roleId #角色ID
      aclId: aclId #访问控制ID
      privilegeId: privilegeId #权限ID
      userGroupRoleId: userGroupRoleId #用户组角色关联ID
      userId: userId #用户ID(整型，会被计算到userPath中)
      id: id #系统默认id字段名

    #系统保留表(必填)
    user: user #用户表
    group: group #分组表
    role: role #角色表
    acl: acl #访问控制表
    privilege: privilege #操作权限表
    userGroupRole: userGroupRole #用户/组/角色关联关系

    display: #需要隐藏的字段，设为hidden的字段不会在查询结果中显示
      user:
        password: hidden

```

## 上传设置
```yaml
ccweb:
  upload: #上传设置
    basePath: /Users/administrator/Desktop/upload
    mimeTypes: gif, jpg, png, jpeg, pdf, doc, docx, xls, xlsx, ppt, pptx, mp3, mp4, webm, wav, ogg
    multiple: true
    maxSize: 20 #上传文件最大尺寸，单位: MB
```
### 可指定具体哪一个字段数据的上传限制，如：
```yaml
ccweb: 
  upload: #上传设置
    multiple: true
    mydb: #库名
      user: #表名
        card: #字段名
          maxSize: 5 #上传文件最大尺寸，单位: MB
          mimeType: gif, jpg, png, jpeg #允许上传的文件格式
          path: /home/upload/card
        photo: #字段名
          maxSize: 5 #上传文件最大尺寸，单位: MB
          mimeType: gif, jpg, png, jpeg #允许上传的文件格式
          path: /home/upload/photo #上传路径的绝对路径

```

## 下载设置
```yaml
ccweb:
  download: #下载设置
    thumb:  #预览的缩略图
      fixedWidth: 200 #固定宽度
      scalRatio: 50 #缩放比率(百分比)
      watermark: watermark.png #水印

```

## 查询默认分页配置
```yaml
ccweb:
    page: #分页配置选项
        maxSize: 50 #分页最大记录数默认值

```

## 默认值
```yaml
ccweb: 
  defaultValue: #指定表字段为null时赋于默认值，UUID_RANDOM=UUID.randomUUID()，DATE_NOW=Datetime.now()
    userGroupRole.userGroupRoleId: UUID_RANDOM
    role.roleId: UUID_RANDOM
    group.groupId: UUID_RANDOM
    privilege.privilegeId: UUID_RANDOM
    acl.aclId: UUID_RANDOM
```

## 白名单、黑名单设置
```yaml
ccweb:
  ip:
    whiteList:  #IP白名单, 逗号分隔
    blackList:  #IP黑名单, 逗号分隔
```

## elasticSearch搜索引擎配置
```yaml
  elasticSearch:
    enable: false
    timeout: 10000
    cluster-name: 127.0.0.1:9200 #逗号分隔
    indexs: #索引设置
      user_username: user.username #格式: key=indexname, value=[{tablename}].{fieldname}
    highlight: #高亮设置
      user: #表名
        fields: username #字段名，逗号分隔
        fragmentSize: 200
        preTags: <em>
        postTags: </em>
        script: painless
        painless:
          password: "doc['password']=\"*****\"; return doc['password'];"
```

## redis配置
```yaml
ccweb:
  redis:
    enable: false
    cluster: false
    # nodes: 127.0.0.1:8667
    host: 127.0.0.1
    port: 6379
    timeout: 0
    
```

## 字典配置
redis开启时,字典数据会被缓存到redis
```yaml
ccweb:
  dict:
    default: 开发
    map:
      pro: 生产
      dev: 开发
```

## 第三方平台代理配置
```yaml
ccweb:
  agent:
    baidu:  #平台标识${platform}
      id: 11010102202001 #第三方平台密钥
      name: 百度 #第三方平台名称
      description: 第三方通道 #描述
      key: 26F90B0E6FFDAAAAAA2BEA6D35C2 #平台的签名密钥
      domain: https://www.baidu.com #平台域名
      sign: md5(${postString}${platform.key}) #签名方式,格式string|md5|aes|mac|base64(${变量}或"字符串")
      charset: UTF-8
      ensureTable: true #是否自动建表
      required: #必填项，第三方平台的必填参数，可支持变量
        agentID: ${platform.id}
        sign: ${platform.sign}
      saveRequired: false #是否保存第三方平台必填项到数据库，默认false
      token: 123123
      headers:
        Authorization: ${token}
      request:
        user: #表名
          insert: #动作,支持动作有insert、delete、update、query、get、callback
            method: post #提交参数的方法
            mode: url #参数拼接方式, url或json, 默认json
            path: /open/api/member/register #路径
            save: both #需保存到表的数据，result=第三方平台返的响应结果，post=提交的参数，both=两种数据都存，同名的会被result覆盖, dont=不保存
            data: returnData #要存到数据库的json数据集位置，不填默认为返回的整个json结果集
            unique: agentID #去重字段(联合主键)，逗号分隔
            roleId: 2 #默认创建角色
            success:
              success: true
            fixedColumns: #保存到数据库的固定列，可以是变量
              password: ${response.userID}
              username: ${post.password}
        order:
          callback:
            primaryKey: outOrderNo #主键
            checkDomain: true #是否检查域名
            data: returnData
            response: success #返回
            required: #必填项
              sign: ${platform.sign}

```

## Websocket配置
```yaml
websocket:
  server: 127.0.0.1 #主机
  enable: true #是否开启
  protocol: ws #协议，ws或wss
  port: 8080 #端口
```
