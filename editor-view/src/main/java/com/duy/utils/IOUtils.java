package com.duy.utils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Duy on 22-Apr-18.
 */

public class IOUtils {

    /**
     * Delete file and all child of file
     *
     * @param file the file to be deleted
     * @return true if delete success
     */
    public static boolean delete(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                boolean result = true;
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        result &= delete(child);
                    }
                }
                result &= file.delete();
                return result;
            } else {
                System.out.println("Delete " + file.getName());
                return file.delete();
            }
        }
        return false;
    }


    /**
     * Create new file, if file exist, do not create file
     */
    public static boolean createNewFile(File file) {
        try {
            file.getParentFile().mkdirs();
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void changeToExecutable(File file) {
        file.setReadable(true);
        file.setWritable(true);
        file.setExecutable(true);
    }
}
