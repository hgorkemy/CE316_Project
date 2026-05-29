package com.iae.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecentProjectsService {

    private static final int MAX_RECENT = 5;
    private static final String APP_DIR =
        System.getProperty("os.name").toLowerCase().contains("win")
            ? System.getenv("APPDATA") + File.separator + "IAE"
            : System.getProperty("user.home") + File.separator + ".iae";

    private static final String RECENT_FILE = APP_DIR + File.separator + "recent.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public void addRecent(int projectId, String projectName) {
        List<RecentEntry> entries = load();
        entries.removeIf(e -> e.id == projectId);
        entries.add(0, new RecentEntry(projectId, projectName));
        if (entries.size() > MAX_RECENT) {
            entries = entries.subList(0, MAX_RECENT);
        }
        save(entries);
    }

    public List<RecentEntry> getRecent() {
        return load();
    }

    public void remove(int projectId) {
        List<RecentEntry> entries = load();
        entries.removeIf(e -> e.id == projectId);
        save(entries);
    }

    private List<RecentEntry> load() {
        File file = new File(RECENT_FILE);
        if (!file.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(file, new TypeReference<List<RecentEntry>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void save(List<RecentEntry> entries) {
        try {
            new File(APP_DIR).mkdirs();
            mapper.writeValue(new File(RECENT_FILE), entries);
        } catch (Exception ignored) {}
    }

    public static class RecentEntry {
        public int id;
        public String name;

        public RecentEntry() {}

        public RecentEntry(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
