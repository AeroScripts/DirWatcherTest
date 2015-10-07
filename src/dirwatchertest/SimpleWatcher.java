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
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Aero
 */
public class SimpleWatcher {
    
    CopyOnWriteArrayList<FileChangeWatcher> watchers = new CopyOnWriteArrayList<FileChangeWatcher>();
    CopyOnWriteArrayList<File> watchedFiles = new CopyOnWriteArrayList<File>(); // because it needs to fire removed for every deleted file if a directory is deleted
    ArrayList<String> extensions = new ArrayList<String>();
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
    
    public void watchDir(Path p) throws IOException{
        File f = p.toFile();
        watchDir(f);
    }
    
    public void watchDir(File f) throws IOException{
        registerForWatch(f);
        // so that it can fire for each file when a folder is deleted
//        for(File s : f.listFiles())
//            
    }
    
    private void registerForWatch(File f) throws IOException{
        Paths.get(f.toURI()).register(watcher, ENTRY_CREATE, ENTRY_DELETE);
    }
    
    public void addWatcher(FileChangeWatcher watcher){
        watchers.add(watcher);
    }

    public void addExtensions(String ... extensions) {
        for(String s : extensions){
            int l = s.length();
            if(l>3)s=s.substring(l-3);else if(l==2)s="."+s; // ugly little bit of code but it makes lookups much faster (does not work with file formats that are only 1 chracter long)
            this.extensions.add(s);
        }
    }

    private boolean isValid(File f){
        String name = f.getName();
        return !f.isDirectory() && extensions.contains(name.substring(name.length()-3));
    }
    
    private void process(File f, Kind<?> kind) throws IOException{
        switch(kind.name()){
            case "ENTRY_CREATE":
                if(f.isDirectory())
                    watchDir(f);
                else
                    added(f);
                break;
            case "ENTRY_DELETE":
                removed(f);
                break;
            default: break;
        }
    }
    
    private void added(File f) throws IOException {
        if(isValid(f)){
            watchedFiles.add(f);
            for(FileChangeWatcher w : watchers)
                w.added(f);
        }
    }
    
    private void removed(File f) throws IOException {
        if(f.isDirectory()){
            String root = f.toString();
            for(File s : watchedFiles){
                if(s.toString().startsWith(root)) // not the best way to do this but it's reliable
                    removed(s);
            }
        }else{
            watchedFiles.remove(f);
            for(FileChangeWatcher w : watchers)
                w.removed(f);
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
                            
                            Path dir = (Path)k.watchable();
                            Path p = dir.resolve(path.context()).normalize();
                            File f = p.toFile();
                            try {
                                process(f, kind);
                            } catch (IOException ex) {
                                ex.printStackTrace();
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
    
}
