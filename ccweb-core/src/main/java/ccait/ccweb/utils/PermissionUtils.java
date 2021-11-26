/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.utils;

import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.enums.PrivilegeScope;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

import static ccait.ccweb.utils.StaticVars.*;

public class PermissionUtils {

    public static final Logger log = LoggerFactory.getLogger( PermissionUtils.class );
    public static final Pattern tablePattern = Pattern.compile("^/(api)(/[^/]+){1,2}/build/table$", Pattern.CASE_INSENSITIVE);
    public static final Pattern viewPattern = Pattern.compile("^/(api)(/[^/]+){1,2}/build/view$", Pattern.CASE_INSENSITIVE);
    public static final Pattern uploadPattern = Pattern.compile("^/(api)(/[^/]+){1,3}/upload$", Pattern.CASE_INSENSITIVE);
    public static final Pattern importPattern = Pattern.compile("^/(api)(/[^/]+){1,2}/import$", Pattern.CASE_INSENSITIVE);
    public static final Pattern exportPattern = Pattern.compile("^/(api)(/[^/]+){1,2}/export", Pattern.CASE_INSENSITIVE);
    public static final Pattern updatePattern = Pattern.compile("^/(api)(/[^/]+){1,2}/update$", Pattern.CASE_INSENSITIVE);
    public static final Pattern deletePattern = Pattern.compile("^/(api)(/[^/]+){1,2}/delete$", Pattern.CASE_INSENSITIVE);

    public static boolean allowIp(HttpServletRequest request, String whiteListText, String blackListText) {

        log.info("whiteList: " + whiteListText);
        log.info("blackList: " + blackListText);

        List<String> whiteList = StringUtils.splitString2List(whiteListText, ",");
        List<String> blackList = StringUtils.splitString2List(blackListText, ",");

        String accessIP = NetworkUtils.getClientIp(request);
        log.info("access ip =====>>> " + accessIP);
        if(StringUtils.isEmpty(accessIP)) {
            return false;
        }

        if(whiteList.size() > 0) {
            if (whiteList.contains(accessIP)) {
                return true;
            }

            else {
                return false;
            }
        }

        if(blackList.size() > 0) {
            if (blackList.contains(accessIP)) {
                return false;
            }
        }

        return true;
    }

    public static class InitLocalMap {
        private boolean myResult;
        private HttpServletResponse response;
        private Map<String, String> attrs;
        private String currentTable;

        public InitLocalMap(HttpServletResponse response, Map<String, String> attrs) {
            this.response = response;
            this.attrs = attrs;
        }

        public boolean is() {
            return myResult;
        }

        public String getCurrentTable() {
            return currentTable;
        }

        private static String md5(String text, String encoding) throws UnsupportedEncodingException, NoSuchAlgorithmException {

            if(StringUtils.isEmpty(text)) {
                return "";
            }

            String key = "ccait" + text;

            MessageDigest md5 = MessageDigest.getInstance("MD5");

            md5.update((key).getBytes(encoding));
            byte b[] = md5.digest();

            int i;
            StringBuffer buf = new StringBuffer("");

            for(int offset=0; offset<b.length; offset++){
                i = b[offset];
                if(i<0){
                    i+=256;
                }
                if(i<16){
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }

            String result = buf.toString();

            return result;
        }

        private static String genPrivateKey(String publicKey) throws UnsupportedEncodingException, NoSuchAlgorithmException {
            publicKey = md5(publicKey, encoding).substring(8, 24);
            String privateKey = "";
            for(int i=0; i<publicKey.length(); i++) {
                int code = (int)publicKey.charAt(i);
                code = code % 26 + 65;
                privateKey+= (char)code;
            }

            return privateKey;
        }

        private static byte[] hexStringToBytes(String hexString) {
            if (hexString == null || hexString.equals("")) {
                return null;
            }
            hexString = hexString.toUpperCase();
            int length = hexString.length() / 2;
            char[] hexChars = hexString.toCharArray();
            byte[] d = new byte[length];
            for (int i = 0; i < length; i++) {
                int pos = i * 2;
                d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
            }
            return d;
        }

        private static byte charToByte(char c) {
            return (byte) "0123456789ABCDEF".indexOf(c);
        }

        public static String bytesToHexString(byte[] src){
            StringBuilder stringBuilder = new StringBuilder("");
            if (src == null || src.length <= 0) {
                return null;
            }
            for (int i = 0; i < src.length; i++) {
                int v = src[i] & 0xFF;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
            return stringBuilder.toString();
        }

        private static String decryptByAES(String text, String publicKey){
            try {
                text = text.toLowerCase();
                String privateKey = genPrivateKey(publicKey);
                byte[] raw = privateKey.getBytes(encoding);
                SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
                byte[] encrypted1 = hexStringToBytes(text);
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original, encoding);
                return originalString;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

        private final static String encoding = "UTF-8";
        private final static String ivParameter = "0123456789abcdef";

        private static String encryptByAES(String text, String publicKey){
            String result = "";

            try {
                String privateKey = genPrivateKey(publicKey);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] raw = privateKey.getBytes(encoding);
                SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
                IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes(encoding));// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
                byte[] encrypted = cipher.doFinal(text.getBytes(encoding));
                result = bytesToHexString(encrypted);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result.toUpperCase();
        }

        public InitLocalMap invoke() throws IOException {
            String datasource = null;
            currentTable = null;

            if(attrs != null) {
                datasource = attrs.get("datasource");
                if(!"join".equalsIgnoreCase(attrs.get("table"))) {
                    currentTable = attrs.get("table");
                }
            }

            if(StringUtils.isNotEmpty(datasource)){
                final String ds = datasource;
                List<String> datasourceList = StringUtils.splitString2List(ApplicationConfig.getInstance().get("${entity.datasource.activated}", ""), ",");
                Optional<String> opt = datasourceList.stream()
                        .filter(a -> a.toLowerCase().equals(ds.toLowerCase())).findAny();
                if(opt == null || !opt.isPresent()) {
                    if(!response.isCommitted()) {
                        response.sendError(HttpStatus.NOT_FOUND.value(), LangConfig.getInstance().get("can_not_access_this_database"));
                    }
                    myResult = true;
                    return this;
                }
            }

            ApplicationContext.getThreadLocalMap().put(CURRENT_DATASOURCE, datasource);
            if(StringUtils.isNotEmpty(currentTable)) {
                ApplicationContext.getThreadLocalMap().put(CURRENT_TABLE, currentTable);
                if(ApplicationContext.getThreadLocalMap().get(CURRENT_MAX_PRIVILEGE_SCOPE + currentTable)==null) {
                    ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + currentTable, PrivilegeScope.SELF);
                }
            }
            myResult = false;
            return this;
        }
    }
}
