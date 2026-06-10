import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final List<String> imagePaths = new ArrayList<>();

    public static void addImage(String path) {
        imagePaths.add(path);
    }

    public static List<String> getImages() {
        return imagePaths;
    }
}