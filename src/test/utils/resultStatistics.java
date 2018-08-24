package test.utils;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.util.*;

import static java.lang.Math.max;

public class resultStatistics {

    private static String systemType = System.getProperty("os.name");

    private static String ROOT_PATH;

    private static List<String> allProjects;

    private static int currentIndex = 0;

    private static int runTimes = 0;

    static List<String> globalFileList = new ArrayList<String>();

    public static void main(String[] args) throws Exception {

        if(ROOT_PATH == null){
            if (systemType.contains("Mac")) {
                ROOT_PATH = "./result";
            }else if (systemType.contains("Windows")) {
                ROOT_PATH = "D:\\codes\\java\\FMCR\\result";
            }else {
                throw new RuntimeException("unknown system type");
            }
        }

        allProjects = getAllProjects(ROOT_PATH);
        runTimes = getRunTimes(allProjects);

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("result");
        HSSFRow row = null;
        //create head
        createTableHeads(workbook,sheet,runTimes);

        for (String packageName : allProjects){

            List<File> allLogFiles = new ArrayList<File>();
            allLogFiles = getAllLogFiles(ROOT_PATH + "/" + packageName,allLogFiles);

            List<String> currentLine = new ArrayList<>();
            currentLine.add(packageName.split("\\.")[2]);

            int totalBefore = 0;
            List<String> beforeList = new ArrayList<>();
            int totalAfter = 0;
            List<String> afterList = new ArrayList<>();
            int totalRealD = 0;
            List<String> realDiff = new ArrayList<>();
            int totalRealF = 0;
            List<String> realFinal = new ArrayList<>();
            int totalDDtime = 0;
            List<String> ddtimeList = new ArrayList<>();
            int totalTime = 0;
            List<String> totaltimeList = new ArrayList<>();

//            System.out.println(dir);
            for(File file : allLogFiles){

//                System.out.println("\t" + file.getPath());
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    String lineStr = null;
                    while((lineStr = bufferedReader.readLine()) != null){

                        if(lineStr.startsWith("Different Pattern Size")){
                            beforeList.add(lineStr.split(":")[1].trim());
                            totalBefore += Integer.parseInt(lineStr.split(":")[1].trim());
//                            System.out.println(lineStr.split(":")[1]);
                        }else if(lineStr.startsWith("final result")){
                            afterList.add(lineStr.split(":")[1].trim());
                            totalAfter += Integer.parseInt(lineStr.split(":")[1].trim());
                        }else if(lineStr.startsWith("Real Different Pattern Size:")){
                            realDiff.add(lineStr.split(":")[1].trim());
                            totalRealD += Integer.parseInt(lineStr.split(":")[1].trim());
                        }else if(lineStr.startsWith("Real final result:")){
                            realFinal.add(lineStr.split(":")[1].trim());
                            totalRealF += Integer.parseInt(lineStr.split(":")[1].trim());
                        }else if(lineStr.startsWith("DD          time")){
                            ddtimeList.add(lineStr.split(":")[1].replace("ms","").trim());
                            totalDDtime += Integer.parseInt(lineStr.split(":")[1].replace("ms","").trim());
                        }else if(lineStr.startsWith("Total       time")){
                            totaltimeList.add(lineStr.split(":")[1].replace("ms","").trim());
                            totalTime += Integer.parseInt(lineStr.split(":")[1].replace("ms","").trim());
                        }else{}
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for(int i = 0; i < runTimes; i ++){
                if(beforeList.size() != runTimes && beforeList.size() < (i+1)){

                    currentLine.add(String.valueOf(0));
                    currentLine.add(String.valueOf(0));
                    currentLine.add(String.valueOf(0));
                    currentLine.add(String.valueOf(0));
                    currentLine.add(String.valueOf(0));
                    currentLine.add(String.valueOf(0));
                }else{
                    currentLine.add(beforeList.get(i));
                    currentLine.add(afterList.get(i));
                    currentLine.add(realDiff.get(i));
                    currentLine.add(realFinal.get(i));
                    currentLine.add(ddtimeList.get(i));
                    currentLine.add(totaltimeList.get(i));
                }
            }

            currentLine.add(String.valueOf(totalBefore / beforeList.size()));
            currentLine.add(String.valueOf(totalAfter / afterList.size()));
            currentLine.add(String.valueOf(totalRealD / realDiff.size()));
            currentLine.add(String.valueOf(totalRealF / realFinal.size()));
            currentLine.add(String.valueOf(totalDDtime / ddtimeList.size()));
            currentLine.add(String.valueOf(totalTime / totaltimeList.size()));

            double minValue = 9999;
            int minIndex = 0;
            for(int i = 0; i < runTimes; i++){

                System.out.println(i);
                if(Integer.parseInt(realFinal.get(i)) != 0 && Integer.parseInt(realDiff.get(i)) != 0){
                    if(Double.parseDouble(realFinal.get(i)) / Double.parseDouble(realDiff.get(i)) < minValue){
                        minValue = Double.parseDouble(realFinal.get(i)) / Double.parseDouble(realDiff.get(i));
                        minIndex = i;
                    }
                }
            }
            currentLine.add(beforeList.get(minIndex));
            currentLine.add(afterList.get(minIndex));
            currentLine.add(realDiff.get(minIndex));
            currentLine.add(realFinal.get(minIndex));
            currentLine.add(ddtimeList.get(minIndex));
            currentLine.add(totaltimeList.get(minIndex));

            row = sheet.createRow(currentIndex++);
            //add project name
            row.createCell(0).setCellValue(currentLine.get(0));
            for(int i = 1; i < currentLine.size() ; i++){
                row.createCell(i).setCellValue(Integer.parseInt(currentLine.get(i)));
            }
        }

        FileOutputStream fos = new FileOutputStream("./result/resultStatistics.xls");
        workbook.write(fos);
        fos.close();

    }

    public static void createTableHeads(HSSFWorkbook workbook,HSSFSheet sheet,int runTimes) throws Exception {

        HSSFCellStyle headstyle = workbook.createCellStyle();
        headstyle.setAlignment(HorizontalAlignment.CENTER);
        headstyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headstyle.setBorderBottom(BorderStyle.THIN);
        headstyle.setBorderLeft(BorderStyle.THIN);
        headstyle.setBorderTop(BorderStyle.THIN);
        headstyle.setBorderRight(BorderStyle.THIN);

        HSSFCell cell;
        HSSFRow row;
        int colNum = 6;

        //first row
        sheet.setColumnWidth( 0, 3600);
        row = sheet.createRow(currentIndex++);

        List<String> row1 = new ArrayList<>();
        List<List<Integer>> mergePos1 = new ArrayList<>();
        row1.add("ProjectNum");
        mergePos1.add(new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 2, 0, 0})));
        for(int i = 0; i < runTimes * colNum; i ++){
            row1.add("times");
        }
        mergePos1.add(new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 0, 1, runTimes*colNum})));
        for(int i = 0; i < colNum; i ++){
            row1.add("average");
        }
        mergePos1.add(new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 0, runTimes*colNum+1, runTimes*colNum+colNum})));

        for(int i = 0; i < colNum; i ++){
            row1.add("best");
        }
        mergePos1.add(new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 0, (runTimes+1)*colNum+1, (runTimes+2)*colNum})));

        for(int index = 0; index < row1.size(); index++){

            cell = row.createCell(index);
            cell.setCellValue(row1.get(index));
            cell.setCellStyle(headstyle);
        }
        //merge
        for(List<Integer> mergeIndex : mergePos1){
            sheet.addMergedRegion(new CellRangeAddress(mergeIndex.get(0), mergeIndex.get(1),mergeIndex.get(2), mergeIndex.get(3)));
        }

        //second row
        row = sheet.createRow(currentIndex++);
        List<String> row2 = new ArrayList<>();
        List<List<Integer>> mergePos2 = new ArrayList<>();

        for(int i = 1; i <= runTimes ; i ++){
            for(int j = 0; j < colNum; j++){
                row2.add(String.valueOf(i));
            }
            mergePos2.add(new ArrayList<Integer>(Arrays.asList(new Integer[]{1, 1, colNum*(i-1) + 1, i*colNum})));
        }

        //average
        row2.add("before");
        row2.add("after");
        row2.add("real Diff");
        row2.add("real Final");
        row2.add("dd time");
        row2.add("total time");
        //best
        row2.add("before");
        row2.add("after");
        row2.add("real Diff");
        row2.add("real Final");
        row2.add("dd time");
        row2.add("total time");


        for(int index = 1; index <= row2.size(); index++){
            cell = row.createCell(index);
            cell.setCellValue(row2.get(index - 1));
            cell.setCellStyle(headstyle);
        }
        //merge
        for(List<Integer> mergeIndex : mergePos2){
            sheet.addMergedRegion(new CellRangeAddress(mergeIndex.get(0), mergeIndex.get(1),mergeIndex.get(2), mergeIndex.get(3)));
        }

        //third row
        row = sheet.createRow(currentIndex++);
        List<String> row3 = new ArrayList<>();
        List<List<Integer>> mergePos3 = new ArrayList<>();

        for(int i = 1; i <= (runTimes + 2) ; i ++){

            row3.add("before");
            row3.add("after");
            row3.add("real Diff");
            row3.add("real Final");
            row3.add("dd time");
            row3.add("total time");
        }
        for(int i = 1; i <= colNum*2; i++){
            mergePos3.add(new ArrayList<Integer>(Arrays.asList(new Integer[]{1, 2, runTimes*colNum + i, runTimes*colNum + i})));
        }

        for(int index = 1; index <= row3.size(); index++){
            cell = row.createCell(index);
            cell.setCellValue(row3.get(index - 1));
            cell.setCellStyle(headstyle);
        }
        //merge
        for(List<Integer> mergeIndex : mergePos3){
            sheet.addMergedRegion(new CellRangeAddress(mergeIndex.get(0), mergeIndex.get(1),mergeIndex.get(2), mergeIndex.get(3)));
        }

    }

    public static List<String> getAllProjects(String rootPath){

        File dir = new File(rootPath);
        File[] files = dir.listFiles();
        List<String> allProjects = new ArrayList<String>();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                allProjects .add(files[i].getName());
            }
        }
        return allProjects;
    }


    public static List<File> getAllLogFiles(String strPath,List<File> allLogFiles) {

        File dir = new File(strPath);
        File[] files = dir.listFiles();

        if(allLogFiles == null){
            allLogFiles = new ArrayList<>();
        }

        if (files != null) {

            for (int i = 0; i < files.length; i++) {

                String fileName = files[i].getName();
                if (files[i].isDirectory()) {

                    getAllLogFiles(files[i].getAbsolutePath(),allLogFiles);
                } else if (fileName.endsWith(".log")) {

                    allLogFiles.add(files[i]);
                } else {
                    continue;
                }
            }

        }
        return allLogFiles;
    }

    public static int getRunTimes(List<String> strPath){

        List<File> allLogFiles = new ArrayList<File>();
        int[] runTimesFlag = new int[100];

        for(String pkgName : strPath){
            allLogFiles = getAllLogFiles(ROOT_PATH + "/" + pkgName,allLogFiles);
            runTimesFlag[allLogFiles.size()] ++;
            allLogFiles.clear();
        }
        int value = runTimesFlag[0];
        int runTimes = 0;
        for(int i = 1; i < runTimesFlag.length; i++){
            if(runTimesFlag[i] > value){
                value = runTimesFlag[i];
                runTimes = i;
            }
        }

        return runTimes;
    }
}
