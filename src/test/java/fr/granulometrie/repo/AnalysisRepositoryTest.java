package fr.granulometrie.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.granulometrie.core.AnalysisParameters;
import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.core.ImageAnalyzer;
import fr.granulometrie.core.Particle;
import fr.granulometrie.core.TestImages;

class AnalysisRepositoryTest {

    @Test
    void savesListsLoadsAndDeletesSessions(@TempDir Path directory) {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setPixelSizeMicrons(1.5);
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);

        try (AnalysisRepository repository = new AnalysisRepository(directory.resolve("granulo-test"))) {
            assertEquals(0, repository.count());

            long id = repository.save(result, "essai automatisé");
            assertEquals(1, repository.count());

            List<StoredSession> sessions = repository.listSessions();
            assertEquals(1, sessions.size());
            StoredSession summary = sessions.get(0);
            assertEquals(id, summary.id());
            assertEquals("essai automatisé", summary.comment());
            assertEquals(result.getStatistics().count(), summary.particleCount());
            assertEquals(1.5, summary.parameters().getPixelSizeMicrons(), 1e-9);

            StoredSession loaded = repository.loadSession(id);
            assertNotNull(loaded);
            assertNotNull(loaded.image());
            assertNotNull(loaded.particles());
            assertEquals(result.getParticles().size(), loaded.particles().size());

            Particle first = loaded.particles().get(0);
            assertEquals(result.getParticles().get(0).area(), first.area(), 1e-9);
            assertEquals(result.getParticles().get(0).circularity(), first.circularity(), 1e-9);

            repository.deleteSession(id);
            assertEquals(0, repository.count());
            assertNull(repository.loadSession(id));
        }
    }

    @Test
    void sessionsAreOrderedFromNewestToOldest(@TempDir Path directory) {
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, new AnalysisParameters());
        try (AnalysisRepository repository = new AnalysisRepository(directory.resolve("ordre"))) {
            long first = repository.save(result, "première");
            long second = repository.save(result, "seconde");

            List<StoredSession> sessions = repository.listSessions();
            assertEquals(2, sessions.size());
            assertEquals(second, sessions.get(0).id());
            assertEquals(first, sessions.get(1).id());
            assertTrue(sessions.get(0).createdAt().isAfter(sessions.get(1).createdAt())
                    || sessions.get(0).createdAt().isEqual(sessions.get(1).createdAt()));
        }
    }
}
