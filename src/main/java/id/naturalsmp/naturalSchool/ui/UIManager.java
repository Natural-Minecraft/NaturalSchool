package id.naturalsmp.naturalSchool.ui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import id.naturalsmp.naturalSchool.profile.SchoolRank;
import id.naturalsmp.naturalSchool.ui.gui.ExamGui;
import id.naturalsmp.naturalSchool.ui.gui.ExamVariantsGui;
import id.naturalsmp.naturalSchool.ui.gui.ProfileGui;
import id.naturalsmp.naturalSchool.ui.gui.RegistrationGui;
import id.naturalsmp.naturalSchool.ui.gui.StaffPanelGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class UIManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final NaturalSchool plugin;

    // GUI instances — satu per fitur/flow
    private final RegistrationGui  registrationGui;
    private final ProfileGui       profileGui;
    private final StaffPanelGui    staffPanelGui;
    private final ExamVariantsGui  examVariantsGui;
    private final ExamGui          examGui;

    private BedrockHandler bedrockHandler;
    private final boolean floodgateEnabled;

    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ExamSession> activeExamSessions = new ConcurrentHashMap<>();

    // Prevents simultaneous NIS registrations from causing duplicate sequence numbers
    private final AtomicBoolean registrationInProgress = new AtomicBoolean(false);

    public UIManager(NaturalSchool plugin) {
        this.plugin         = plugin;
        this.registrationGui  = new RegistrationGui(plugin);
        this.profileGui       = new ProfileGui(plugin);
        this.staffPanelGui    = new StaffPanelGui(plugin);
        this.examVariantsGui  = new ExamVariantsGui(plugin);
        this.examGui          = new ExamGui(plugin);

        this.floodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");
        if (this.floodgateEnabled) {
            BedrockHandler handler = null;
            try {
                handler = (BedrockHandler) Class.forName("id.naturalsmp.naturalSchool.ui.BedrockHandlerImpl")
                        .getConstructor(NaturalSchool.class)
                        .newInstance(plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to initialize Bedrock Handler: " + e.getMessage());
            }
            this.bedrockHandler = handler;
        } else {
            this.bedrockHandler = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player state helpers
    // ─────────────────────────────────────────────────────────────────────────

    public void startOnboarding(Player player) {
        freezePlayer(player);
        openStep1(player);
    }

    public void freezePlayer(Player player)   { frozenPlayers.add(player.getUniqueId()); }
    public void unfreezePlayer(Player player) { frozenPlayers.remove(player.getUniqueId()); }
    public boolean isFrozen(UUID uuid)        { return frozenPlayers.contains(uuid); }

    private boolean isBedrockPlayer(Player player) {
        if (!floodgateEnabled || bedrockHandler == null) return false;
        return bedrockHandler.isBedrockPlayer(player.getUniqueId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registration GUI routing  →  RegistrationGui
    // ─────────────────────────────────────────────────────────────────────────

    public void openStep1(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openStep1(player);
        else registrationGui.openStep1Java(player);
    }

    public void openStep2(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openStep2(player);
        else registrationGui.openStep2Java(player);
    }

    public void openStep3(Player player) { openStep3(player, false); }

    public void openStep3(Player player, boolean showWarning) {
        if (isBedrockPlayer(player)) bedrockHandler.openStep3(player, showWarning);
        else registrationGui.openStep3Java(player, showWarning);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Profile GUI routing  →  ProfileGui
    // ─────────────────────────────────────────────────────────────────────────

    public void openProfile(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openForm(player, SchoolMenuType.PROFILE);
        else profileGui.openProfileJava(player);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Staff Panel GUI routing  →  StaffPanelGui
    // ─────────────────────────────────────────────────────────────────────────

    public void openStaffPanel(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openForm(player, SchoolMenuType.STAFF_PANEL);
        else staffPanelGui.openStaffPanelJava(player);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openMenu dispatcher (legacy compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    public void openMenu(Player player, SchoolMenuType menuType) {
        if (player == null || menuType == null) return;

        switch (menuType) {
            case REGISTRATION: openStep1(player);    break;
            case PROFILE:      openProfile(player);  break;
            case STAFF_PANEL:  openStaffPanel(player); break;
            default: throw new IllegalArgumentException("Unsupported menu type: " + menuType);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exam Variants GUI routing  →  ExamVariantsGui
    // ─────────────────────────────────────────────────────────────────────────

    public void openTestExam(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openTestExam(player);
        else examVariantsGui.openTestExamJava(player);
    }

    public void openExam1(Player player, boolean showWarning) {
        if (isBedrockPlayer(player)) bedrockHandler.openExam1(player, showWarning);
        else examVariantsGui.openExam1Java(player, showWarning);
    }

    public void openExam2(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openExam2(player);
        else examVariantsGui.openExam2Java(player);
    }

    public void openExam3(Player player, boolean showWarning) {
        if (isBedrockPlayer(player)) bedrockHandler.openExam3(player, showWarning);
        else examVariantsGui.openExam3Java(player, showWarning);
    }

    public void openExam4(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openExam4(player);
        else examVariantsGui.openExam4Java(player);
    }

    public void openExam5(Player player, boolean showWarning) {
        if (isBedrockPlayer(player)) bedrockHandler.openExam5(player, showWarning);
        else examVariantsGui.openExam5Java(player, showWarning);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exam main flow GUI routing  →  ExamGui
    // ─────────────────────────────────────────────────────────────────────────

    public void openExamPortal(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openExamPortal(player);
        else examGui.openExamPortalJava(player);
    }

    public void openExamClosed(Player player) {
        if (isBedrockPlayer(player)) bedrockHandler.openExamClosed(player);
        else examGui.openExamClosedJava(player);
    }

    public void openExamQuestion1(Player player, String subject, boolean showWarning) {
        if (isBedrockPlayer(player)) bedrockHandler.openExamQuestion1(player, subject, showWarning);
        else examGui.openExamQuestion1Java(player, subject, showWarning);
    }

    public void openExamQuestion2(Player player, String subject) {
        if (isBedrockPlayer(player)) bedrockHandler.openExamQuestion2(player, subject);
        else examGui.openExamQuestion2Java(player, subject);
    }

    public void openExamQuestion3(Player player, String subject, boolean showWarning) {
        if (isBedrockPlayer(player)) bedrockHandler.openExamQuestion3(player, subject, showWarning);
        else examGui.openExamQuestion3Java(player, subject, showWarning);
    }

    public void openExamConfirmation(Player player, String subject) {
        if (isBedrockPlayer(player)) bedrockHandler.openExamConfirmation(player, subject);
        else examGui.openExamConfirmationJava(player, subject);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exam Session Management
    // ─────────────────────────────────────────────────────────────────────────

    public ExamSession getExamSession(UUID uuid) { return activeExamSessions.get(uuid); }

    public void startExamSession(Player player, String subject) {
        activeExamSessions.put(player.getUniqueId(), new ExamSession(player.getUniqueId(), subject));
    }

    public void clearExamSession(Player player) {
        activeExamSessions.remove(player.getUniqueId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registration completion logic
    // ─────────────────────────────────────────────────────────────────────────

    public void completeRegistration(Player player) {
        UUID uuid = player.getUniqueId();
        StudentProfile profile = plugin.getProfileManager().getProfile(uuid);
        if (profile == null) {
            player.kick(MM.deserialize("<red>Failed to load profile. Please reconnect!</red>"));
            return;
        }

        if (profile.getNis() != null && !profile.getNis().isEmpty()) {
            unfreezePlayer(player);
            player.sendMessage(MM.deserialize("<yellow>Pendaftaran dilewati. Anda sudah terdaftar dengan NIS: "
                    + profile.getNis() + "</yellow>"));
            return;
        }

        if (!registrationInProgress.compareAndSet(false, true)) {
            player.sendMessage(MM.deserialize("<yellow>Sistem sedang memproses pendaftaran lain. Silakan coba lagi sebentar.</yellow>"));
            return;
        }

        player.sendMessage(MM.deserialize("<yellow>Mendaftarkan profil Anda...</yellow>"));

        CompletableFuture.supplyAsync(() -> {
            try {
                return plugin.getDatabaseManager().getRegisteredNisCount();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        .thenAccept(count -> {
            String generatedNis = plugin.getProfileManager().generateNis(count);

            profile.setNis(generatedNis);
            profile.setAcademicStage("SD");
            profile.setAcademicClass(1);
            profile.setRank(SchoolRank.SD_1);

            plugin.getProfileManager().saveProfileAsync(profile).thenRun(() -> {
                registrationInProgress.set(false);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    unfreezePlayer(player);
                    player.sendMessage(MM.deserialize(
                            "<green>Registrasi Berhasil! Selamat datang di NaturalSchool! NIS Anda: "
                                    + generatedNis + " (SD Kelas 1).</green>"));
                });
            }).exceptionally(ex -> {
                profile.setNis(null);
                profile.setAcademicStage("NONE");
                profile.setAcademicClass(0);
                profile.setRank(SchoolRank.NONE);
                registrationInProgress.set(false);
                plugin.getLogger().log(Level.SEVERE, "Failed to save registration for " + uuid, ex);
                Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(MM.deserialize("<red>Gagal menyimpan profil. Silakan coba lagi.</red>"))
                );
                return null;
            });
        }).exceptionally(ex -> {
            registrationInProgress.set(false);
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch NIS count for " + uuid, ex);
            Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage(MM.deserialize("<red>Gagal mengambil nomor urut NIS. Silakan coba lagi.</red>"))
            );
            return null;
        });
    }
}
