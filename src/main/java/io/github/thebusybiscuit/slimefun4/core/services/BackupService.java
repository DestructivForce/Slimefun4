package io.github.thebusybiscuit.slimefun4.core.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.mrCookieSlime.Slimefun.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.api.Slimefun;

/**
 * This Service creates a Backup of your Slimefun world data on every server shutdown.
 * 
 * @author TheBusyBiscuit
 *
 */
public class BackupService implements Runnable {

    private static final int MAX_BACKUPS = 20;

    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ROOT);
    private final File directory = new File("data-storage/Slimefun/block-backups");

    @Override
    public void run() {
        List<File> backups = Arrays.asList(directory.listFiles());

        if (backups.size() > MAX_BACKUPS) {
            try {
                deleteOldBackups(backups);
            }
            catch (IOException e) {
                Slimefun.getLogger().log(Level.WARNING, "Could not delete an old backup", e);
            }
        }

        File file = new File(directory, format.format(LocalDateTime.now()) + ".zip");

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file))) {
                        createBackup(output);
                    }

                    Slimefun.getLogger().log(Level.INFO, "Backed up Slimefun data to: {0}", file.getName());
                }
                else {
                    Slimefun.getLogger().log(Level.WARNING, "Could not create backup-file: {0}", file.getName());
                }
            }
            catch (IOException x) {
                Slimefun.getLogger().log(Level.SEVERE, x, () -> "An Error occurred while creating a backup for Slimefun " + SlimefunPlugin.getVersion());
            }
        }
    }

    private void createBackup(ZipOutputStream output) throws IOException {

        for (File folder : new File("data-storage/Slimefun/stored-blocks/").listFiles()) {
            addDirectory(output, folder, "stored-blocks/" + folder.getName());
        }

        addDirectory(output, new File("data-storage/Slimefun/universal-inventories/"), "universal-inventories");
        addDirectory(output, new File("data-storage/Slimefun/stored-inventories/"), "stored-inventories");

        File chunks = new File("data-storage/Slimefun/stored-chunks/chunks.sfc");

        if (chunks.exists()) {
            byte[] buffer = new byte[1024];
            ZipEntry entry = new ZipEntry("stored-chunks/chunks.sfc");
            output.putNextEntry(entry);

            try (FileInputStream input = new FileInputStream(chunks)) {
                int length;

                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }

            output.closeEntry();
        }
    }

    private void addDirectory(ZipOutputStream output, File directory, String zipPath) throws IOException {
        byte[] buffer = new byte[1024];

        for (File file : directory.listFiles()) {
            ZipEntry entry = new ZipEntry(zipPath + '/' + file.getName());
            output.putNextEntry(entry);

            try (FileInputStream input = new FileInputStream(file)) {
                int length;

                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }

            output.closeEntry();
        }
    }

    private void deleteOldBackups(List<File> backups) throws IOException {
        Collections.sort(backups, (a, b) -> {
            LocalDateTime time1 = LocalDateTime.parse(a.getName().substring(0, a.getName().length() - 4), format);
            LocalDateTime time2 = LocalDateTime.parse(b.getName().substring(0, b.getName().length() - 4), format);

            return time2.compareTo(time1);
        });

        for (int i = backups.size() - MAX_BACKUPS; i > 0; i--) {
            Files.delete(backups.get(i).toPath());
        }
    }

}
