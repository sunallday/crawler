package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Get{
    //创建一个客户端
    public static CloseableHttpClient client = HttpClients.createDefault();

    public static String path="D:/temp/";

    public static String prefix="https://m.qishuta.org";




    /**
     * 该方法为获取单个页面里的context
     */
    public static String gettxt(String urlroot) {
        //创建一个get请求
        HttpGet request = new HttpGet(urlroot);
        //发起请求
        try {
            HttpResponse response = Get.client.execute(request);
            //把响应转化为字符串 指定编码格式，否则会出现乱码
            String html = EntityUtils.toString(response.getEntity(),"utf-8");

            //把字符串转化为文档
            Document document = Jsoup.parse(html);
            //获取文档里id为novelcontent的元素
            Element novelcontent = document.getElementById("novelcontent");
            //获取文本
            String txt=novelcontent.ownText();
            //删掉多余内容
            txt=txt.substring(txt.indexOf(") ")+2);
            txt=txt.replaceAll("（本章未完，请点击下一页继续阅读）","");
            //将空格变为换行
            txt=txt.replaceAll(" ","\n");
            //删除空白行
            txt=txt.replaceAll("(?m)^[ \t]\r?\n","");

            return txt;


        } catch (Exception e) {
            System.out.println("运行出错 gettxt(String url) url:" + urlroot);
        }

        return null;
    }


    /**
     * 获取下一页的地址
     */
    public static String getNextUrl(String urlroot){
        HttpGet request = new HttpGet(urlroot);
        //发起请求
        try {
            HttpResponse response = Get.client.execute(request);

            //把响应转化为字符串 指定编码格式，否则会出现乱码
            String html = EntityUtils.toString(response.getEntity(),"utf-8");

            //把字符串转化为文档
            Document document = Jsoup.parse(html);
            //获取文档里id为novelcontent的元素
            Element next = document.getElementsByClass("p1 p3").first();
            Element a=next.getElementsByTag("a").first();
            //获取元素内无标签文字
            String txt = a.ownText();
            //<a href="url"> href不以http开头，会默认增加当前目录路径为前缀

            if(txt.equals("下一页")){
                return prefix+a.attr("href");
            }

        } catch (Exception e) {
            System.out.println("运行出错 getNextUrl(String url) url:" + urlroot);
        }

        return null;
    }

    /**
     * 获取单章context
     * @param urlroot
     * @return
     */
    public static String getone(String urlroot){
        StringBuilder txt=new StringBuilder();
        txt.append(gettxt(urlroot));
        String next=urlroot;
        while((next=getNextUrl(next))!=null){
            txt.append(gettxt(next));
        }

        return  new String(txt);
    }

    /**
     * 得到当前页面的所有章节指向与章节名字
     * */
    public static HashMap<Integer, Zhang> getCatalog(String urlroot,int begin){
        int copy=begin;
        HttpGet request=new HttpGet(urlroot);
        String[] paths=new String[20];
        HashMap<Integer,Zhang> map=new HashMap<>();
        try {
            CloseableHttpResponse response = client.execute(request);
            String html=EntityUtils.toString(response.getEntity(),"utf-8");
            Document document=Jsoup.parse(html);
            Element listXm=document.getElementsByClass("list_xm").last();
            Element ul=listXm.getElementsByTag("ul").first();
            Elements lis=ul.getElementsByTag("li");
            for(Element e:lis){
                Element a=e.getElementsByTag("a").first();
                String name=a.ownText();
                String url=prefix+a.attr("href");
                Zhang zhang=new Zhang();
                zhang.name=name;
                zhang.url=url;
                map.put(copy++,zhang);
            }
            return map;
        }catch (Exception e){
            System.out.println("getCatalog出错url:"+urlroot);
        }

        return null;
    }

    /**
     * 储存单章
     */
    public static void save(String name,String txt){
        try{
            FileWriter writer=new FileWriter(path+name+".txt");
            writer.write(txt);
            writer.flush();
            writer.close();
        }catch (Exception e){
            System.out.println("save出错 name:"+name+" rooturl:"+txt);
        }
    }

    public static void saves(HashMap<String,String> map){
        Iterator<Map.Entry<String,String>> iterator=map.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,String> entry=iterator.next();
            save(entry.getKey(),entry.getValue());
        }
    }


    /**
     * 获取所有目录地址
     */
    public static HashMap<Integer,String> getAll(String url){
        HashMap<Integer,String> map=new HashMap<>();
        int begin=0;
        HttpGet request=new HttpGet(url);
        try{
            CloseableHttpResponse response=client.execute(request);
            String html=EntityUtils.toString(response.getEntity(),"utf-8");
            Document document=Jsoup.parse(html);
            Element element=document.getElementsByAttributeValue("name","pageselect").first();
            Elements options=element.getElementsByTag("option");
            for(Element e:options){
                String t=e.attr("value");
                map.put(begin++,prefix+t);
            }
        }catch (Exception e){
            System.out.println("getAll出错:"+e.getMessage());
        }

        return map;
    }

    public static void getAllTxt(String url,String name){
        HashMap<Integer,String> map=Get.getAll(url);
        Iterator<Map.Entry<Integer,String>> iterator=map.entrySet().iterator();
        ThreadPoolExecutor poolExecutor=new MyPool(6,
                6,10,TimeUnit.MINUTES,new LinkedBlockingDeque<Runnable>(),name);
        while(iterator.hasNext()){
            Map.Entry<Integer,String> e=iterator.next();
            Map<Integer,Zhang> pmap= Get.getCatalog(e.getValue(),e.getKey()*20+1);
            //进行内容爬取
            MyThread myThread=new MyThread(pmap);
            poolExecutor.execute(myThread);
        }
        //执行完毕进行合并
        poolExecutor.shutdown();
    }


}
class MyPool extends ThreadPoolExecutor {
    String name;
    public MyPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.name=name;
    }

    @Override
    protected void terminated() {
        System.out.println("开始合并");
        hebing(name);
    }

    public static void hebing(String name){
        try{
            String path="D:/temp/";
            File folder = new File(path);
            File[] files = folder.listFiles();
            int n=files.length;
            FileWriter writer=new FileWriter(path+name+".txt");

            for(int i=1;i<=n;i++){
                File file=new File(path+i+".txt");
                InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    //System.out.println(line);
                    writer.write(line+"\n");
                }

                bufferedReader.close();
                file.delete();
            }
            writer.flush();
            writer.close();
            System.out.println("合并完成");
            Date date=new Date();
            System.out.println(date.getTime()-Main.times);
        }catch (Exception e){
            System.out.println("合并出错");
        }
    }
}

class MyThread implements Runnable{
    Map<Integer,Zhang> pmap;

    public MyThread(Map<Integer, Zhang> pmap) {
        this.pmap = pmap;
    }

    @Override
    public void run() {
        Iterator<Map.Entry<Integer,Zhang>> iterator=pmap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Integer,Zhang> e=iterator.next();
            String name=Integer.toString(e.getKey());
            System.out.println(name);
            String txt=e.getValue().name+"\n"+ Get.getone(e.getValue().url);
            //System.out.println(txt);
            Get.save(name,txt);
        }

    }
}

class Zhang{
    public String name;
    public String url;

    @Override
    public String toString() {
        return "Zhang{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}