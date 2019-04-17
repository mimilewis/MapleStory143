/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.lieDetector;

import configs.ServerConfig;
import tools.HexTool;
import tools.Pair;
import tools.Randomizer;

import java.io.*;
import java.net.URL;

/**
 * @author AuroX
 */
public class LieDetectorScript {

    private static final String IMG_DIRECTORY = ServerConfig.WORLD_SCRIPTSPATH + "/lieDetector";
    private static final String CAPTCHA_VERIFIER = "98818D40B83AECCFB7AFD7FD9653E1037519AC61";
    private static final String CAPTCHA_SERVER = "http://localhost/captcha.php?verify=" + CAPTCHA_VERIFIER;

    public static Pair<String, String> getImageBytes() {
        try {
            URL url = new URL(CAPTCHA_SERVER);
            ByteArrayOutputStream output;
            InputStream inputStream = url.openStream();
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n = 0;
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
            }
            String imgByte = HexTool.toString(output.toByteArray());
            return new Pair<>(imgByte.substring(39, imgByte.length()), output.toString().split("CAPTCHA")[0]);
        } catch (IOException ex) {
            File directory = new File(IMG_DIRECTORY);
            if (!directory.exists()) {
                System.err.println("lieDetector folder does not exist!");
                return null;
            }
            String filename[] = directory.list();
            String answer = filename[Randomizer.nextInt(filename.length)];
            answer = answer.substring(0, answer.length() - 4); // .jpg 
            try {
                return new Pair<>(HexTool.toString(getBytesFromFile(new File(IMG_DIRECTORY + "/" + answer + ".jpg"))), answer);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        byte[] bytes;
        try {
            InputStream is = new FileInputStream(file);
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                is.close();
                return null;
            }
            bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            if (offset < bytes.length) {
                System.err.println("[Lie Detector Script] Could not completely read file " + file.getName());
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return bytes;
    }
}