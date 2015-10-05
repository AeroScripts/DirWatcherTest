/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dirwatchertest;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aero
 */
public class SimpleWatcher {
    
    // May want to implement proper concurrency here but CopyOnWriteArrayList work fine in most cases.
    CopyOnWriteArrayList<FileChangeWatcher> watchers = new CopyOnWriteArrayList<FileChangeWatcher>();
    WatchService watcher;
    
    public SimpleWatcher(String[] directories) throws IOException{
        this();
        for(String s : directories){
            watchDir(Paths.get(s));
        }
    }
    public SimpleWatcher(Path[] directories) throws IOException{
        this();
        for(Path s : directories){
            watchDir(s);
        }
    }
    public SimpleWatcher() throws IOException{ 
        watcher = FileSystems.getDefault().newWatchService();
        new Thread(new Runnable() {

            @Override
            public void run() {
                WatchKey k;
                try{
                    while(true){ // not a fan
                        k = watcher.take();
                        for (WatchEvent<?> e : k.pollEvents()) {
                            WatchEvent<Path> path = (WatchEvent<Path>)e;
                            Kind<?> kind = e.kind();
                            Path p = path.context();
                            
                            if(kind == ENTRY_CREATE){
                                for(FileChangeWatcher w : watchers) w.added(p);
                            }else if(kind == ENTRY_DELETE){
                                for(FileChangeWatcher w : watchers) w.removed(p);
                            }
                        }
                        k.reset();
                    }
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            
        },"Directory Watcher").start();
    }
    
    
    public void watchDir(Path p) throws IOException{
//        WatchService watcher = FileSystems.getDefault().newWatchService();
        p.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
    }
    
    public void addWatcher(FileChangeWatcher watcher){
        watchers.add(watcher);
    }
}
