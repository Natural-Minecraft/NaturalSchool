package id.naturalsmp.naturalSchool.classes;

import id.naturalsmp.naturalSchool.NaturalSchool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassroomManager {

    public static class ClassroomData {
        private final int idKelas;
        private UUID waliKelasUuid;
        private String worldName;
        private Integer x1, y1, z1, x2, y2, z2;
        private final Map<UUID, String> officers = new ConcurrentHashMap<>();

        public ClassroomData(int idKelas) {
            this.idKelas = idKelas;
        }

        public int getIdKelas() {
            return idKelas;
        }

        public UUID getWaliKelasUuid() {
            return waliKelasUuid;
        }

        public void setWaliKelasUuid(UUID waliKelasUuid) {
            this.waliKelasUuid = waliKelasUuid;
        }

        public String getWorldName() {
            return worldName;
        }

        public void setWorldName(String worldName) {
            this.worldName = worldName;
        }

        public Integer getX1() { return x1; }
        public Integer getY1() { return y1; }
        public Integer getZ1() { return z1; }
        public Integer getX2() { return x2; }
        public Integer getY2() { return y2; }
        public Integer getZ2() { return z2; }

        public void setBounds(String worldName, Integer x1, Integer y1, Integer z1, Integer x2, Integer y2, Integer z2) {
            this.worldName = worldName;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        public boolean hasBounds() {
            return worldName != null && x1 != null && y1 != null && z1 != null && x2 != null && y2 != null && z2 != null;
        }

        public Map<UUID, String> getOfficers() {
            return officers;
        }

        public boolean isInside(Location loc) {
            if (!hasBounds()) return false;
            if (!loc.getWorld().getName().equalsIgnoreCase(worldName)) return false;
            int px = loc.getBlockX();
            int py = loc.getBlockY();
            int pz = loc.getBlockZ();
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            return px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ;
        }
    }

    private final NaturalSchool plugin;
    private final Map<Integer, ClassroomData> classroomCache = new ConcurrentHashMap<>();

    public ClassroomManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void loadAllClassrooms() {
        classroomCache.clear();
        
        // Load classrooms
        List<Map<String, Object>> dbClassrooms = plugin.getDatabaseManager().getAllClassrooms();
        for (Map<String, Object> map : dbClassrooms) {
            int idKelas = (int) map.get("id_kelas");
            ClassroomData data = classroomCache.computeIfAbsent(idKelas, ClassroomData::new);
            
            String waliStr = (String) map.get("wali_kelas_uuid");
            if (waliStr != null && !waliStr.isEmpty()) {
                try {
                    data.setWaliKelasUuid(UUID.fromString(waliStr));
                } catch (IllegalArgumentException ignored) {}
            }
            
            String world = (String) map.get("world");
            Integer x1 = (Integer) map.get("x1");
            Integer y1 = (Integer) map.get("y1");
            Integer z1 = (Integer) map.get("z1");
            Integer x2 = (Integer) map.get("x2");
            Integer y2 = (Integer) map.get("y2");
            Integer z2 = (Integer) map.get("z2");
            
            if (world != null && x1 != null) {
                data.setBounds(world, x1, y1, z1, x2, y2, z2);
            }
        }

        // Load officers
        for (int idKelas = 1; idKelas <= 12; idKelas++) {
            List<Map<String, String>> officersList = plugin.getDatabaseManager().getClassroomOfficers(idKelas);
            ClassroomData data = classroomCache.computeIfAbsent(idKelas, ClassroomData::new);
            for (Map<String, String> off : officersList) {
                try {
                    UUID uuid = UUID.fromString(off.get("player_uuid"));
                    String role = off.get("role");
                    data.getOfficers().put(uuid, role.toUpperCase());
                } catch (Exception ignored) {}
            }
        }
    }

    public ClassroomData getClassroom(int idKelas) {
        return classroomCache.computeIfAbsent(idKelas, ClassroomData::new);
    }

    public Collection<ClassroomData> getAllClassroomData() {
        return classroomCache.values();
    }

    public void saveClassroomBounds(int idKelas, String world, Integer x1, Integer y1, Integer z1, Integer x2, Integer y2, Integer z2) {
        ClassroomData data = getClassroom(idKelas);
        data.setBounds(world, x1, y1, z1, x2, y2, z2);
        
        String waliStr = data.getWaliKelasUuid() != null ? data.getWaliKelasUuid().toString() : null;
        plugin.getDatabaseManager().saveClassroom(idKelas, waliStr, world, x1, y1, z1, x2, y2, z2);
    }

    public void updateWaliKelas(int idKelas, UUID waliKelasUuid) {
        ClassroomData data = getClassroom(idKelas);
        data.setWaliKelasUuid(waliKelasUuid);
        
        String waliStr = waliKelasUuid != null ? waliKelasUuid.toString() : null;
        plugin.getDatabaseManager().updateClassroomWaliKelas(idKelas, waliStr);
    }

    public void assignOfficer(int idKelas, UUID playerUuid, String role) {
        // Clear previous assignment for this player (a player can only hold one class role)
        removeOfficer(playerUuid);
        
        ClassroomData data = getClassroom(idKelas);
        data.getOfficers().put(playerUuid, role.toUpperCase());
        plugin.getDatabaseManager().saveClassroomOfficer(playerUuid.toString(), idKelas, role.toUpperCase());
    }

    public void removeOfficer(UUID playerUuid) {
        for (ClassroomData data : classroomCache.values()) {
            data.getOfficers().remove(playerUuid);
        }
        plugin.getDatabaseManager().removeClassroomOfficer(playerUuid.toString());
    }

    public void clearOfficers(int idKelas) {
        ClassroomData data = getClassroom(idKelas);
        data.getOfficers().clear();
        plugin.getDatabaseManager().clearClassroomOfficers(idKelas);
    }

    public String getClassroomRegion(int classNum) {
        return "kelas" + classNum;
    }

    public void addDoor(int idKelas, int doorNumber, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        plugin.getDatabaseManager().saveClassroomDoor(idKelas, doorNumber, worldName, x1, y1, z1, x2, y2, z2);
    }

    public void removeDoor(int idKelas, int doorNumber) {
        plugin.getDatabaseManager().deleteClassroomDoor(idKelas, doorNumber);
    }

    public List<Map<String, Object>> getDoors(int idKelas) {
        return plugin.getDatabaseManager().getClassroomDoors(idKelas);
    }

    public void toggleDoors(int idKelas, boolean open) {
        List<Map<String, Object>> doors = getDoors(idKelas);
        if (doors == null || doors.isEmpty()) return;

        for (Map<String, Object> door : doors) {
            String worldName = (String) door.get("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            int x1 = (int) door.get("x1");
            int y1 = (int) door.get("y1");
            int z1 = (int) door.get("z1");
            int x2 = (int) door.get("x2");
            int y2 = (int) door.get("y2");
            int z2 = (int) door.get("z2");

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            Material mat = open ? Material.AIR : Material.TINTED_GLASS;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        block.setType(mat);
                    }
                }
            }
        }
    }
}
