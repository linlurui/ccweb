package ccait.ccweb;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Common {
    public static File mkdirs(String path) {
        File file = new File(path);
        if(!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static String ensurePath(String root) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = sdf.format(new Date());
        String[] dataArray = dateString.split("\\-");
        String path = String.format("/%s/%s", dataArray[0], dataArray[1]);
        File file = mkdirs(root + path);

        int count = 0;
        File[] files = file.listFiles();
        if(files!=null && files.length > 0) {
            count = files.length;
        }

        if(count > 0) {
            files = new File(root + String.format("%s/%s", path, String.format("%02d", count - 1))).listFiles();
            if(files!=null && files.length < 100) {
                path = String.format("%s/%s", path, String.format("%02d", count - 1));
            }

            else {
                path = String.format("%s/%s", path, String.format("%02d", count));
            }
        }

        else {
            path = String.format("%s/%s", path, "00");
        }

        path = String.format("%s/%s", path, UUID.randomUUID().toString().replace("-", ""));

        mkdirs(root + path);

        return path + "/";
    }

    public static String getCurrentOperatingSystem(){
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("---------当前操作系统是-----------" + os);
        return os;
    }

    public static String getTempPath() {
        String path = "/temp/word2html";
        if(!"linux".equals(Common.getCurrentOperatingSystem())) {
            path = System.getProperty("user.dir") + path;
        }

        Common.mkdirs(path);

        return path + Common.ensurePath(path);
    }
}
