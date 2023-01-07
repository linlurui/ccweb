package ccait.trigger;

import ccait.ccweb.annotation.OnInsert;
import ccait.ccweb.annotation.OnSave;
import ccait.ccweb.annotation.OnUpdate;
import ccait.ccweb.annotation.Trigger;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.model.UserModel;
import jdk.nashorn.internal.objects.annotations.Constructor;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;

@Component
@Scope("prototype")
@Trigger
public class CommonTrigger {
    private static final Logger log = LoggerFactory.getLogger( CommonTrigger.class );

    @Constructor
    private void init() {
        log.info("init trigger  =================================> " + this.getClass().getSimpleName());
    }

    @OnInsert
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if(user == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }
        for(Map<String, Object> item : list) {
            for(Map.Entry<String, Object> entry : item.entrySet()) {
                if("createOn".equals(entry.getKey()) || "modifyOn".equals(entry.getKey())) {
                    entry.setValue(new Date());
                }

                if("createBy".equals(entry.getKey()) || "modifyBy".equals(entry.getKey())) {
                    entry.setValue(user.getUserId());
                }
            }
        }
    }

    @OnUpdate
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if(user == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }
        for(Map.Entry<String, Object> entry : queryInfo.getData().entrySet()) {
            if("createOn".equals(entry.getKey()) || "modifyOn".equals(entry.getKey())) {
                entry.setValue(new Date());
            }

            if("createBy".equals(entry.getKey()) || "modifyBy".equals(entry.getKey())) {
                entry.setValue(user.getUserId());
            }
        }
    }

    @OnSave
    public void onSave(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        this.onUpdate(queryInfo, request);
    }
}
