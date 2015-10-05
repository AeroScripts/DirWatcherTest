/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dirwatchertest;

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
            watcher.addWatcher(new FileChangeWatcher() {

                @Override
                public void added(Path p) {
                    System.out.println("File added: " + p);
                }

                @Override
                public void removed(Path p) {
                    System.out.println("File removed: " + p);
                }
                
            });
        } catch (IOException ex) {
            Logger.getLogger(DirWatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
