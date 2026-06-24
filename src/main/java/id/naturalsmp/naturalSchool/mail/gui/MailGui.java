package id.naturalsmp.naturalSchool.mail.gui;

import id.naturalsmp.naturalSchool.NaturalSchool;
import id.naturalsmp.naturalSchool.profile.StudentProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.text.SimpleDateFormat;
import java.util.*;

public class MailGui {

    private final NaturalSchool plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public MailGui(NaturalSchool plugin) {
        this.plugin = plugin;
    }

    public void openMailMenu(Player player) {
        if (plugin.getServer().getPluginManager().getPlugin("Floodgate") != null &&
            FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openMailMenuBedrock(player);
        } else {
            openMailMenuJava(player);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAVA EDITION (PAPER DIALOGS)
    // ─────────────────────────────────────────────────────────────────────────

    private void openMailMenuJava(Player player) {
        UUID uuid = player.getUniqueId();
        int unread = plugin.getDatabaseManager().getUnreadMailCount(uuid);
        int totalSent = plugin.getDatabaseManager().getTotalSentMailCount(uuid);

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Mail System ===</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Pesan Belum Dibaca:</yellow> <white>" + unread + "</white>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Total Surat Terkirim:</yellow> <white>" + totalSent + "</white>")));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        Component inboxLink = MiniMessage.miniMessage().deserialize("<aqua>» [1] Kotak Masuk (Inbox)</aqua>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    Bukkit.getScheduler().runTask(plugin, () -> openMailListJava(p, false));
                }
            }));
        bodies.add(DialogBody.plainMessage(inboxLink));

        Component archiveLink = MiniMessage.miniMessage().deserialize("<gray>» [2] Kotak Arsip (Archive)</gray>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    Bukkit.getScheduler().runTask(plugin, () -> openMailListJava(p, true));
                }
            }));
        bodies.add(DialogBody.plainMessage(archiveLink));

        Component sendLink = MiniMessage.miniMessage().deserialize("<green>» [3] Tulis Surat Baru</green>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    Bukkit.getScheduler().runTask(plugin, () -> openSendMailFormJava(p, null));
                }
            }));
        bodies.add(DialogBody.plainMessage(sendLink));

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Mail System"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(ActionButton.builder(Component.text("Tutup"))
                .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build()))
        );
        player.showDialog(dialog);
    }

    private void openMailListJava(Player player, boolean archived) {
        UUID uuid = player.getUniqueId();
        List<Map<String, Object>> mails = plugin.getDatabaseManager().getMails(uuid, archived);

        List<DialogBody> bodies = new ArrayList<>();
        String title = archived ? "Kotak Arsip" : "Kotak Masuk (Inbox)";
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== " + title + " ===</bold></gold>")));

        if (mails.isEmpty()) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>Tidak ada surat dalam kotak ini.</gray>")));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        } else {
            for (Map<String, Object> mail : mails) {
                int parentId = (int) mail.get("parent_id");
                String senderName = (String) mail.get("sender_name");
                String subject = (String) mail.get("subject");
                Date sentAt = (Date) mail.get("sent_at");
                int isRead = (int) mail.get("is_read");
                String readPrefix = isRead == 0 ? "<red><bold>[Baru] </bold></red>" : "";

                Component mailLink = MiniMessage.miniMessage().deserialize(
                    "• " + readPrefix + "<yellow>" + senderName + "</yellow>: <white>" + subject + "</white> <gray>(" + dateFormat.format(sentAt) + ")</gray> <green>[Buka]</green>"
                ).clickEvent(ClickEvent.callback(aud -> {
                    if (aud instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openMailDetailJava(p, parentId, archived));
                    }
                }));
                bodies.add(DialogBody.plainMessage(mailLink));
            }
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        }

        Component backLink = MiniMessage.miniMessage().deserialize("<gray>[Kembali ke Menu Utama]</gray>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    Bukkit.getScheduler().runTask(plugin, () -> openMailMenuJava(p));
                }
            }));
        bodies.add(DialogBody.plainMessage(backLink));

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(title))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(ActionButton.builder(Component.text("Tutup"))
                .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build()))
        );
        player.showDialog(dialog);
    }

    private void openMailDetailJava(Player player, int parentId, boolean fromArchive) {
        List<Map<String, Object>> thread = plugin.getDatabaseManager().getMailThread(parentId);
        if (thread.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Surat tidak ditemukan.</red>"));
            openMailListJava(player, fromArchive);
            return;
        }

        // Mark all as read
        for (Map<String, Object> msg : thread) {
            int msgId = (int) msg.get("id");
            if ((int) msg.get("is_read") == 0) {
                plugin.getDatabaseManager().setMailReadStatus(msgId, true);
            }
        }

        Map<String, Object> rootMail = thread.get(0);
        String subject = (String) rootMail.get("subject");

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Detail Surat: " + subject + " ===</bold></gold>")));

        // Display Thread Chronologically
        for (int i = 0; i < thread.size(); i++) {
            Map<String, Object> msg = thread.get(i);
            String sender = (String) msg.get("sender_name");
            String body = (String) msg.get("body");
            Date sent = (Date) msg.get("sent_at");

            if (i > 0) {
                bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gray>------------------------------------------</gray>")));
            }
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Dari:</yellow> <white>" + sender + "</white> <gray>(" + dateFormat.format(sent) + ")</gray>")));
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<white>" + body + "</white>")));
        }
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        // Action Buttons
        Component replyLink = MiniMessage.miniMessage().deserialize("<green>[Tulis Balasan]</green>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    Bukkit.getScheduler().runTask(plugin, () -> openReplyMailFormJava(p, parentId, subject, null));
                }
            }));
        bodies.add(DialogBody.plainMessage(replyLink));

        String archiveLabel = fromArchive ? "[Pindahkan ke Inbox]" : "[Arsipkan]";
        Component archiveLink = MiniMessage.miniMessage().deserialize("<gray>" + archiveLabel + "</gray>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    plugin.getDatabaseManager().setMailArchivedStatus(parentId, !fromArchive);
                    String msgText = fromArchive ? "<green>Surat dipindahkan ke Kotak Masuk.</green>" : "<green>Surat berhasil diarsipkan.</green>";
                    p.sendMessage(MiniMessage.miniMessage().deserialize(msgText));
                    Bukkit.getScheduler().runTask(plugin, () -> openMailListJava(p, fromArchive));
                }
            }));
        bodies.add(DialogBody.plainMessage(archiveLink));

        Component deleteLink = MiniMessage.miniMessage().deserialize("<red>[Hapus Percakapan]</red>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    plugin.getDatabaseManager().deleteMailThread(parentId);
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Utas percakapan berhasil dihapus.</green>"));
                    Bukkit.getScheduler().runTask(plugin, () -> openMailListJava(p, fromArchive));
                }
            }));
        bodies.add(DialogBody.plainMessage(deleteLink));

        Component backLink = MiniMessage.miniMessage().deserialize("<gray>[Kembali ke Daftar Surat]</gray>")
            .clickEvent(ClickEvent.callback(aud -> {
                if (aud instanceof Player p) {
                    Bukkit.getScheduler().runTask(plugin, () -> openMailListJava(p, fromArchive));
                }
            }));
        bodies.add(DialogBody.plainMessage(backLink));

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Detail Surat"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(ActionButton.builder(Component.text("Tutup"))
                .action(DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build()))
                .build()))
        );
        player.showDialog(dialog);
    }

    private void openSendMailFormJava(Player player, String errorMsg) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Tulis Surat Baru ===</bold></gold>")));
        if (errorMsg != null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red>[!] Error: " + errorMsg + "</red>")));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        }

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(DialogInput.text("recipient", Component.text("Penerima (Nama Murid/Staff)")).width(340).build());
        inputs.add(DialogInput.text("subject", Component.text("Subjek")).width(340).build());
        inputs.add(DialogInput.text("body", Component.text("Isi Pesan")).width(340).build());

        ActionButton sendBtn = ActionButton.builder(Component.text("Kirim"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    String recipientInput = view.getText("recipient");
                    String subject = view.getText("subject");
                    String body = view.getText("body");

                    if (recipientInput == null || recipientInput.trim().isEmpty() ||
                        subject == null || subject.trim().isEmpty() ||
                        body == null || body.trim().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> openSendMailFormJava(p, "Seluruh kolom isian wajib diisi!"));
                        return;
                    }

                    List<Map<String, String>> matches = plugin.getDatabaseManager().searchStudents(recipientInput.trim());
                    Bukkit.getScheduler().runTask(plugin, () -> handleRecipientMatchesJava(p, matches, recipientInput.trim(), subject.trim(), body.trim()));
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Tulis Surat"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(inputs)
                .build())
            .type(DialogType.notice(sendBtn))
        );
        player.showDialog(dialog);
    }

    private void handleRecipientMatchesJava(Player player, List<Map<String, String>> matches, String query, String subject, String body) {
        if (matches.isEmpty()) {
            openSendMailFormJava(player, "Pemain '" + query + "' tidak ditemukan dalam database.");
            return;
        }

        if (matches.size() == 1) {
            Map<String, String> match = matches.get(0);
            UUID recipientUuid = UUID.fromString(match.get("uuid"));
            plugin.getMailManager().sendMail(0, player.getUniqueId(), player.getName(), recipientUuid.toString(), "PLAYER", "PERSONAL", subject, body);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Surat berhasil dikirim ke <yellow>" + match.get("username") + "</yellow>.</green>"));
            openMailMenuJava(player);
            return;
        }

        // Multiple Matches
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold>=== Pilih Penerima yang Tepat ===</gold>")));
        bodies.add(DialogBody.plainMessage(Component.text("Ditemukan beberapa murid yang mirip. Silakan pilih salah satu untuk mengirim:")));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        for (Map<String, String> student : matches) {
            UUID uuid = UUID.fromString(student.get("uuid"));
            String name = student.get("username");
            String nis = student.get("nis");
            String nisDisplay = nis != null ? nis : "Tidak Ada NIS";

            Component studentLink = MiniMessage.miniMessage().deserialize("<yellow>»</yellow> <white>" + name + "</white> <gray>(" + nisDisplay + ")</gray> <green>[Kirim]</green>")
                .clickEvent(ClickEvent.callback(aud -> {
                    if (aud instanceof Player p) {
                        plugin.getMailManager().sendMail(0, p.getUniqueId(), p.getName(), uuid.toString(), "PLAYER", "PERSONAL", subject, body);
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Surat berhasil dikirim ke <yellow>" + name + "</yellow>.</green>"));
                        Bukkit.getScheduler().runTask(plugin, () -> openMailMenuJava(p));
                    }
                }));
            bodies.add(DialogBody.plainMessage(studentLink));
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Pilih Penerima"))
                .canCloseWithEscape(true)
                .body(bodies)
                .build())
            .type(DialogType.notice(ActionButton.builder(Component.text("Batal"))
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player p) {
                        Bukkit.getScheduler().runTask(plugin, () -> openSendMailFormJava(p, null));
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build()))
        );
        player.showDialog(dialog);
    }

    private void openReplyMailFormJava(Player player, int parentId, String origSubject, String errorMsg) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<gold><bold>=== Balas Surat ===</bold></gold>")));
        bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<yellow>Subjek:</yellow> <white>Re: " + origSubject + "</white>")));
        bodies.add(DialogBody.plainMessage(Component.text(" ")));

        if (errorMsg != null) {
            bodies.add(DialogBody.plainMessage(MiniMessage.miniMessage().deserialize("<red>[!] Error: " + errorMsg + "</red>")));
            bodies.add(DialogBody.plainMessage(Component.text(" ")));
        }

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(DialogInput.text("body", Component.text("Isi Balasan")).width(340).build());

        ActionButton sendBtn = ActionButton.builder(Component.text("Kirim Balasan"))
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player p) {
                    String body = view.getText("body");
                    if (body == null || body.trim().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> openReplyMailFormJava(p, parentId, origSubject, "Isi balasan tidak boleh kosong!"));
                        return;
                    }

                    // Find recipient UUID by looking at the thread
                    List<Map<String, Object>> thread = plugin.getDatabaseManager().getMailThread(parentId);
                    if (thread.isEmpty()) {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Utas surat tidak ditemukan.</red>"));
                        return;
                    }

                    Map<String, Object> rootMail = thread.get(0);
                    String originalSenderUuid = (String) rootMail.get("sender_uuid");
                    String originalRecipientUuid = (String) rootMail.get("recipient_uuid");

                    // The recipient of the reply is the other participant in the conversation thread
                    String recipientUuidStr = p.getUniqueId().toString().equals(originalSenderUuid) ? originalRecipientUuid : originalSenderUuid;

                    plugin.getMailManager().sendMail(parentId, p.getUniqueId(), p.getName(), recipientUuidStr, "PLAYER", "PERSONAL", "Re: " + origSubject, body.trim());
                    p.sendMessage(MiniMessage.miniMessage().deserialize("<green>Balasan berhasil dikirim!</green>"));
                    
                    Bukkit.getScheduler().runTask(plugin, () -> openMailDetailJava(p, parentId, false));
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Tulis Balasan"))
                .canCloseWithEscape(true)
                .body(bodies)
                .inputs(inputs)
                .build())
            .type(DialogType.notice(sendBtn))
        );
        player.showDialog(dialog);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEDROCK EDITION (CUMULUS FORMS)
    // ─────────────────────────────────────────────────────────────────────────

    private void openMailMenuBedrock(Player player) {
        UUID uuid = player.getUniqueId();
        int unread = plugin.getDatabaseManager().getUnreadMailCount(uuid);
        int totalSent = plugin.getDatabaseManager().getTotalSentMailCount(uuid);

        SimpleForm form = SimpleForm.builder()
            .title("Mail System")
            .content("=== Informasi Surat ===\n" +
                     "Pesan Belum Dibaca: " + unread + "\n" +
                     "Total Surat Terkirim: " + totalSent)
            .button("Kotak Masuk (Inbox)")
            .button("Kotak Arsip (Archive)")
            .button("Tulis Surat Baru")
            .button("Tutup")
            .validResultHandler(response -> {
                int id = response.clickedButtonId();
                if (id == 0) {
                    openMailListBedrock(player, false);
                } else if (id == 1) {
                    openMailListBedrock(player, true);
                } else if (id == 2) {
                    openSendMailFormBedrock(player, null);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openMailListBedrock(Player player, boolean archived) {
        UUID uuid = player.getUniqueId();
        List<Map<String, Object>> mails = plugin.getDatabaseManager().getMails(uuid, archived);
        String title = archived ? "Kotak Arsip" : "Kotak Masuk";

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(mails.isEmpty() ? "Tidak ada surat di kotak ini." : "Silakan pilih surat untuk membukanya:");

        builder.button("Kembali ke Menu Utama");
        for (Map<String, Object> mail : mails) {
            String sender = (String) mail.get("sender_name");
            String subject = (String) mail.get("subject");
            Date sent = (Date) mail.get("sent_at");
            int isRead = (int) mail.get("is_read");
            String newLabel = isRead == 0 ? "[BARU] " : "";

            builder.button(newLabel + sender + "\n" + subject + " (" + dateFormat.format(sent) + ")");
        }

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked == 0) {
                openMailMenuBedrock(player);
            } else {
                int index = clicked - 1;
                if (index >= 0 && index < mails.size()) {
                    int parentId = (int) mails.get(index).get("parent_id");
                    openMailDetailBedrock(player, parentId, archived);
                }
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    private void openMailDetailBedrock(Player player, int parentId, boolean fromArchive) {
        List<Map<String, Object>> thread = plugin.getDatabaseManager().getMailThread(parentId);
        if (thread.isEmpty()) {
            player.sendMessage("§cSurat tidak ditemukan.");
            openMailListBedrock(player, fromArchive);
            return;
        }

        // Mark as read
        for (Map<String, Object> msg : thread) {
            int msgId = (int) msg.get("id");
            if ((int) msg.get("is_read") == 0) {
                plugin.getDatabaseManager().setMailReadStatus(msgId, true);
            }
        }

        Map<String, Object> rootMail = thread.get(0);
        String subject = (String) rootMail.get("subject");

        StringBuilder sb = new StringBuilder();
        sb.append("=== Detail Surat: ").append(subject).append(" ===\n\n");
        for (int i = 0; i < thread.size(); i++) {
            Map<String, Object> msg = thread.get(i);
            String sender = (String) msg.get("sender_name");
            String body = (String) msg.get("body");
            Date sent = (Date) msg.get("sent_at");

            if (i > 0) {
                sb.append("\n------------------------------------------\n");
            }
            sb.append("Dari: ").append(sender).append(" (").append(dateFormat.format(sent)).append(")\n")
              .append(body).append("\n");
        }

        String archiveLabel = fromArchive ? "Pindahkan ke Inbox" : "Arsipkan";

        SimpleForm form = SimpleForm.builder()
            .title("Detail Surat")
            .content(sb.toString())
            .button("Balas Surat")
            .button(archiveLabel)
            .button("Hapus Percakapan")
            .button("Kembali ke Daftar")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                if (clicked == 0) {
                    openReplyMailFormBedrock(player, parentId, subject, null);
                } else if (clicked == 1) {
                    plugin.getDatabaseManager().setMailArchivedStatus(parentId, !fromArchive);
                    player.sendMessage(fromArchive ? "§aSurat dipindahkan ke Kotak Masuk." : "§aSurat berhasil diarsipkan.");
                    openMailListBedrock(player, fromArchive);
                } else if (clicked == 2) {
                    plugin.getDatabaseManager().deleteMailThread(parentId);
                    player.sendMessage("§aUtas percakapan berhasil dihapus.");
                    openMailListBedrock(player, fromArchive);
                } else {
                    openMailListBedrock(player, fromArchive);
                }
            })
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void openSendMailFormBedrock(Player player, String errorMsg) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("Tulis Surat Baru");

        if (errorMsg != null) {
            builder.label("§cError: " + errorMsg);
        }

        builder.input("Penerima (Nama Murid/Staff)", "")
            .input("Subjek", "")
            .input("Isi Pesan", "")
            .validResultHandler(response -> {
                int offset = errorMsg != null ? 1 : 0;
                String recipientInput = response.asInput(offset);
                String subject = response.asInput(offset + 1);
                String body = response.asInput(offset + 2);

                if (recipientInput == null || recipientInput.trim().isEmpty() ||
                    subject == null || subject.trim().isEmpty() ||
                    body == null || body.trim().isEmpty()) {
                    openSendMailFormBedrock(player, "Seluruh kolom isian wajib diisi!");
                    return;
                }

                List<Map<String, String>> matches = plugin.getDatabaseManager().searchStudents(recipientInput.trim());
                handleRecipientMatchesBedrock(player, matches, recipientInput.trim(), subject.trim(), body.trim());
            });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    private void handleRecipientMatchesBedrock(Player player, List<Map<String, String>> matches, String query, String subject, String body) {
        if (matches.isEmpty()) {
            openSendMailFormBedrock(player, "Pemain '" + query + "' tidak ditemukan dalam database.");
            return;
        }

        if (matches.size() == 1) {
            Map<String, String> match = matches.get(0);
            UUID recipientUuid = UUID.fromString(match.get("uuid"));
            plugin.getMailManager().sendMail(0, player.getUniqueId(), player.getName(), recipientUuid.toString(), "PLAYER", "PERSONAL", subject, body);
            player.sendMessage("§aSurat berhasil dikirim ke §e" + match.get("username") + "§a.");
            openMailMenuBedrock(player);
            return;
        }

        // Multiple matches
        SimpleForm.Builder selectBuilder = SimpleForm.builder()
            .title("Pilih Penerima")
            .content("Ditemukan beberapa murid. Pilih salah satu untuk mengirim:");

        for (Map<String, String> student : matches) {
            String name = student.get("username");
            String nis = student.get("nis");
            String nisDisplay = nis != null ? nis : "Tidak Ada NIS";
            selectBuilder.button(name + "\n(" + nisDisplay + ")");
        }

        selectBuilder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx >= 0 && idx < matches.size()) {
                Map<String, String> match = matches.get(idx);
                UUID uuid = UUID.fromString(match.get("uuid"));
                plugin.getMailManager().sendMail(0, player.getUniqueId(), player.getName(), uuid.toString(), "PLAYER", "PERSONAL", subject, body);
                player.sendMessage("§aSurat berhasil dikirim ke §e" + match.get("username") + "§a.");
                openMailMenuBedrock(player);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), selectBuilder.build());
    }

    private void openReplyMailFormBedrock(Player player, int parentId, String origSubject, String errorMsg) {
        CustomForm.Builder builder = CustomForm.builder()
            .title("Balas Surat")
            .label("Subjek: Re: " + origSubject);

        if (errorMsg != null) {
            builder.label("§cError: " + errorMsg);
        }

        int textOffset = errorMsg != null ? 2 : 1;

        builder.input("Isi Balasan", "")
            .validResultHandler(response -> {
                String body = response.asInput(textOffset);
                if (body == null || body.trim().isEmpty()) {
                    openReplyMailFormBedrock(player, parentId, origSubject, "Isi balasan tidak boleh kosong!");
                    return;
                }

                List<Map<String, Object>> thread = plugin.getDatabaseManager().getMailThread(parentId);
                if (thread.isEmpty()) {
                    player.sendMessage("§cUtas surat tidak ditemukan.");
                    return;
                }

                Map<String, Object> rootMail = thread.get(0);
                String originalSenderUuid = (String) rootMail.get("sender_uuid");
                String originalRecipientUuid = (String) rootMail.get("recipient_uuid");

                String recipientUuidStr = player.getUniqueId().toString().equals(originalSenderUuid) ? originalRecipientUuid : originalSenderUuid;

                plugin.getMailManager().sendMail(parentId, player.getUniqueId(), player.getName(), recipientUuidStr, "PLAYER", "PERSONAL", "Re: " + origSubject, body.trim());
                player.sendMessage("§aBalasan berhasil dikirim!");
                
                openMailDetailBedrock(player, parentId, false);
            });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }
}
