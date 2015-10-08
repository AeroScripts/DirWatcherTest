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
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Aero
 */
public class SimpleWatcher {
    
    private class MoveHackContainer {
        File file;
        String name;
        long deleted;
        MoveHackContainer(File f, long deleted){
            file = f;
            name = file.getName();
            this.deleted = deleted;
        }
        @Override
        public int hashCode() {
            return file.getName().hashCode();
        }
    }
    
    CopyOnWriteArrayList<FileChangeWatcher> watchers = new CopyOnWriteArrayList<FileChangeWatcher>();
    CopyOnWriteArrayList<File> watchedFiles = new CopyOnWriteArrayList<File>(); // because it needs to fire removed for every deleted file if a directory is deleted
    ArrayList<String> extensions = new ArrayList<String>();
    WatchService watcher;
    
    final long MOVEHACK_MAX_TIME = 64; // maximum amount of time (miliseconds) between ENTRY_DELETED and ENTRY_CREATED with the same filename
    
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
        for(File s : f.listFiles()){
            if(s.isDirectory())
                watchDir(s);
            else
                added(s);
        }
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
        if(kind == ENTRY_CREATE){
            if(f.isDirectory())
                watchDir(f);
            else
                added(f);
        }else removed(f);
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
            if(isValid(f)){
                watchedFiles.remove(f);
                for(FileChangeWatcher w : watchers)
                    w.removed(f);
            }
        }
    }
    
    private void processMove(File from, File to){
        if(to.isDirectory()){
            String baseFrom = from.toString() + File.separator;
            System.out.println(baseFrom);
            for(File f : to.listFiles()){
                for(FileChangeWatcher w : watchers)
                    w.moved(new File(baseFrom + f.getName()), f);
            }
        }else
            for(FileChangeWatcher w : watchers)
                w.moved(from, to);
    }
    
    public SimpleWatcher() throws IOException{ 
        watcher = FileSystems.getDefault().newWatchService();
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                WatchKey k = null; // moved hack
                try{
                    while(true){ // not a fan
                        k = watcher.take();
//                        watcher.
                        boolean removed = false;
                        File lastRemoved = null;
                        int cnt = 0;
                        for (WatchEvent<?> e : k.pollEvents()) {
                            WatchEvent<Path> path = (WatchEvent<Path>)e;
                            Kind<?> kind = e.kind();
                            removed = kind == ENTRY_DELETE;
                            Path dir = (Path)k.watchable();
                            Path p = dir.resolve(path.context()).normalize();
                            File f = p.toFile();
                            if(!removed && lastRemoved != null){ // renamed
                                 processMove(lastRemoved, f);
                            }else
                                if(removed) 
                                    lastRemoved = f;
                                else
                                    try {
                                        process(f, kind);
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                        }
                        k.reset();
                        if(removed){
                            k = watcher.poll(MOVEHACK_MAX_TIME, TimeUnit.MILLISECONDS);
                            if(k != null){
                                for (WatchEvent<?> e : k.pollEvents()) {
                                    Kind<?> kind = e.kind();
                                    WatchEvent<Path> path = (WatchEvent<Path>)e;
                                    Path dir = (Path)k.watchable();
                                    Path p = dir.resolve(path.context()).normalize();
                                    File f = p.toFile();
                                    if(kind == ENTRY_CREATE){
                                        processMove(lastRemoved, f);
                                        k.reset();
                                    }else{
                                        try {
                                            process(lastRemoved, ENTRY_DELETE);
                                            process(f, ENTRY_DELETE);
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                                k.reset();
                            }else 
                                try {
                                    process(lastRemoved, ENTRY_DELETE);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                        }
                    }
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            
        },"Directory Watcher");
        t.setDaemon(true);
        t.start();
    }
    
}
