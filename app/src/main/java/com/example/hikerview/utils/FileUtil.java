package com.example.hikerview.utils;

import android.content.Context;

import com.annimon.stream.function.Consumer;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.setting.model.SettingConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * @author fisher
 * @description 文件工具类
 */

public class FileUtil {

    public static String getFilePath(String url) {
        if (url.startsWith("hiker://files/")) {
            String fileName = url.replace("hiker://files/", "");
            File file = new File(SettingConfig.rootDir + File.separator + fileName);
            return file.getAbsolutePath();
        } else if (url.startsWith("file://")) {
            url = url.replace("file://", "");
            File file = new File(url);
            return file.getAbsolutePath();
        }
        return null;
    }

    public static String getExistFilePath(String url) {
        if (url.startsWith("hiker://files/")) {
            String fileName = url.replace("hiker://files/", "");
            File file = new File(SettingConfig.rootDir + File.separator + fileName);
            if (file.exists()) {
                return file.getAbsolutePath();
            } else {
                return null;
            }
        } else if (url.startsWith("file://")) {
            url = url.replace("file://", "");
            File file = new File(url);
            if (file.exists()) {
                return file.getAbsolutePath();
            } else {
                return null;
            }
        }
        return null;
    }

    //private static final Log Debug = LogFactory.getLog(FileUtil.class);

    // 获取从classpath根目录开始读取文件注意转化成中文
    public static String getCPFile(String path) {
        URL url = FileUtil.class.getClassLoader().getResource(path);
        String filepath = url.getFile();
        File file = new File(filepath);
        byte[] retBuffer = new byte[(int) file.length()];
        try {
            FileInputStream fis = new FileInputStream(filepath);
            fis.read(retBuffer);
            fis.close();
            return new String(retBuffer, "GBK");
        } catch (IOException e) {
            //Debug.error("FilesInAppUtil.getCPFile读取文件异常：" + e.toString());
            return null;
        }
    }


    /**
     * 利用java本地拷贝文件及文件夹,如何实现文件夹对文件夹的拷贝呢?如果文件夹里还有文件夹怎么办呢?
     *
     * @param objDir 目标文件夹
     * @param srcDir 源的文件夹
     */
    public static void copyDirectiory(final String objDir, final String srcDir)
            throws IOException {
        (new File(objDir)).mkdirs();
        File[] file = (new File(srcDir)).listFiles();
        if (file == null) {
            return;
        }
        for (File value : file) {
            if (value.isFile()) {
                FileInputStream input = new FileInputStream(value);
                FileOutputStream output = new FileOutputStream(objDir + "/"
                        + value.getName());
                byte[] b = new byte[1024 * 5];
                int len;
                while ((len = input.read(b)) != -1) {
                    output.write(b, 0, len);
                }
                output.flush();
                output.close();
                input.close();
            }
            if (value.isDirectory()) {
                copyDirectiory(objDir + "/" + value.getName(), srcDir + "/"
                        + value.getName());
            }
        }

    }

    /**
     * 将一个文件inName拷贝到另外一个文件outName中
     *
     * @param inName  源文件路径
     * @param outName 目标文件路径
     */
    public static void copyFile(final String inName, final String outName)
            throws FileNotFoundException, IOException {
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(
                    inName));
            BufferedOutputStream os;
            os = new BufferedOutputStream(
                    new FileOutputStream(outName));
            copyFile(is, os, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制文件
     *
     * @param source 输入文件
     * @param target 输出文件
     */
    public static void copy(File source, File target) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(source);
            fileOutputStream = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            while (fileInputStream.read(buffer) > 0) {
                fileOutputStream.write(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copy a file from an opened InputStream to opened OutputStream
     *
     * @param is    source InputStream
     * @param os    target OutputStream
     * @param close 写入之后是否需要关闭OutputStream
     */
    public static void copyFile(InputStream is, OutputStream os, boolean close)
            throws IOException {
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
        is.close();
        if (close)
            os.close();
    }

    public static void copyFile(Reader is, Writer os, boolean close)
            throws IOException {
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
        is.close();
        if (close)
            os.close();
    }

    public static void copyFile(String inName, PrintWriter pw, boolean close)
            throws FileNotFoundException, IOException {
        BufferedReader is = new BufferedReader(new FileReader(inName));
        copyFile(is, pw, close);
    }

    /**
     * 从文件inName中读取第一行的内容
     *
     * @param inName 源文件路径
     * @return 第一行的内容
     */
    public static String readLine(String inName) throws FileNotFoundException,
            IOException {
        BufferedReader is = new BufferedReader(new FileReader(inName));
        String line = null;
        line = is.readLine();
        is.close();
        return line;
    }

    /**
     * default buffer size
     */
    private static final int BLKSIZ = 8192;

    public static void copyFileBuffered(String inName, String outName)
            throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(inName);
        OutputStream os = new FileOutputStream(outName);
        int count = 0;
        byte b[] = new byte[BLKSIZ];
        while ((count = is.read(b)) != -1) {
            os.write(b, 0, count);
        }
        is.close();
        os.close();
    }

    /**
     * 将String变成文本文件
     *
     * @param text     源String
     * @param fileName 目标文件路径
     */
    public static void stringToFile(final String text, final String fileName)
            throws IOException {
        File file = new File(fileName);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try {
            BufferedWriter os = new BufferedWriter(new FileWriter(fileName));
            os.write(text);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开文件获得BufferedReader
     *
     * @param fileName 目标文件路径
     * @return BufferedReader
     */
    public static BufferedReader openFile(String fileName) throws IOException {
        return new BufferedReader(new FileReader(fileName));
    }

    /**
     * 获取文件filePath的字节编码byte[]
     *
     * @param filePath 文件全路径
     * @return 文件内容的字节编码
     * @roseuid 3FBE26DE027D
     */
    public static byte[] fileToBytes(String filePath) {
        if (filePath == null) {
            //Debug.info("路径为空：");
            return null;
        }

        File tmpFile = new File(filePath);

        byte[] retBuffer = new byte[(int) tmpFile.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            fis.read(retBuffer);
            fis.close();
            return retBuffer;
        } catch (IOException e) {
            //Debug.error("读取文件异常：" + e.toString());
            return null;
        }
    }

    /**
     * 将byte[]转化成文件fullFilePath
     *
     * @param fullFilePath 文件全路径
     * @param content      源byte[]
     */
    public static void bytesToFile(final String fullFilePath, final byte[] content) {
        if (fullFilePath == null || content == null) {
            return;
        }

        // 创建相应的目录
        File f = new File(getDir(fullFilePath));
        if (!f.exists()) {
            f.mkdirs();
        }

        try {
            FileOutputStream fos = new FileOutputStream(fullFilePath);
            fos.write(content);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 根据传入的文件全路径，返回文件所在路径
     *
     * @param fullPath 文件全路径
     * @return 文件所在路径
     */
    public static String getDir(String fullPath) {
        int iPos1 = fullPath.lastIndexOf("/");
        int iPos2 = fullPath.lastIndexOf("\\");
        iPos1 = (iPos1 > iPos2 ? iPos1 : iPos2);
        return fullPath.substring(0, iPos1 + 1);
    }

    /**
     * 根据传入的文件全路径，返回文件全名（包括后缀名）
     *
     * @param fullPath 文件全路径
     * @return 文件全名（包括后缀名）
     */
    public static String getFileName(String fullPath) {
        int iPos1 = fullPath.lastIndexOf("/");
        int iPos2 = fullPath.lastIndexOf("\\");
        iPos1 = (iPos1 > iPos2 ? iPos1 : iPos2);
        return fullPath.substring(iPos1 + 1);
    }

    /**
     * 获得文件名fileName中的后缀名
     *
     * @param fileName 源文件名
     * @return String 后缀名
     */
    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1,
                fileName.length());
    }

    /**
     * 根据传入的文件全名（包括后缀名）或者文件全路径返回文件名（没有后缀名）
     *
     * @param fullPath 文件全名（包括后缀名）或者文件全路径
     * @return 文件名（没有后缀名）
     */
    public static String getPureFileName(String fullPath) {
        String fileFullName = getFileName(fullPath);
        return fileFullName.substring(0, fileFullName.lastIndexOf("."));
    }

    /**
     * 转换文件路径中的\\为/
     *
     * @param filePath 要转换的文件路径
     * @return String
     */
    public static String wrapFilePath(String filePath) {
        filePath.replace('\\', '/');
        if (filePath.charAt(filePath.length() - 1) != '/') {
            filePath += "/";
        }
        return filePath;
    }

    /**
     * 删除整个目录path,包括该目录下所有的子目录和文件
     *
     * @param path
     */
    public static void deleteDirs(final String path) {
        File rootFile = new File(path);
        if (!rootFile.exists()) {
            return;
        }
        File[] files = rootFile.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                deleteDirs(file.getPath());
            } else {
                file.delete();
            }
        }
        rootFile.delete();
    }


    /**
     * 删除整个目录path,包括该目录下所有的子目录和文件
     *
     * @param path
     */
    public static void deleteFile(final String path) {
        File rootFile = new File(path);
        if (rootFile.exists()) {
            rootFile.delete();
        }
    }


    /** */
    /**
     * 文件重命名
     *
     * @param path    文件目录
     * @param oldname 原来的文件名
     * @param newname 新文件名
     */
    public static void renameFile(String path, String oldname, String newname) {
        if (!oldname.equals(newname)) {//新的文件名和以前文件名不同时,才有必要进行重命名
            File oldfile = new File(path + File.separator + oldname);
            File newfile = new File(path + File.separator + newname);
            if (!oldfile.exists()) {
                return;//重命名文件不存在
            }
            if (newfile.exists())//若在该目录下已经有一个文件和新文件名相同，则不允许重命名
                System.out.println(newname + "已经存在！");
            else {
                oldfile.renameTo(newfile);
            }
        } else {
            System.out.println("新文件名和旧文件名相同...");
        }
    }


    /** */
    /**
     * 文件夹重命名
     */
    public static boolean renameDir(String oldDirPath, String newDirPath) {
        File oleFile = new File(oldDirPath); //要重命名的文件或文件夹
        File newFile = new File(newDirPath);  //重命名为zhidian1
        return oleFile.renameTo(newFile);  //执行重命名
    }

    public static String fileToString(String filePath) {
        List<String> fileString = new ArrayList<>();
        try (BufferedReader os = new BufferedReader(new FileReader(filePath))) {
            String valueString;
            while ((valueString = os.readLine()) != null) {
                fileString.add(valueString);
            }
        } catch (IOException e) {
            Timber.d(e, "文件异常%s", e.getMessage());
        }
        return StringUtil.listToString(fileString, "\n");
    }

    public static String getFormatedFileSize(long size) {
        String result;
        if (size == 0) {
            return "0B";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        if (size < 1024) {
            result = df.format((double) size) + "B";
        } else if (size < 1048576) {
            result = df.format((double) size / 1024) + "KB";
        } else if (size < 1073741824) {
            result = df.format((double) size / 1048576) + "MB";
        } else {
            result = df.format((double) size / 1073741824) + "GB";
        }
        return result;
    }

    public static String getExtension(String fileName) {
        if (fileName.lastIndexOf(".") < 1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }


    public static String getName(String fileName) {
        if (fileName.lastIndexOf(".") < 1) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public static long getFolderSize(File file) {
        if (!file.isDirectory()) {
            return file.length();
        }
        long size = 0;
        File[] fileList = file.listFiles();
        for (File aFileList : fileList) {
            if (aFileList.isDirectory()) {
                size = size + getFolderSize(aFileList);
            } else {
                size = size + aFileList.length();
            }
        }
        return size;
    }

    public static String fileNameFilter(String fileName) {
        Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|.]");
        return fileName == null ? null : FilePattern.matcher(fileName).replaceAll("");
    }


    /**
     * 生成视频播放网页
     */
    public static void generateHtml(String fileDir, String fileName) {
        String h1 = "<!doctype html >\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <title>方圆影视播放页</title>\n" +
                "    <style>\n" +
                "        html,\n" +
                "        body {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "        }/*这里是关键*/\n" +
                "        #a1 {\n" +
                "            margin: 0 auto;\n" +
                "            width: 100%;\n" +
                "            height: 600px;\n" +
                "        }\n" +
                "    </style>\n" +
                "    <script type=\"text/javascript\"src=\"./player/ckplayer/jquery.min.js\"></script>\n" +
                "    <script type=\"text/javascript\" src=\"./player/ckplayer/ckplayer.js\" charset=\"utf-8\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"a1\"></div>\n" +
                "<script type=\"text/javascript\">\n" +
                "    var url=\"";
        String h2 = "\";\n" +
                "    var flashvars={\n" +
                "        f:'./player/m3u8/m3u8.swf',\n" +
                "        a:url,\n" +
                "        s:4,\n" +
                "        c:0\n" +
                "    };\n" +
                "    var params={bgcolor:'#000',allowFullScreen:true,allowScriptAccess:'always',wmode:'transparent'};\n" +
                "    var video=[url];\n" +
                "    var videoObject = {\n" +
                "        container: '#a1',\n" +
                "        variable: 'player',//该属性必需设置，值等于下面的new chplayer()的对象\n" +
                "        flashplayer:false,//如果强制使用flashplayer则设置成true\n" +
                "        video:url,\n" +
                "        logo: ''\n" +
                "    };\n" +
                "    var player=new ckplayer(videoObject);\n" +
                "    player.embed('./player/ckplayer/ckplayer.swf','a1','ckplayer_a1','100%','100%',false,flashvars,video,params);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
        String html = h1 + fileName + h2;
        try {
            stringToFile(html, fileDir + File.separator + "index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        copyFilesFromAssets(Application.getContext(), "player", fileDir);
    }

    public static void copyFilesFromAssets(Context context, String assetsPath, String savePath) {
        try {
            String[] fileNames = context.getAssets().list(assetsPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(savePath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {// 如果是文件
                InputStream is = context.getAssets().open(assetsPath);
                FileOutputStream fos = new FileOutputStream(new File(savePath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                    // buffer字节
                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                }
                fos.flush();// 刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void scanFiles(String dir, Consumer<String> consumer) {
        File file = new File(dir);
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            consumer.accept(file.getAbsolutePath());
        } else {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    scanFiles(file1.getAbsolutePath(), consumer);
                }
            }
        }
    }

    public static int getFileCount(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return 1;
        } else {
            int count = 0;
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    count = count + getFileCount(file1.getAbsolutePath());
                }
            }
            return count;
        }
    }
}
