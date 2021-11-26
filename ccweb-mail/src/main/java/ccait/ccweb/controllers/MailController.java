/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.controllers;


import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.entites.*;
import ccait.ccweb.enums.Algorithm;
import ccait.ccweb.enums.ReceivesType;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.MailUtils;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping( value = {"api/{datasource}/mail"} )
public class MailController extends BaseController {

    @Value("${ccweb.table.user:user}")
    private String userTable;

    @Value("${ccweb.table.reservedField.userId:userId}")
    private String userIdField;

    @Value("${ccweb.mail.receives.table:}")
    private String receivesTable;

    @Value("${ccweb.mail.receives.field:}")
    private String receivesField;

    @PostConstruct
    private void init() {
        userTable = ApplicationConfig.getInstance().get("${ccweb.table.user}", userTable);
        userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField);
        receivesTable = ApplicationConfig.getInstance().get("${ccweb.mail.receives.table}", receivesTable);
        receivesField = ApplicationConfig.getInstance().get("${ccweb.mail.receives.field}", receivesField);
    }

    /***
     * send email
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{userId}", method = RequestMethod.POST )
    public ResponseData sendTo(@PathVariable UserModel user, @RequestBody SMTPMessage message) throws Exception {
        if(!hasPrivilege(user)) {
            return error(LangConfig.getInstance().get("has_not_privilege"));
        }

        List<String> receives = getReceives(new ArrayList<Integer>(){{add(user.getUserId());}});

        String receiveString = StringUtils.join(";", receives);

        MailUtils.send(receiveString, message.getSubject(), message.getContent());

        return success();
    }

    /***
     * send email for group
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "send/{type}", method = RequestMethod.POST )
    public ResponseData sendForGroup(@PathVariable ReceivesType receivesType, @RequestBody SMTPMessage message) throws Exception {
        if(getLoginUser() == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }
        List<String> receives = null;
        switch (receivesType) {
            case ALL:
                receives = getReceives(getUserIds());
                break;
            case GROUP:
                receives = getReceives(getUserIdsByGroup(message.getGroupIds()));
                break;
            case ROLE:
                receives = getReceives(getUserIdsByRole(message.getRoleIds()));
                break;
            default:
                if(message.getUserIds() != null && message.getUserIds().size() > 0) {
                    receives = getReceives(message.getUserIds());
                }
                else {
                    return error(LangConfig.getInstance().get("has_not_privilege"));
                }
                break;
        }

        if(receives.size()<1) {
            return success();
        }

        String receiveString = StringUtils.join(";", receives);

        MailUtils.send(receiveString, message.getSubject(), message.getContent());

        return success();
    }

    private List<Integer> getUserIdsByRole(final List<Integer> roleIds) throws IOException {
        if(getLoginUser() == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        List<UserGroupRoleModel> tinyUserGroupRole = getLoginUser().getUserGroupRoleModels().stream().filter(a -> roleIds.contains(a.getRoleId())).collect(Collectors.toList());
        return tinyUserGroupRole.stream().map(a->a.getUserId()).distinct().collect(Collectors.toList());
    }

    private List<Integer> getUserIdsByGroup(List<Integer> groupIds) throws Exception {
        if(getLoginUser() == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        List<UserGroupRoleModel> tinyUserGroupRole = getLoginUser().getUserGroupRoleModels().stream().filter(a -> groupIds.contains(a.getGroupId())).collect(Collectors.toList());
        return tinyUserGroupRole.stream().map(a->a.getUserId()).distinct().collect(Collectors.toList());
    }

    private List<String> getReceives(List<Integer> userids) throws SQLException {

        List<String> receives = new ArrayList<>();
        if(userids == null || userids.size()<0) {
            return receives;
        }

        QueryInfo queryInfo = new QueryInfo();
        queryInfo.setConditionList(new ArrayList<ConditionInfo>(){{
            add(new ConditionInfo(){{ setName(userIdField); setAlgorithm(Algorithm.IN); setValue(userids); }});
        }});

        queryInfo.setSelectList(new ArrayList<SelectInfo>(){{ add(new SelectInfo() {{ setField(receivesField);}}); }});

        Queryable query = (Queryable) EntityContext.getEntity(receivesTable, queryInfo);

        receives = query.where(String.format("%s=#{%s}", userIdField, userIdField)).select(receivesField).query(String.class);

        return receives;
    }

    private boolean hasPrivilege(UserModel user) throws Exception {
        if(getLoginUser() == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        if(user.getUserId() == getLoginUser().getUserId()) {
            return true;
        }

        List<Integer> useridList = getUserIds();
        switch(getCurrentMaxPrivilegeScope(userTable)) {
            case ALL:
            case GROUP:
            case CHILD:
            case PARENT_AND_CHILD:
                if(useridList.contains(user.getUserId())) {
                    return true;
                }
                break;
        }

        return false;
    }

    private List<Integer> getUserIds() throws Exception {
        if(getLoginUser() == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        List<Integer> useridList = ApplicationContext
                .getUserIdByAllGroups(request, getLoginUser())
                .stream().collect(Collectors.toList());

        return useridList;
    }
}
