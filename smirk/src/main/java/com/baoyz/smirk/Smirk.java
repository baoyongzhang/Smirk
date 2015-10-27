/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 baoyongzhang <baoyz94@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.baoyz.smirk;

import android.content.Context;
import android.util.Log;

import com.baoyz.smirk.exception.SmirkManagerNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * Created by baoyz on 15/10/27.
 */
public class Smirk {

    private static final String MANAGER_SUFFIX = "$$SmirkManager";

    private List<DexFile> mDexFiles;
    private Context mContext;

    public Smirk(Context context, List<DexFile> dexFiles) {
        mDexFiles = dexFiles;
        mContext = context;
    }

    public <T> T create(Class<T> clazz) {
        SmirkManager manager = findExtensionManager(clazz);
        if (clazz.isInstance(manager)) {
            return (T) manager;
        }
        return null;
    }

    private <T> SmirkManager findExtensionManager(Class<T> clazz) {
        String className = clazz.getCanonicalName() + MANAGER_SUFFIX;
        try {
            SmirkManager manager = (SmirkManager) Class.forName(className).newInstance();
            List<T> extensionInstances = findExtensionInstances(clazz);
            manager.putAll(extensionInstances);
            return manager;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new SmirkManagerNotFoundException(className);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> List<T> findExtensionInstances(Class<T> clazz) {
        List<Class<T>> classList = findSubClasses(clazz);
        List<T> list = new ArrayList<>();
        for (Class<T> cla : classList) {
            try {
                list.add(cla.newInstance());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private <T> List<Class<T>> findSubClasses(Class<T> clazz) {
        List<Class<T>> list = new ArrayList<>();
        for (DexFile dexFile : mDexFiles) {
            list.addAll(findSubClassesFromDexFile(dexFile, clazz));
        }
        return list;
    }

    private <T> List<Class<T>> findSubClassesFromDexFile(DexFile dexFile, Class<T> clazz) {
        List<Class<T>> list = new ArrayList<>();
        Enumeration<String> entries = dexFile.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement();
            Log.e("byz", "findSubClass " + name);
            // TODO 这样无法load到Class,应该需要创建DexClassLoader
            Class cla = dexFile.loadClass(name, dexFile.getClass().getClassLoader());
            if (cla == null)
                continue;
            if (cla.isAssignableFrom(cla)) {
                list.add(cla);
            }
        }
        return list;
    }

    public static class Builder {

        private Context mContext;
        private List<DexFile> mDexFiles;

        public Builder(Context context) {
            mContext = context;
            mDexFiles = new ArrayList<>();
        }

        public Builder addDexPath(String dexPath) {
            loadDex(new File(dexPath));
            return this;
        }

        public Builder addDexPath(File dexPath) {
            List<DexFile> list = loadDex(dexPath);
            if (list != null) {
                mDexFiles.addAll(list);
            }
            return this;
        }

        public Smirk build() {
            return new Smirk(mContext, mDexFiles);
        }

        private List<DexFile> loadDex(File dexPath) {

            if (dexPath == null || !dexPath.exists()) {
                return null;
            }

            List<DexFile> dexList = new ArrayList<>();

            if (dexPath.isDirectory()) {
                File[] files = dexPath.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        dexList.addAll(loadDex(file));
                        continue;
                    }
                    if (file.getName().endsWith(".dex")) {
                        try {
                            DexFile.loadDex(dexPath.getAbsolutePath(), File.createTempFile("opt", "dex",
                                    mContext.getFilesDir()).getPath(), 0);
                            dexList.add(new DexFile(dexPath));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (dexPath.getName().endsWith(".dex")) {
                    try {
                        File tmp = mContext.getDir("osdk", 0);
                        DexFile.loadDex(dexPath.getAbsolutePath(), tmp.getAbsolutePath(), 0);
                        dexList.add(new DexFile(dexPath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return dexList;
        }

    }
}
