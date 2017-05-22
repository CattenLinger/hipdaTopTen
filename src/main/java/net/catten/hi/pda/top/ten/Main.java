package net.catten.hi.pda.top.ten;

import java.io.IOException;

/**
 * Created by CattenLinger on 2017/5/21.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        HiPdaServices hiPdaServices = new HiPdaServices("-","-");

        hiPdaServices.login();
        hiPdaServices.writeDiscoveryToXml("test.xml",1);
    }
}

/*  Old logic

    public static void main(String[] args) {

        hipda test = new hipda("2016-6-24");
        try {

            test.writeDiscoveryToXml("test.xml", 10, test);

            test.postData(test.login(), test.getTopTen("test.xml"));
            // test.login();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

*/