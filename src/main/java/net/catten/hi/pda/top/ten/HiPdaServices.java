package net.catten.hi.pda.top.ten;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by CattenLinger on 2017/5/21.
 */
public class HiPdaServices {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final static String HTTP_HEAD_SET_COOKIE = "Set-Cookie";
    private final static String HTTP_REQ_PROP_COOKIE = "Cookie";

    private final static String P_KEY_LOGIN = "loginPage";

    private String username;
    private String password;

    private Properties servicesProperties;

    private Properties loginProperties;
    private List<String> cookieList;

    public HiPdaServices(String username, String password) throws IOException {
        this.loginProperties = getLoginProperties(username, password);
        this.servicesProperties = new Properties();
        servicesProperties.load(this.getClass().getResourceAsStream("/services.properties"));
    }

    /**
     * Load login information from property file
     * <p>
     * Username and password is for future actions
     *
     * @param username username
     * @param password password
     * @return
     * @throws IOException
     */
    private Properties getLoginProperties(String username, String password) throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/login.properties"));
        properties.setProperty("username", username);
        properties.setProperty("password", password);
        return properties;
    }

    /**
     * Login action
     *
     * @throws IOException
     */
    public void login() throws IOException {
        URL url = new URL(servicesProperties.getProperty(P_KEY_LOGIN));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        // post
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        PrintWriter printWriter = new PrintWriter(urlConnection.getOutputStream());
        printWriter.write(makeLoginData(loginProperties));
        printWriter.flush();

        this.cookieList = urlConnection.getHeaderFields().get(HTTP_HEAD_SET_COOKIE);
    }

    /**
     * Get contents in Discovery
     *
     * @param page:which page u want to
     * @return
     * @throws Exception
     */
    public String requestDiscoveryContent(int page) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(servicesProperties.getProperty("discoveryPage") + page)
                        .openConnection();

        // add cookie to request
        for (String cookie : cookieList)
            connection.setRequestProperty(HTTP_REQ_PROP_COOKIE, cookie.split(";", 2)[0]);

        connection.connect();

        //StringBuilder content = new StringBuilder();
        //InputStream in = new GZIPInputStream(connection.getInputStream());
        InputStream in = new BufferedInputStream(connection.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "gbk"));
        StringBuilder stringBuilder = new StringBuilder();
        //int count = 0;

        String line;
        while ((line = reader.readLine()) != null) {

            if (line.matches(" <span id=\"thread_(.*)span>")) {
                //Delete invalid XML characters in title
                stringBuilder.append(line.replaceAll("[\\x00-\\x08\\x0b-\\x0c\\x0e-\\x1f]", ""));
            }

            if (line.matches("<td class=\"nums\"><str(.*)td>")) {
                //int i = line.indexOf('/');
                //int j = line.lastIndexOf('/');
                stringBuilder.insert(
                        stringBuilder.length() - 7,
                        line.replaceAll("strong", "reply")
                                .replaceAll("em", "hit")
                                .replaceAll("<td class=\"nums\">", "")
                                .replaceAll("</td>", ""))
                        .append("\r\n");
            }

            if (line.matches("<em>20(.*)em>")) {
                ;
                stringBuilder.insert(stringBuilder.length() - 7, line.replaceAll("em", "date"));
            }

            //content.append(line + "\r\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Use this method to save data to xml file
     *
     * @param FilePath
     * @param pages
     * @throws Exception
     */

    public void writeDiscoveryToXml(String FilePath, int pages) throws Exception {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(FilePath), "UTF-8");

            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\r\n");
            writer.write("<discovery>" + "\r\n");
            for (int i = 1; i <= pages; i++) {
                logger.info("Writting page " + i);
                writer.write(requestDiscoveryContent(i));
            }
            writer.write("</discovery>" + "\r\n");
        } catch (Exception e) {
            logger.warning(String.format(
                    "Something goes wrong, error from %s, cause : %s.",
                    e.getClass().getName(),
                    e.getMessage()));
        } finally {
            if (writer != null) writer.close();
        }
    }

    /**
     * Use this method to post the topTen to specific Thread
     *
     * @param content
     * @throws Exception
     */

    public void postData(String content) throws Exception {

        String formhash = null;

        HttpURLConnection postConnection = (HttpURLConnection) new URL(
                servicesProperties.getProperty("postUrl")).openConnection();

        HttpURLConnection hashConnection = (HttpURLConnection) new URL(
                servicesProperties.getProperty("formHashUrl")).openConnection();

        // add cookie to request
        for (String cookie : cookieList) {
            postConnection.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
            hashConnection.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
        }

        hashConnection.connect();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(hashConnection.getInputStream()), "gbk"));

        String line = null;
        while ((line = reader.readLine()) != null) {
            // System.out.println(line);

            if (line.matches("^<a href=\"lo(.*)"))
                formhash = line
                    .replaceAll("<a href=\"logging\\.php\\?action=logout\\&amp;formhash=", "")
                    .replaceAll("\">退出</a>", "");
        }

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("formhash", formhash);
        map.put("subject", "");
        map.put("usesig", "0");
        map.put("message", content);

        // encode map to string
        StringBuilder params = new StringBuilder();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry element = (Map.Entry) it.next();
            params.append(element.getKey());
            params.append("=");
            params.append(java.net.URLEncoder.encode(element.getValue().toString(), "GBK"));
            params.append("&");
        }
        if (params.length() > 0) {
            params.deleteCharAt(params.length() - 1);
        }

        postConnection.setDoOutput(true);
        postConnection.setDoInput(true);
        postConnection.connect();
        PrintWriter printWriter = new PrintWriter(postConnection.getOutputStream());
        printWriter.write(params.toString());
        printWriter.flush();

        //InputStream in = new GZIPInputStream(postConnection.getInputStream());
        //reader = new BufferedReader(new InputStreamReader(in, "gbk"));

        logger.info(".........Success.........");
    }

    /**
     * Handle the xml file, and get the top ten file
     *
     * @param FilePath
     * @return
     * @throws Exception
     */

    public String getTopTen(String FilePath,String date) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 文档解析器
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(FilePath);
        Element e = document.getDocumentElement();

        NodeList nl = e.getElementsByTagName("span");
        Map<String, Integer> topTen = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < nl.getLength(); i++) {
            NodeList nl2 = nl.item(i).getChildNodes();
            if (nl2.item(1).getTextContent().equals(date)) {
                topTen.put(nl.item(i).getAttributes().item(0).getTextContent(),
                        new Integer(nl2.item(2).getTextContent()));
            }

        }

        logger.info(".........Sorting.........");
        topTen = topTen.entrySet()
                .stream()
                .sorted((arg0, arg1) -> arg1.getValue() - arg0.getValue())
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));

        // Get sorted thread(from high to low)
        Collection<String> topTenThread = topTen.keySet();
        String[] topTenThreadArr = topTenThread.toArray(new String[0]);

        StringBuilder postContent = new StringBuilder()
                .append("[size=5][color=#0000ff]")
                .append(date)
                .append("Top ten[/color][/size]")
                .append("\r\n")
                .append("\r\n");

        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < nl.getLength(); i++) {
                NodeList nl2 = nl.item(i).getChildNodes();
                if (topTenThreadArr[j].equals(nl.item(i).getAttributes().item(0).getTextContent())) {
                    String temp = nl.item(i).getAttributes().item(0).getTextContent();// TITLE
                    temp = temp.replaceAll("thread_", "");
                    postContent.append(String.format(
                            "[url=http://www.hi-pda.com/forum/viewthread.php?tid=%s&extra=page%3D1]%s[/url]\r\n回复数：%s\r\n\r\n",
                            temp,
                            nl2.item(0).getTextContent(),
                            nl2.item(2).getTextContent()
                    ));
                }
            }
        }

        return postContent.toString();
    }


    /**
     * Build POST form content
     *
     * @param properties Login info
     * @return
     */
    private String makeLoginData(Properties properties) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Map.Entry<Object, Object>> entryIterator = properties.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, Object> entry = entryIterator.next();
            stringBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            if (entryIterator.hasNext()) stringBuilder.append("&");
        }
        return stringBuilder.toString();
    }


}
