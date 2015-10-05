/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dirwatchertest;

import java.nio.file.Path;

/**
 *
 * @author Aero
 */
public interface FileChangeWatcher {
    abstract void added(Path p);
    abstract void removed(Path p);
}
