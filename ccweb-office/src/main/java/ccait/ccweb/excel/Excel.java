/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.excel;


import com.itextpdf.text.DocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Excel {

    protected Workbook wb;
    protected Sheet sheet;

    public Excel(InputStream is) throws IOException, InvalidFormatException {
        this.wb = WorkbookFactory.create(is);
        this.sheet = wb.getSheetAt(wb.getActiveSheetIndex());
    }

    public Sheet getSheet() {
        return sheet;
    }

    public Workbook getWorkbook(){
        return wb;
    }

    public static byte[] toPdfBytes(byte[] bytes) throws IOException, InvalidFormatException, DocumentException {

        List<ExcelObject> objects = new ArrayList<ExcelObject>();

        InputStream is = new ByteArrayInputStream(bytes);
        objects.add(new ExcelObject("A_" + UUID.randomUUID().toString().replace("_", ""), is));

        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        Excel2Pdf pdf = new Excel2Pdf(objects , fos);
        pdf.convert();
        return fos.toByteArray();
    }
}
