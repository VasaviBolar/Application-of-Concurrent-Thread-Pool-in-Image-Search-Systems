import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class ImageService {

    private static final String DIR = "images/";

    public static String saveImage(byte[] data) throws Exception {

        File folder = new File(DIR);
        if (!folder.exists()) folder.mkdirs();

        String name = UUID.randomUUID() + ".jpg";
        File file = new File(DIR + name);

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();

        System.out.println("Saved: " + file.getAbsolutePath());

        return file.getAbsolutePath(); // 🔥 absolute path
    }
}