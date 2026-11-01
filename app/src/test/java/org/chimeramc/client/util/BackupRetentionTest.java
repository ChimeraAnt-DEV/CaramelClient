package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The per-instance backup retention rule (item 144): keep the newest N, never delete the only copy,
 * and only ever consider files this class owns.
 */
public class BackupRetentionTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BackupRetention.Candidate candidate(String name, String instance, long createdAt)
            throws Exception {
        File file = folder.newFile(name);
        return new BackupRetention.Candidate(file, instance, createdAt);
    }

    @Test
    public void keepsTheNewestNPerInstance() throws Exception {
        List<BackupRetention.Candidate> list = new ArrayList<>();
        list.add(candidate("chimeralauncher_instance_A_1.chimerabackup", "A", 100));
        list.add(candidate("chimeralauncher_instance_A_2.chimerabackup", "A", 300));
        list.add(candidate("chimeralauncher_instance_A_3.chimerabackup", "A", 200));

        List<File> stale = BackupRetention.selectForDeletion(list, 2);

        assertEquals(1, stale.size());
        assertTrue(stale.get(0).getName().endsWith("_1.chimerabackup"));
    }

    @Test
    public void retentionIsPerInstanceNotGlobal() throws Exception {
        List<BackupRetention.Candidate> list = new ArrayList<>();
        list.add(candidate("chimeralauncher_instance_A_1.chimerabackup", "A", 100));
        list.add(candidate("chimeralauncher_instance_B_1.chimerabackup", "B", 100));
        list.add(candidate("chimeralauncher_instance_B_2.chimerabackup", "B", 200));

        // Keep 1: each instance keeps its newest, so only A's single and B's older go.
        List<File> stale = BackupRetention.selectForDeletion(list, 1);
        assertEquals(1, stale.size());
        assertTrue(stale.get(0).getName().contains("_B_1"));
    }

    @Test
    public void aZeroOrNegativeRetentionFallsBackToTheDefaultAndNeverDeletesTheOnlyCopy()
            throws Exception {
        // One backup and a nonsensical retention: the only copy must survive.
        List<BackupRetention.Candidate> single = new ArrayList<>();
        single.add(candidate("chimeralauncher_instance_single.chimerabackup", "S", 100));
        assertTrue(BackupRetention.selectForDeletion(single, 0).isEmpty());
        assertTrue(BackupRetention.selectForDeletion(single, -9).isEmpty());

        // A negative value is read as the default (5), so six backups leave the newest five.
        List<BackupRetention.Candidate> many = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            many.add(candidate("chimeralauncher_instance_many_" + i + ".chimerabackup", "M", i * 100L));
        }
        List<File> stale = BackupRetention.selectForDeletion(many, -1);
        assertEquals(1, stale.size());
        assertFalse(stale.get(0).getName().endsWith("_6.chimerabackup"));
    }

    @Test
    public void aTieOnTimestampFallsBackToTheNameSoTheResultIsDeterministic() throws Exception {
        List<BackupRetention.Candidate> list = new ArrayList<>();
        list.add(candidate("chimeralauncher_instance_A_aaa.chimerabackup", "A", 100));
        list.add(candidate("chimeralauncher_instance_A_bbb.chimerabackup", "A", 100));

        List<File> stale = BackupRetention.selectForDeletion(list, 1);
        assertEquals(1, stale.size());
        // Newest-first ties resolve on the descending name, so "bbb" is kept and "aaa" is removed.
        assertTrue(stale.get(0).getName().endsWith("_aaa.chimerabackup"));
    }

    @Test
    public void keepIsClampedAndJunkParsesToTheDefault() {
        assertEquals(BackupRetention.DEFAULT_KEEP, BackupRetention.clampKeep(0));
        assertEquals(BackupRetention.DEFAULT_KEEP, BackupRetention.clampKeep(-5));
        assertEquals(BackupRetention.MAX_KEEP, BackupRetention.clampKeep(10_000));
        assertEquals(3, BackupRetention.clampKeep(3));
        assertEquals(BackupRetention.DEFAULT_KEEP, BackupRetention.parseKeep(null));
        assertEquals(BackupRetention.DEFAULT_KEEP, BackupRetention.parseKeep("nonsense"));
        assertEquals(4, BackupRetention.parseKeep("4"));
        assertEquals(BackupRetention.MAX_KEEP, BackupRetention.parseKeep("9999"));
    }

    @Test
    public void onlyManagedBackupNamesAreConsidered() {
        assertTrue(BackupRetention.isManagedBackupName(
                "chimeralauncher_instance_A_20260101_120000.chimerabackup"));
        assertFalse(BackupRetention.isManagedBackupName("world.mcworld"));
        assertFalse(BackupRetention.isManagedBackupName("some_other.chimerabackup"));
        assertFalse(BackupRetention.isManagedBackupName(null));
    }

    @Test
    public void anEmptyOrNullListDeletesNothing() {
        assertTrue(BackupRetention.selectForDeletion(null, 5).isEmpty());
        assertTrue(BackupRetention.selectForDeletion(new ArrayList<>(), 5).isEmpty());
    }
}
