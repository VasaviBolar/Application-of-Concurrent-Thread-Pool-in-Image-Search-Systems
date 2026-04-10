import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageService {

    private static final String IMAGE_DIR = "images/";

    public static String saveImage(byte[] data) throws IOException {

        File dir = new File(IMAGE_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // create folder if missing
        }

        String fileName = UUID.randomUUID() + ".jpg";
        String path = IMAGE_DIR + fileName;

        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(data);
        }

        System.out.println("Saved image at: " + path);

        return path;
    }
}