package distributedSystem.Monitoring.service;


import distributedSystem.Monitoring.model.DeviceReading;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class RecentStore {
    private final int capacity;
    private final Deque<DeviceReading> buffer;

    public RecentStore(org.springframework.core.env.Environment env) {
        this.capacity = Integer.parseInt(env.getProperty("app.recent-buffer-size", "500"));
        this.buffer = new ArrayDeque<>(capacity);
    }

    public synchronized void add(DeviceReading reading) {
        if (buffer.size() >= capacity) {
            buffer.removeFirst();
        }
        buffer.addLast(reading);
    }

    public synchronized List<DeviceReading> latest(int limit) {
        int n = Math.min(limit, buffer.size());
        ArrayList<DeviceReading> list = new ArrayList<>(n);
        // iterate from tail backwards
        Object[] arr = buffer.toArray();
        for (int i = arr.length - 1; i >= 0 && list.size() < n; i--) {
            list.add((DeviceReading) arr[i]);
        }
        return list;
    }

    public synchronized int size() {
        return buffer.size();
    }
}
