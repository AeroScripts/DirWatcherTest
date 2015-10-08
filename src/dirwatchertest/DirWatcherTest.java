package dirwatchertest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aero
 */
public class DirWatcherTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            SimpleWatcher watcher = new SimpleWatcher(new String[]{"./test1/", "./test2/", "./test3"});
            watcher.addExtensions("mp3", "flac", "aac", "wav", "ogg"); // etc (also accepts arrays)
            watcher.addWatcher(new FileChangeWatcher() {

                @Override
                public void added(File p) {
                    System.out.println("File added: " + p);
                }

                @Override
                public void removed(File p) {
                    System.out.println("File removed: " + p);
                }

                @Override
                public void moved(File from, File to) {
                    System.out.println("File moved: " + from.toString() + " -> " + to.toString());
                }
                
            });
        } catch (IOException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
