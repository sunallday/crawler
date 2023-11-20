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
        String url="https://m.qishuta.org/Shtml23858.html";
        String name="遮天";
        Get.getAllTxt(url,name);
    }
}