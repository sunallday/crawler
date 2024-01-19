package org.example;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

    static long times;
    public static void main(String[] args) {
        Date date=new Date();
        times=date.getTime();
        String url="https://m.qishuta.org/Shtml82535.html";
        //名字最好不要是数字，合并的时候会出错
        String name="神印王座";
        Get.getAllTxt(url,name);

    }
}