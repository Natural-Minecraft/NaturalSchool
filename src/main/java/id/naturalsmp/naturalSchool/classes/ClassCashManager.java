package id.naturalsmp.naturalSchool.classes;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.classes.ClassroomManager.ClassroomData;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

public class ClassCashManager {

    private final NaturalSchool plugin;
    private Economy econ = null;

    public ClassCashManager(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault plugin not found! Class cash features will not work.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No Economy provider registered in Vault!");
            return false;
        }
        econ = rsp.getProvider();
        plugin.getLogger().info("Successfully hooked into Vault Economy for class cash fund system.");
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public String getCurrentWeekIdentifier() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
        WeekFields weekFields = WeekFields.ISO; // Standard ISO week (starts on Monday)
        int week = now.get(weekFields.weekOfWeekBasedYear());
        int year = now.get(weekFields.weekBasedYear());
        return String.format("%d-W%02d", year, week);
    }

    public boolean hasPaidWeeklyFee(UUID playerUuid, int idKelas, String weekId) {
        List<Map<String, Object>> payments = plugin.getDatabaseManager().getClassCashPayments(idKelas, weekId);
        for (Map<String, Object> payment : payments) {
            String pUuidStr = (String) payment.get("player_uuid");
            if (pUuidStr != null && pUuidStr.equals(playerUuid.toString())) {
                return true;
            }
        }
        return false;
    }

    public static class CashOperationResult {
        private final boolean success;
        private final String message;

        public CashOperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public CashOperationResult payWeeklyFee(Player player) {
        if (econ == null) {
            return new CashOperationResult(false, "Sistem ekonomi (Vault) tidak terhubung.");
        }

        StudentProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return new CashOperationResult(false, "Profil belajarmu belum dimuat.");
        }

        int classNum = profile.getAcademicClass();
        if (classNum < 1 || classNum > 12) {
            return new CashOperationResult(false, "Kamu tidak terdaftar di kelas mana pun (Kelas 1-12).");
        }

        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        if (!data.isWeeklyFeeEnabled()) {
            return new CashOperationResult(false, "Kas mingguan dinonaktifkan oleh pengurus kelasmu.");
        }

        String weekId = getCurrentWeekIdentifier();
        if (hasPaidWeeklyFee(player.getUniqueId(), classNum, weekId)) {
            return new CashOperationResult(false, "Kamu sudah membayar uang kas untuk minggu ini (" + weekId + ").");
        }

        double fee = data.getWeeklyFee();
        if (!econ.has(player, fee)) {
            return new CashOperationResult(false, "Uangmu tidak cukup! Butuh " + String.format("%,.0f", fee) + " untuk membayar.");
        }

        // Withdraw & save
        econ.withdrawPlayer(player, fee);
        plugin.getClassroomManager().updateClassCash(classNum, data.getCashBalance() + fee, data.getWeeklyFee(), data.isWeeklyFeeEnabled());
        plugin.getDatabaseManager().saveClassCashPayment(player.getUniqueId().toString(), classNum, weekId, fee);
        plugin.getDatabaseManager().saveClassCashTransaction(classNum, player.getUniqueId().toString(), "DEPOSIT", fee, "Bayar kas mingguan (" + weekId + ")");

        return new CashOperationResult(true, "Berhasil membayar uang kas sebesar " + String.format("%,.0f", fee) + " untuk minggu " + weekId + ".");
    }

    public CashOperationResult withdrawCash(Player player, int classNum, double amount, String reason) {
        if (econ == null) {
            return new CashOperationResult(false, "Sistem ekonomi (Vault) tidak terhubung.");
        }

        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        if (data.getCashBalance() < amount) {
            return new CashOperationResult(false, "Saldo kas kelas tidak mencukupi! Saldo saat ini: " + String.format("%,.0f", data.getCashBalance()));
        }

        // Deposit & save
        econ.depositPlayer(player, amount);
        plugin.getClassroomManager().updateClassCash(classNum, data.getCashBalance() - amount, data.getWeeklyFee(), data.isWeeklyFeeEnabled());
        plugin.getDatabaseManager().saveClassCashTransaction(classNum, player.getUniqueId().toString(), "WITHDRAW", amount, reason);

        return new CashOperationResult(true, "Berhasil menarik " + String.format("%,.0f", amount) + " dari kas kelas ke dompetmu.");
    }

    public CashOperationResult applyFine(OfflinePlayer target, Player sender, int classNum, double amount, String reason) {
        if (econ == null) {
            return new CashOperationResult(false, "Sistem ekonomi (Vault) tidak terhubung.");
        }

        if (!econ.has(target, amount)) {
            return new CashOperationResult(false, "Pemain " + target.getName() + " tidak memiliki uang yang cukup untuk membayar denda sebesar " + String.format("%,.0f", amount) + ".");
        }

        // Withdraw & save
        econ.withdrawPlayer(target, amount);
        ClassroomData data = plugin.getClassroomManager().getClassroom(classNum);
        plugin.getClassroomManager().updateClassCash(classNum, data.getCashBalance() + amount, data.getWeeklyFee(), data.isWeeklyFeeEnabled());
        plugin.getDatabaseManager().saveClassCashTransaction(classNum, target.getUniqueId().toString(), "FINE", amount, reason + " (Denda oleh " + sender.getName() + ")");

        return new CashOperationResult(true, "Berhasil mengenakan denda sebesar " + String.format("%,.0f", amount) + " kepada " + target.getName() + ".");
    }
}
