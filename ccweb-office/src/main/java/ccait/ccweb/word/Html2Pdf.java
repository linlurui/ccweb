/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.word;

import ccait.ccweb.Common;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfName;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

public class Html2Pdf {


    /***
     * HTML转PDF
     * @param html
     * @return
     * @throws Exception
     */
    public static byte[] convertHtmlToPdf(String html)
            throws Exception {
        html = DocType.ENTITY + html.replaceAll("<([Mm][eE][tT][aA])([^>]+)/?", "<$1 $2 /")
                .replaceAll("<[bB][rR]\\s*>", "<br/>")
                .replaceAll("<([iI][mM][gG])([^>]+)/?", "<$1 $2 /");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        ITextRenderer textRenderer = new ITextRenderer();
        ITextFontResolver fontResolver = textRenderer.getFontResolver();

        String agreementBody = html;
        agreementBody = agreementBody.replace("&nbsp;", " ");
        agreementBody = agreementBody.replace("&ndash;", "–");
        agreementBody = agreementBody.replace("&mdash;", "—");
        agreementBody = agreementBody.replace("&lsquo;", "‘"); // left single quotation mark
        agreementBody = agreementBody.replace("&rsquo;", "’"); // right single quotation mark
        agreementBody = agreementBody.replace("&sbquo;", "‚"); // single low-9 quotation mark
        agreementBody = agreementBody.replace("&ldquo;", "“"); // left double quotation mark
        agreementBody = agreementBody.replace("&rdquo;", "”"); // right double quotation mark
        agreementBody = agreementBody.replace("&bdquo;", "„"); // double low-9 quotation mark
        agreementBody = agreementBody.replace("&prime;", "′"); // minutes
        agreementBody = agreementBody.replace("&Prime;", "″"); // seconds
        agreementBody = agreementBody.replace("&lsaquo;", "‹"); // single left angle quotation
        agreementBody = agreementBody.replace("&rsaquo;", "›"); // single right angle quotation
        agreementBody = agreementBody.replace("&oline;", "‾"); // overline

        if("linux".equals(Common.getCurrentOperatingSystem())){
            fontResolver.addFont("/usr/share/fonts/chiness/simsun.ttc", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }else{
            fontResolver.addFont("c:\\Windows\\Fonts\\simsun.ttc", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            fontResolver.addFont("c:\\Windows\\Fonts\\arial.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
        }
        textRenderer.setDocumentFromString(agreementBody, null);
        textRenderer.layout();
        textRenderer.createPDF(os);
        os.flush();
        os.close();

        return os.toByteArray();
    }
}
