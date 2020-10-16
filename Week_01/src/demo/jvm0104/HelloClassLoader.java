package demo.jvm0104;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Author:
 * @Description:
 * @Created on 2020/10/16 5:55 PM
 * @Modified by:
 */
public class HelloClassLoader extends ClassLoader {

    public static void main(String[] args) {
        try {
            Class<?> aClass = new HelloClassLoader().findClass("Hello");
            Object o = aClass.newInstance();
            Method hello = aClass.getMethod("hello");
            hello.invoke(o);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        byte[] bytes = null;
        try {
            fis = new FileInputStream(new File(this.getClass().getResource("file/Hello.xlass").getPath()));
            baos = new ByteArrayOutputStream();

            //循环读取字节文件
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = fis.read(buf)) != -1) {
                //每个字节反转后写入字节数组输出流
                byte[] r = revert(buf);
                baos.write(r, 0, len);
            }

            //从输出流中拿到字节数组
            bytes = baos.toByteArray();

            //关闭输入输出流
            fis.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * 对字节数组进行反转
     *
     * @param buf
     * @return
     */
    private byte[] revert(byte[] buf) {
        byte[] r = new byte[1024];
        for (int i = 0; i < buf.length; i++) {
            int k = 255 - buf[i];
            r[i] = (byte) k;
        }
        return r;
    }
}