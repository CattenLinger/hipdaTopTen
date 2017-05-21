package net.catten.hi.pda.top.ten;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by CattenLinger on 2017/5/21.
 */
public class HiPdaServices {

    private final static String HTTP_HEAD_SET_COOKIE = "Set-Cookie";

    private final static String P_KEY_LOGIN = "loginPage";

    private String username;
    private String password;

    private Properties servicesProperties;

    private Properties loginProperties;
    private List<String> cookieList;

    public HiPdaServices(String username, String password) throws IOException {
        this.loginProperties = getLoginProperties(username,password);
        this.servicesProperties = new Properties();
        servicesProperties.load(this.getClass().getResourceAsStream("/services.properties"));
    }

    private Properties getLoginProperties(String username, String password) throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/login.properties"));
        properties.setProperty("username",username);
        properties.setProperty("password",password);
        return properties;
    }

    private String makeLoginData(Properties properties){
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Map.Entry<Object,Object>> entryIterator = properties.entrySet().iterator();
        while (entryIterator.hasNext()){
            Map.Entry<Object,Object> entry = entryIterator.next();
            stringBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            if(entryIterator.hasNext()) stringBuilder.append("&");
        }
        return stringBuilder.toString();
    }

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


}
