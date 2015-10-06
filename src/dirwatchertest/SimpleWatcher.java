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
    
    // May want to implement proper concurrency here but CopyOnWriteArrayList work fine in most cases.
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

    private void removeAll(File f) {
        if(f.isDirectory()){
            String root = f.toString();
            for(File file : watchedFiles){
                String full = file.toString();
                if(full.startsWith(root)){
                    for(FileChangeWatcher w : watchers)
                        w.removed(Paths.get(f.toURI()));
                    watchedFiles.remove(file); // this normally isnt safe, but thanks to CopyOnWriteArrayList...
                }
            }
        }else{
            for(FileChangeWatcher w : watchers) 
                w.added(Paths.get(f.toURI()));
            watchedFiles.remove(f);
        }
    }
    
    private void checkAll(File f, Kind<?> kind, boolean fire) throws IOException{
        if(!f.exists()) return;
        Path p = Paths.get(f.toURI());
        p.register(watcher, ENTRY_CREATE, ENTRY_DELETE); // there has to be a better way to convert File to Path... Oh well
        if(kind == ENTRY_DELETE){
            removeAll(f);
        }else{
            for(File s : f.listFiles()){
                if(s.isDirectory()){
                    checkAll(s, kind, fire);
                }else
                    checkFile(s, kind, fire);
            }
        }
    }
    
    public boolean isValid(File f){
        String name = f.getName();
        return !f.isDirectory() && extensions.contains(name.substring(name.length()-3));
    }
    
    public void checkFile(File f, Kind<?> kind, boolean fire){
        if(isValid(f)){
            if(kind == ENTRY_CREATE){
                if(fire)
                    for(FileChangeWatcher w : watchers) 
                        w.added(Paths.get(f.toURI()));
                watchedFiles.add(f);
            }else if(kind == ENTRY_DELETE){
                if(fire)
                    for(FileChangeWatcher w : watchers) 
                        w.removed(Paths.get(f.toURI()));
                watchedFiles.remove(f);
            }
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
                            if(f.isDirectory())
                                checkAll(f, kind, true);
                            else 
                                checkFile(f, kind, true);
                        }
                        k.reset();
                    }
                }catch(InterruptedException e){
                    e.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
        },"Directory Watcher").start();
    }
    
    
    public void watchDir(Path p) throws IOException{
        p.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
        // so that it can fire for each file when a folder is deleted
        for(File f : p.toFile().listFiles())
            if(f.isDirectory())
                checkAll(f, ENTRY_CREATE, false);
            else if(isValid(f))
                watchedFiles.add(f);
    }
    
    public void addWatcher(FileChangeWatcher watcher){
        watchers.add(watcher);
    }

    void addExtensions(String ... extensions) {
        for(String s : extensions){
            int l = s.length();
            if(l>3)s=s.substring(l-3);else if(l==2)s="."+s; // ugly little bit of code but it makes lookups much faster (does not work with file formats that are only 1 chracter long)
            this.extensions.add(s);
        }
    }
    
}
