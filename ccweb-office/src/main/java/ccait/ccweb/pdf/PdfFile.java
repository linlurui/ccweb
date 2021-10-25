/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.pdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfFile
{
    public String trailer;
    public InputStream memory;
    public Hashtable<Integer, PdfFileObject> objects;
    public String pages;

    public PdfFile(InputStream InputStream)
    {
        this.memory = InputStream;
    }

    public InputStream ToStream()
    {
        return this.memory;
    }

    public void load() throws Exception {
        int startxref = this.getStartXref();
        this.trailer = this.parseTrailer(startxref);
        ArrayList<Integer> adds=this.getAddresses(startxref);
        this.loadHash( adds);
    }
    private void loadHash(ArrayList<Integer> addresses) throws IOException {
        this.objects = new Hashtable();
        int part=0;
        int total=addresses.size();
        this.memory.reset();
        for (int add : addresses)
        {
            this.memory.mark(add);
            InputStreamReader isr = new InputStreamReader(this.memory);
            BufferedReader sr = new BufferedReader(isr);
            String line = sr.readLine();
            if(line != null) {
                if (line.length() < 2) {
                    line = sr.readLine();
                }
                Matcher m = Pattern.compile("(\\d+)( )+0 obj").matcher(line);
                if (m.matches()) {
                    int num = Integer.parseInt(m.group(1).trim());
                    if (!objects.containsKey(num)) {
                        objects.put(num, PdfFileObject.create(this, num, add));
                    }
                }
            }
            part++;
        }
    }

    public String getVersion()
    {
        String strRtn = "";
        try
        {
            InputStreamReader isr = new InputStreamReader(this.memory);
            BufferedReader sr = new BufferedReader(isr);
            String strLine = sr.readLine();

            int i = strLine.indexOf("-");
            if (i >= 0)
            {
                try
                {
                    strRtn = strLine.substring(i+1);
                }

                catch(Exception e){}
            }
        }
        catch(Exception e) {}
        return strRtn;
    }

    public PdfFileObject loadObject(String text,String key) {
        int i = text.indexOf("/" + key + " ");
        if(i<0) {
            return null;
        }

        String[] arr = text.substring(i + 1).split(" ");
        if (arr!=null && arr.length>1) {
            return this.loadObject(Integer.parseInt(arr[1]));
        }
        return null;
    }

    public PdfFileObject loadObject(int number)
{
    return this.objects.get(number);
}
    public List<PdfFileObject> getPageList() {
        PdfFileObject root = this.loadObject(this.trailer, "Root");
        PdfFileObject pages = this.loadObject(root.text, "Pages");
        return pages.getKids();
    }

    public Integer[] getPages() {
        ArrayList ps = new ArrayList();
        if (this.pages == null || pages.length() == 0)
        {
            for (int index = 0; index < this.getPageCount(); index++)
            {
                ps.add(index);
            }
        }
        else
        {
            String[] ss = this.pages.split(",| |;");
            for (String s : ss)
                if (Pattern.matches("\\d+-\\d+", s))
            {
                int start = Integer.parseInt(s.split("-")[0].trim());
                int end = Integer.parseInt(s.split("-")[1].trim());
                if (start > end)
                    return new Integer[] { 0 };
                while (start <= end)
                {
                    ps.add(start-1);
                    start++;
                }
            }
						else
            {
                ps.add(Integer.parseInt(s.trim())-1);
            }
        }
        return (Integer[]) ps.toArray(new Integer[]{});
    }

    public int getPageCount() {
        return this.getPageList().size();
    }

    private ArrayList<Integer> getAddresses(int xref) throws IOException {
        this.memory.reset();
        this.memory.mark(xref);
        ArrayList<Integer> al = new ArrayList<Integer>();
        InputStreamReader isr = new InputStreamReader(this.memory);
        BufferedReader sr = new BufferedReader(isr);
        String line="";
        String prevPattern = "/Prev \\d+";
        Boolean ok = true;
        while (ok)
        {
            if (Pattern.matches("\\d{10} 00000 n\\s*", line))
            {
                al.add(Integer.parseInt(line.substring(0,10)));
            }

            line = sr.readLine();
            ok = !(line == null || Pattern.matches(">>", line));
            if (line != null)
            {
                Matcher m = Pattern.compile(prevPattern).matcher(line);
                if (m.matches())
                {
                    al.addAll(this.getAddresses(Integer.parseInt(m.group().substring(6))));
                }
            }

        }
        return al;
    }

    private int getStartXref() throws Exception {
        this.memory.reset();
        InputStreamReader isr = new InputStreamReader(this.memory);
        BufferedReader sr = new BufferedReader(isr);
        this.memory.mark(this.memory.available() - 100);
        String line="";
        while (!line.startsWith("startxref"))
        {
            line = sr.readLine();
        }
        Integer startxref = Integer.parseInt(sr.readLine().trim());
        if (startxref == -1) {
            throw new Exception("Cannot find the startxref");
        }
        this.memory.reset();

        return startxref;
    }

    private String parseTrailer(int xref) throws Exception {
        this.memory.reset();
        this.memory.mark(xref);
        InputStreamReader isr = new InputStreamReader(this.memory);
        BufferedReader sr = new BufferedReader(isr);
        String line;
        String trailer = "";
        Boolean istrailer = false;
        while ((line = sr.readLine()) != "startxref")
        {
            if(line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("trailer"))
            {
                trailer = "";
                istrailer = true;
            }
            if (istrailer)
            {
                trailer += line + "\r";
            }
        }
        if (trailer == "") {
            throw new Exception("Cannot find trailer");
        }

        this.memory.reset();

        return trailer;
    }

}
