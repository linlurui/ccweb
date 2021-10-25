/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.ppt;

import org.apache.poi.hslf.usermodel.*;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PPTTool {

    public static void drawPPT(String extesion, Graphics2D graphics, Slide slide) {
        switch (extesion.toLowerCase()) {
            case "pptx":
                ((XSLFSlide) slide).draw(graphics);
                break;
            case "ppt":
                ((HSLFSlide) slide).draw(graphics);
                break;
        }
    }

    /***
     * 设置PPT字体
     * @param extesion
     * @param iSlide
     * @param fontname
     * @return
     */
    public static void setPPTFont(String extesion, Slide iSlide, String fontname) {
        switch (extesion.toLowerCase()) {
            case "pptx":
                XSLFSlide slide = (XSLFSlide) iSlide;
                for(XSLFShape shape : slide.getShapes()){
                    if(shape instanceof XSLFTextShape) {
                        XSLFTextShape tsh = (XSLFTextShape)shape;
                        for(XSLFTextParagraph p : tsh){
                            for(XSLFTextRun r : p){
                                r.setFontFamily(fontname);
                            }
                        }
                    }
                }
                break;
            case "ppt":
                HSLFSlide slide2 = (HSLFSlide) iSlide;
                for(HSLFShape shape : slide2.getShapes()){
                    if(shape instanceof HSLFTextShape) {
                        HSLFTextShape tsh = (HSLFTextShape)shape;
                        for(HSLFTextParagraph p : tsh){
                            for(HSLFTextRun r : p){
                                r.setFontFamily(fontname);
                            }
                        }
                    }
                }
                break;
        }
    }

    /***
     * 获取ppt对象
     * @param extesion
     * @param fileBytes
     * @return
     * @throws IOException
     */
    public static SlideShow getSildeShow(String extesion, byte[] fileBytes) throws IOException {

        InputStream is = new ByteArrayInputStream(fileBytes);

        switch (extesion.toLowerCase()) {
            case "pptx":
                return new XMLSlideShow(is);
            case "ppt":
                return new HSLFSlideShow(is);
            default:
                return null;
        }
    }

    public static BufferedImage getPageImageByPPT(byte[] fileBytes, int page, String extesion) throws IOException {

        SlideShow ppt = getSildeShow(extesion, fileBytes);
        Dimension pgsize = ppt.getPageSize();

        try {
            if(page<1 || page>ppt.getSlides().size()) {
                throw new Exception("page_number_over");
            }
            else {
                page--;
            }

            //防止中文乱码
            setPPTFont(extesion, (Slide) ppt.getSlides().get(page), "宋体");

            BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            // clear the drawing area
            graphics.setPaint(Color.white);
            graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

            // render
            drawPPT(extesion, graphics, (Slide) ppt.getSlides().get(page));

            return img;

        } catch (Exception e) {
            throw new IOException("fail_to_convert_page_number");
        }
    }

}
