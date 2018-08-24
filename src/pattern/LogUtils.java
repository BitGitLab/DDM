package pattern;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.*;

public class LogUtils {
    public static Logger logger;

    private static String systemType = System.getProperty("os.name");

    private static String ROOT_PATH;

    private static String regexString;

    public static Logger getLogger(String name) throws IOException {
        if(logger != null) {
            return logger;
        }

        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.ALL);

        System.out.println(name);
        FileHandler handler = new FileHandler(getPath(name, ".log"));

        handler.setLevel(Level.ALL);
        handler.setFormatter(new LogFormatter());
        logger.addHandler(handler);

        return logger;
    }


    public static String getPath(String name, String suffix ) {

        if(ROOT_PATH == null){
            if (systemType.contains("Mac")) {
                ROOT_PATH = "./result";
            }else if (systemType.contains("Windows")) {
                ROOT_PATH = "D:\\codes\\java\\FMCR\\result";
            }else {
                throw new RuntimeException("unknown system type");
            }
        }

        int times = 1;

        Path path = Paths.get(ROOT_PATH, name, times + "" , "result" + suffix);
        System.out.println(path.toString());

        while(isExist(path.toString())){
            times++;
            path = Paths.get(ROOT_PATH, name, times + "" , "result" + suffix);
        }

        try{
            File file = new File(path.toString());
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toString();
    }

    /**
     * 判断文件夹是否存在
     * @param filePath 文件夹路径
     * true 文件不存在，false 文件存在不做任何操作
     */
    public static boolean isExist(String filePath) {

        if(regexString == null){
            if (systemType.contains("Mac")) {
                regexString = "/";
            }else if (systemType.contains("Windows")) {
                regexString = "\\\\";
            }else {
                throw new RuntimeException("unknown system type");
            }
        }

        String paths[] = filePath.split(regexString);

        String dir = paths[0];
        for (int i = 0; i < paths.length - 2; i++) {
            try {
                dir = dir + "/" + paths[i + 1];
                File dirFile = new File(dir);
                if (!dirFile.exists()) {
                    dirFile.mkdir();
                    System.out.println("创建目录为：" + dir);
                }
            } catch (Exception err) {
                System.err.println("文件夹创建发生异常");
            }
        }
        File fp = new File(filePath);
        if(!fp.exists()){
            return false; // 文件不存在，执行下载功能
        }else{
            return true; // 文件存在不做处理
        }
    }
}

class LogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        Date date = new Date();
        String sDate = date.toString();
//        return "[" + sDate + "]" +
        return           record.getMessage() + "\n";
    }
}
