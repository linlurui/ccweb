
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



CCWEB是基于springboot设计的CQRS敏捷web api开发框架，项目由深圳市春蚕智能信息技术有限公司启动于2018底，2019年发布第一个版本，经过多次迭代现已升级到2.0，由于发展需要现已将2.0开源，原1.0版本将继续保留供学习交流但将停止维护，更多功能及更详细的文档请看——>[2.0](https://github.com/linlurui/ccweb2)
</p>
    <img align="right" src="https://gitee.com/ccait/dapperq/raw/master/pay5.jpg" alt="捐赠给作者"  width="200">
    <p align="right">
        <em>捐赠给作者</em>
    </p>
</p>

# ccweb-start
ccweb-start是ccweb-api的启动包，其中包含了springcloud的微服务组件与springboos2.0

## 运行环境
* jdk1.8

## 文件结构
* ccweb-start-1.0.0-SNAPSHOT.jar 【ccweb默认服务启动包】
* application.yml 【应用程序主配置文件】
* db-config.xml 【数据库连接配置文件，2.0版本开始不建议使用，数据配置可直接配在application.yml中】
* entity.queryable-2.0-SNAPSHOT.jar【动态查询依赖包】
* rxjava-2.1.10.jar【查询结果异步IO依赖包】
* spring-context-5.0.4.RELEASE.jar【动态实体注入依赖包】
* install.sh【linux系统依赖包安装脚本，需要先安装JDK1.8并且使用JDK自带的JRE，windows下需要安装cygwin来运行该脚本)】
* log4j2.xml 【可选，log4j2日志配置文件，记录ccweb服务异常信息】

## 服务启动命令
***java -jar ccweb-start-1.0.0-SNAPSHOT.jar***

## 接口说明
ccweb-start内置了默认的api接口可以让前端直接通过表名操作数据，需要限制访问的可以设置系统默认创建的用户权限表进行控制，接口的请求类型同时支持json和表单提交，表单中存在文件上传的会自动上传到表的字段中，字段类型必须为blob。

### 接口、触发器、权限等基本的使用方式在2.0被保留，请参考[2.0](https://github.com/linlurui/ccweb2)文档