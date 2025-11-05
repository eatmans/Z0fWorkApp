package com.z0fsec.workhelper.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessManager {
    private static ProcessManager instance;
    private final Map<Integer, Process> runningProcesses;
    private final ExecutorService executorService;

    private ProcessManager() {
        runningProcesses = new HashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    public static ProcessManager getInstance() {
        if (instance == null) {
            instance = new ProcessManager();
        }
        return instance;
    }

    public void registerProcess(int toolId, Process process) {
        runningProcesses.put(toolId, process);
    }

    public void stopProcess(int toolId) {
        Process process = runningProcesses.get(toolId);
        if (process != null && process.isAlive()) {
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            runningProcesses.remove(toolId);
        }
    }

    public boolean isToolRunning(int toolId) {
        Process process = runningProcesses.get(toolId);
        return process != null && process.isAlive();
    }

    public void shutdown() {
        // 停止所有运行中的进程
        for (Process process : runningProcesses.values()) {
            if (process.isAlive()) {
                process.destroy();
            }
        }
        executorService.shutdown();
    }
}