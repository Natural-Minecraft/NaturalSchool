package id.naturalsmp.naturalSchool.classes;

import com.google.gson.Gson;
import id.naturalsmp.naturalSchool.NaturalSchool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassroomManager {

    public static class OfficerInfo {
        private String role;
        private String username;

        public OfficerInfo(String role, String username) {
            this.role = role;
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public static class DoorData {
        private int doorNumber;
        private String world;
        private int x1, y1, z1, x2, y2, z2;

        public DoorData(int doorNumber, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
            this.doorNumber = doorNumber;
            this.world = world;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        public int getDoorNumber() {
            return doorNumber;
        }

        public String getWorld() {
            return world;
        }

        public int getX1() {
            return x1;
        }

        public int getY1() {
            return y1;
        }

        public int getZ1() {
            return z1;
        }

        public int getX2() {
            return x2;
        }

        public int getY2() {
            return y2;
        }

        public int getZ2() {
            return z2;
        }
    }

    public static class ClassroomData {
        private final int idKelas;
        private UUID waliKelasUuid;
        private String waliKelasName;
        private String worldName;
        private Integer x1, y1, z1, x2, y2, z2;
        private final Map<UUID, OfficerInfo> officers = new ConcurrentHashMap<>();
        private final List<DoorData> doors = new ArrayList<>();
        private double cashBalance = 0.0;
        private double weeklyFee = 1000.0;
        private boolean weeklyFeeEnabled = true;

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

        public String getWaliKelasName() {
            return waliKelasName;
        }

        public void setWaliKelasName(String waliKelasName) {
            this.waliKelasName = waliKelasName;
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

        public Map<UUID, OfficerInfo> getOfficers() {
            return officers;
        }

        public List<DoorData> getDoors() {
            return doors;
        }

        public double getCashBalance() {
            return cashBalance;
        }

        public void setCashBalance(double cashBalance) {
            this.cashBalance = cashBalance;
        }

        public double getWeeklyFee() {
            return weeklyFee;
        }

        public void setWeeklyFee(double weeklyFee) {
            this.weeklyFee = weeklyFee;
        }

        public boolean isWeeklyFeeEnabled() {
            return weeklyFeeEnabled;
        }

        public void setWeeklyFeeEnabled(boolean weeklyFeeEnabled) {
            this.weeklyFeeEnabled = weeklyFeeEnabled;
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
        
        List<Map<String, Object>> dbClassrooms = plugin.getDatabaseManager().getAllClassrooms();
        Gson gson = new Gson();
        for (Map<String, Object> map : dbClassrooms) {
            int idKelas = (int) map.get("id_kelas");
            ClassroomData data = classroomCache.computeIfAbsent(idKelas, ClassroomData::new);
            
            String waliStr = (String) map.get("wali_kelas_uuid");
            if (waliStr != null && !waliStr.isEmpty()) {
                try {
                    data.setWaliKelasUuid(UUID.fromString(waliStr));
                } catch (IllegalArgumentException ignored) {}
            }
            data.setWaliKelasName((String) map.get("wali_kelas_name"));
            
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
            
            // officers JSON
            String officersJson = (String) map.get("officers");
            if (officersJson != null && !officersJson.isEmpty()) {
                try {
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, OfficerInfo>>(){}.getType();
                    Map<String, OfficerInfo> parsedOfficers = gson.fromJson(officersJson, type);
                    if (parsedOfficers != null) {
                        for (Map.Entry<String, OfficerInfo> entry : parsedOfficers.entrySet()) {
                            try {
                                UUID uuid = UUID.fromString(entry.getKey());
                                data.getOfficers().put(uuid, entry.getValue());
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse officers for classroom " + idKelas + ": " + e.getMessage());
                }
            }

            // doors JSON
            String doorsJson = (String) map.get("doors");
            if (doorsJson != null && !doorsJson.isEmpty()) {
                try {
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<DoorData>>(){}.getType();
                    List<DoorData> parsedDoors = gson.fromJson(doorsJson, type);
                    if (parsedDoors != null) {
                        data.getDoors().addAll(parsedDoors);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse doors for classroom " + idKelas + ": " + e.getMessage());
                }
            }

            // cash details
            if (map.containsKey("cash_balance")) {
                data.setCashBalance((double) map.get("cash_balance"));
            }
            if (map.containsKey("weekly_fee")) {
                data.setWeeklyFee((double) map.get("weekly_fee"));
            }
            if (map.containsKey("weekly_fee_enabled")) {
                Object val = map.get("weekly_fee_enabled");
                if (val instanceof Number) {
                    data.setWeeklyFeeEnabled(((Number) val).intValue() == 1);
                } else if (val instanceof Boolean) {
                    data.setWeeklyFeeEnabled((Boolean) val);
                }
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
        String waliName = data.getWaliKelasName();
        plugin.getDatabaseManager().saveClassroom(idKelas, waliStr, waliName, world, x1, y1, z1, x2, y2, z2);
    }

    public void updateWaliKelas(int idKelas, UUID waliKelasUuid) {
        ClassroomData data = getClassroom(idKelas);
        data.setWaliKelasUuid(waliKelasUuid);
        
        String waliStr = waliKelasUuid != null ? waliKelasUuid.toString() : null;
        String waliName = null;
        if (waliKelasUuid != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(waliKelasUuid);
            waliName = op.getName();
        }
        data.setWaliKelasName(waliName);
        plugin.getDatabaseManager().updateClassroomWaliKelas(idKelas, waliStr, waliName);
    }

    public void assignOfficer(int idKelas, UUID playerUuid, String username, String role) {
        // Clear previous assignment for this player
        removeOfficer(playerUuid);
        
        ClassroomData data = getClassroom(idKelas);
        data.getOfficers().put(playerUuid, new OfficerInfo(role.toUpperCase(), username));
        saveOfficersToDb(idKelas, data);
    }

    public void removeOfficer(UUID playerUuid) {
        for (Map.Entry<Integer, ClassroomData> entry : classroomCache.entrySet()) {
            ClassroomData data = entry.getValue();
            if (data.getOfficers().containsKey(playerUuid)) {
                data.getOfficers().remove(playerUuid);
                saveOfficersToDb(entry.getKey(), data);
            }
        }
    }

    public void clearOfficers(int idKelas) {
        ClassroomData data = getClassroom(idKelas);
        data.getOfficers().clear();
        saveOfficersToDb(idKelas, data);
    }

    private void saveOfficersToDb(int idKelas, ClassroomData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Gson gson = new Gson();
            String officersJson = gson.toJson(data.getOfficers());
            plugin.getDatabaseManager().updateClassOfficers(idKelas, officersJson);
        });
    }

    public String getClassroomRegion(int classNum) {
        return "kelas" + classNum;
    }

    public void addDoor(int idKelas, int doorNumber, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        ClassroomData data = getClassroom(idKelas);
        data.getDoors().removeIf(door -> door.getDoorNumber() == doorNumber);
        data.getDoors().add(new DoorData(doorNumber, worldName, x1, y1, z1, x2, y2, z2));
        saveDoorsToDb(idKelas, data);
    }

    public void removeDoor(int idKelas, int doorNumber) {
        ClassroomData data = getClassroom(idKelas);
        data.getDoors().removeIf(door -> door.getDoorNumber() == doorNumber);
        saveDoorsToDb(idKelas, data);
    }

    private void saveDoorsToDb(int idKelas, ClassroomData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Gson gson = new Gson();
            String doorsJson = gson.toJson(data.getDoors());
            plugin.getDatabaseManager().updateClassDoors(idKelas, doorsJson);
        });
    }

    public List<Map<String, Object>> getDoors(int idKelas) {
        ClassroomData data = getClassroom(idKelas);
        List<Map<String, Object>> list = new ArrayList<>();
        for (DoorData door : data.getDoors()) {
            Map<String, Object> map = new HashMap<>();
            map.put("door_number", door.getDoorNumber());
            map.put("world", door.getWorld());
            map.put("x1", door.getX1());
            map.put("y1", door.getY1());
            map.put("z1", door.getZ1());
            map.put("x2", door.getX2());
            map.put("y2", door.getY2());
            map.put("z2", door.getZ2());
            list.add(map);
        }
        return list;
    }

    public void updateClassCash(int idKelas, double balance, double weeklyFee, boolean feeEnabled) {
        ClassroomData data = getClassroom(idKelas);
        data.setCashBalance(balance);
        data.setWeeklyFee(weeklyFee);
        data.setWeeklyFeeEnabled(feeEnabled);
        plugin.getDatabaseManager().updateClassCash(idKelas, balance, weeklyFee, feeEnabled);
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
