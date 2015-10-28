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

import com.baoyz.smirk.exception.SmirkManagerNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexFile;

/**
 * Created by baoyz on 15/10/27.
 */
public class Smirk {

    private static final String MANAGER_SUFFIX = "$$SmirkManager";

    private Map<DexFile, ExtensionClassLoader> mDexFiles;
    private Context mContext;
    private Map<String, SmirkManager> mManagerCache;

    public Smirk(Context context, Map<DexFile, ExtensionClassLoader> dexFiles) {
        mDexFiles = dexFiles;
        mContext = context;
        mManagerCache = new HashMap<>();
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
        SmirkManager manager = mManagerCache.get(className);
        if (manager != null) {
            return manager;
        }
        try {
            manager = (SmirkManager) Class.forName(className).newInstance();
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
        Set<Map.Entry<DexFile, ExtensionClassLoader>> entries = mDexFiles.entrySet();
        for (Map.Entry<DexFile, ExtensionClassLoader> dexEntry : entries) {
            list.addAll(findSubClassesFromDexFile(dexEntry, clazz));
        }
        return list;
    }

    private <T> List<Class<T>> findSubClassesFromDexFile(Map.Entry<DexFile, ExtensionClassLoader> dexEntry, Class<T> clazz) {
        List<Class<T>> list = new ArrayList<>();
        Enumeration<String> entries = dexEntry.getKey().entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement();
            Class cla = null;
            try {
                cla = dexEntry.getValue().loadClass(name);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
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
        private Map<DexFile, ExtensionClassLoader> mDexFiles;

        public Builder(Context context) {
            mContext = context;
            mDexFiles = new HashMap<>();
        }

        public Builder addDexPath(String dexPath) {
            loadDex(new File(dexPath));
            return this;
        }

        public Builder addDexPath(File dexPath) {
            Map<DexFile, ExtensionClassLoader> map = loadDex(dexPath);
            if (map != null) {
                mDexFiles.putAll(map);
            }
            return this;
        }

        public Smirk build() {
            return new Smirk(mContext, mDexFiles);
        }

        private Map<DexFile, ExtensionClassLoader> loadDex(File dexPath) {

            if (dexPath == null || !dexPath.exists()) {
                return null;
            }

            Map<DexFile, ExtensionClassLoader> dexMap = new HashMap<>();

            if (dexPath.isDirectory()) {
                File[] files = dexPath.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        dexMap.putAll(loadDex(file));
                        continue;
                    }
                    if (file.getName().endsWith(".dex")) {
                        putDex(dexPath, dexMap);
                    }
                }
            } else {
                if (dexPath.getName().endsWith(".dex")) {
                    putDex(dexPath, dexMap);
                }
            }
            return dexMap;
        }

        private void putDex(File dexPath, Map<DexFile, ExtensionClassLoader> dexMap) {
            try {
                File outPath = mContext.getDir("smirk", 0);
                DexFile dexFile = DexFile.loadDex(dexPath.getAbsolutePath(), new File(outPath, dexPath.getName()).getAbsolutePath(), 0);
                ExtensionClassLoader classLoader = new ExtensionClassLoader(dexPath.getAbsolutePath(), outPath.getAbsolutePath(), null, mContext.getClassLoader());
                dexMap.put(dexFile, classLoader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
