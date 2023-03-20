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
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.entites.SaveDataInfo;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.*;
import entity.query.ColumnInfo;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
@RequestMapping( value = {"api/{datasource}"}, produces = "text/plain;charset=UTF-8" )
public class ApiController extends BaseController {

    /***
     * join query count
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "join/count", method = RequestMethod.POST )
    public ResponseData doJoinQueryCount(@RequestBody QueryInfo queryInfo) {

        try {
            Long result = super.joinQueryCount(queryInfo);

            return success( result );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(113, e);
        }
    }

    /***
     * join query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "join", method = RequestMethod.POST )
    public ResponseData doJoinQuery(@RequestBody QueryInfo queryInfo) {

        try {
            List result = super.joinQuery(queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return success( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(113, e);
        }
    }

    /***
     * create or alter table
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/build/table", method = {RequestMethod.POST, RequestMethod.PUT} )
    public ResponseData doCreateOrAlterTable(@PathVariable String table, @RequestBody List<ColumnInfo> columns) {
        try{

            super.createOrAlterTable(table, columns);

            return success();
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);
            return error(e.getMessage());
        }
    }
    /***
     * create or alter view
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/build/view", method = {RequestMethod.POST, RequestMethod.PUT} )
    public ResponseData doCreateOrAlterView(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try{

            super.createOrAlterView(table, queryInfo);

            return success();
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);
            return error(e.getMessage());
        }
    }

    /***
     * get
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.GET )
    public ResponseData doGet(@PathVariable String table, @PathVariable String id)  {
        try {

            Map data = super.get(table, id);

            return success( data );
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(100, e);
        }
    }

    /***
     * query
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.POST )
    public ResponseData doQuery(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            List result = super.query(table, queryInfo);

            queryInfo.getPageInfo().setPageCount();

            return success( result, queryInfo.getPageInfo() );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(110, e);
        }
    }

    /***
     * query and update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/update", method = RequestMethod.POST )
    public ResponseData doQueryUpdate(@PathVariable String table, @RequestBody QueryInfo queryInfo) {

        try {

            boolean result = super.updateByQuery(table, queryInfo);

            return success( result );
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(110, e);
        }
    }

    /***
     * exist
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/exist", method = RequestMethod.POST )
    public ResponseData doExist(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try {

            Boolean result = super.exist(table, queryInfo);

            return success( result );

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(111, e);
        }
    }

    /***
     * count
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/count", method = RequestMethod.POST )
    public ResponseData doCount(@PathVariable String table, @RequestBody QueryInfo queryInfo) {
        try {

            Long result = super.count(table, queryInfo);
            return success( result );

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(112, e);
        }
    }

    /***
     * insert and select id
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/max/{field}", method = RequestMethod.PUT )
    public ResponseData doInsertAndReturnId(@PathVariable String table, @PathVariable String field, @RequestBody List<Map<String, Object>> postData)
    {
        try {
            List<Object> result = super.insert(table, postData, field);
            if(result.size() == 1) {
                return success(result.get(0));
            }

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(120, e);
        }
    }

    /***
     * insert
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}", method = RequestMethod.PUT )
    public ResponseData doInsert(@PathVariable String table, @RequestBody List<Map<String, Object>> postData)
    {
        try {
            List<Object> result = new ArrayList<>();
            for(int i=0; i < postData.size(); i++) {
                Map data = (Map)postData.get(i);
                result.add(super.insert(table, data));
            }

            if(result.size() == 1) {
                return success(result.get(0));
            }

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(120, e);
        }
    }

    /***
     * update
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.PUT )
    public ResponseData doUpdate(@PathVariable String table, @PathVariable String id, @RequestBody Map<String, Object> postData) {
        try {

            Integer result = super.update(table, id, postData);

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(130, e);
        }
    }

    /***
     * save
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/save", method = RequestMethod.PUT  )
    public ResponseData doSave(@PathVariable String datasource, @PathVariable String table, @RequestBody SaveDataInfo saveDataInfo) {
        try {
            UserModel user = getLoginUser();
            if(user == null) {
                throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
            }

            Boolean isRemoveData = (saveDataInfo.getRemoveIdList()!=null && saveDataInfo.getRemoveIdList().size()>0);
            if(isRemoveData) {
                super.deleteByIdList(table, saveDataInfo.getRemoveIdList());
            }

            if(saveDataInfo.getPostData()==null || saveDataInfo.getPostData().size()<1) {
                return success();
            }

            if(saveDataInfo.getQueryInfo() != null) {
                saveDataInfo.getQueryInfo().setPageInfo(new PageInfo() {{
                    setPageIndex(1);
                    setPageSize(1);
                }});
                List list = super.query(table, queryInfo);
                if(list.size()>0) {
                    List<Integer> result = new ArrayList<>();
                    list.forEach(item-> {
                        try {
                            Map<String, Object> info = JsonUtils.convert(list.get(0), Map.class);
                            if(saveDataInfo.getPostData()!=null && saveDataInfo.getPostData().get(0).size()>0) {
                                queryInfo.setData(saveDataInfo.getPostData().get(0));
                            }
                            if(queryInfo.getData()!=null) {
                                result.add(super.update(table, info.get("id").toString(), queryInfo.getData()));
                            }
                        } catch (Exception e) {
                            getLogger().error(e.getMessage());
                        }
                    });
                    if(result.size()>0) {
                        return success(result);
                    }
                }
            }

            ColumnInfo pkColumn = EntityContext.getPrimaryKey(datasource, table);
            String pk = (pkColumn==null? "": pkColumn.getColumnName());

            List<Object> result = new ArrayList<>();
            for(int i = 0; i < saveDataInfo.getPostData().size(); i++) {
                Map<String, Object> data = saveDataInfo.getPostData().get(i);
                if(!isRemoveData && data.containsKey(pk) && StringUtils.isEmpty(data.get(pk).toString())) {
                    String id = data.get(pk).toString();
                    data.remove(pk);
                    data.put(modifyByField, user.getUserId());
                    data.put(modifyOnField, new Date());
                    result.add(super.update(table, id, data));
                }
                else {
                    data.put(createByField, user.getUserId());
                    data.put(createOnField, new Date());
                    result.add(super.insert(table, data));
                }
            }

            if(result.size() == 1) {
                return success(result.get(0));
            }

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + "ERROR=====> ", e);

            return error(130, e);
        }
    }

    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{id}", method = RequestMethod.DELETE )
    public ResponseData doDelete(@PathVariable String table, @PathVariable String id) {
        try {

            Integer result = super.delete(table, id);

            return success(result);
        }

        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(140, e);
        }
    }


    /***
     * delete
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/delete", method = RequestMethod.POST )
    public ResponseData deleteByIds(@PathVariable String table, @RequestBody List<Object> idList) {

        List result = null;
        try {
            result = super.deleteByIdList(table, idList);
        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(140, e);
        }

        return success(result);
    }

    /***
     * login
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "login", method = RequestMethod.POST )
    public ResponseData loginByPassword(@RequestBody UserModel user) {
        try {

            user = super.login(user);

            return success(user);

        } catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + e, e);

            return error(150, e);
        }
    }

    /***
     * logout
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "logout", method = RequestMethod.GET )
    public ResponseData logouted() {

        super.logout();

        return success();
    }

    /***
     * download
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "download/{table}/{field}/{id}/index/{index}", method = RequestMethod.GET )
    public void downloadedByIndex(@PathVariable String table, @PathVariable String field, @PathVariable String id, @PathVariable Integer index) throws Exception {
        super.download(table, field, id, index);
    }

    /***
     * download
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "download/{table}/{field}/{id}", method = RequestMethod.GET )
    public void downloaded(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {
        super.download(table, field, id, 0);
    }

    /***
     * upload
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/{field}/upload", method = RequestMethod.POST )
    public Map<String, Object> uploaded(@PathVariable String table, @PathVariable String field, @RequestBody Map<String, Object> uploadFiles) throws Exception {
        return super.upload(table, field, uploadFiles);
    }

    /***
     * preview
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "preview/{table}/{field}/{id}", method = RequestMethod.GET )
    public void previewed(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {
        super.preview(table, field, id, 0, 0);
    }

    /***
     * preview
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "preview/{table}/{field}/{id}/index/{index}", method = RequestMethod.GET )
    public void previewedByIndex(@PathVariable String table, @PathVariable String field, @PathVariable String id, @PathVariable Integer index) throws Exception {
        super.preview(table, field, id, index, 0);
    }

    /***
     * preview
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "preview/{table}/{field}/{id}/{page}", method = RequestMethod.GET )
    public void previewedPage(@PathVariable String table, @PathVariable String field, @PathVariable String id, @PathVariable Integer page) throws Exception {
        super.preview(table, field, id, 0, page);
    }

    /***
     * preview
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "preview/{table}/{field}/{id}/{page}/index/{index}", method = RequestMethod.GET )
    public void previewedPageByIndex(@PathVariable String table, @PathVariable String field, @PathVariable String id, @PathVariable Integer index, @PathVariable Integer page) throws Exception {
        super.preview(table, field, id, index, page);
    }

    /***
     * play video
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "play/{table}/{field}/{id}", method = RequestMethod.GET )
    public void playVideo(@PathVariable String table, @PathVariable String field, @PathVariable String id) throws Exception {
        super.playVideo(table, field, id, 0);
    }

    /***
     * play video
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "play/{table}/{field}/{id}/index/{index}", method = RequestMethod.GET )
    public void playVideoByIndex(@PathVariable String table, @PathVariable String field, @PathVariable String id, @PathVariable Integer index) throws Exception {
        super.playVideo(table, field, id, index);
    }

    /***
     * export select data
     * @param table
     * @param queryInfo
     * @return
     * @throws Exception
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/export", method = RequestMethod.POST )
    public void doExport(@PathVariable String table, @RequestBody QueryInfo queryInfo) throws Exception {

        if(queryInfo.getSelectList() == null || queryInfo.getSelectList().size() < 1) {
            throw new IOException("SelectList can not be empty!!!");
        }

        List list = query(table, queryInfo, true);

        export(table, list, queryInfo);
    }

    /***
     * export by join query
     * @param queryInfo
     * @return
     * @throws Exception
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "export/join", method = RequestMethod.POST )
    public void doExport(@RequestBody QueryInfo queryInfo) throws Exception {

        if(queryInfo.getSelectList() == null || queryInfo.getSelectList().size() < 1) {
            throw new IOException("SelectList can not be empty!!!");
        }

        List list = joinQuery(queryInfo, true);
        export(UUID.randomUUID().toString().replace("-", ""), list, queryInfo);
    }

    /***
     * import excel
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "{table}/import", method = {RequestMethod.POST, RequestMethod.PUT} )
    public ResponseData doImportExcel(@PathVariable String table, @RequestBody Map<String, Object> uploadFiles) throws Exception {
        super.importExcel(table, uploadFiles);
        return success();
    }
}
