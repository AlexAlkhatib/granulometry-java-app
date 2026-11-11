package fr.granulometrie.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatisticsUtilsTest {

    @Test
    void percentileInterpolatesLinearly() {
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0);

        assertEquals(10.0, StatisticsUtils.percentile(values, 0), 1e-9);
        assertEquals(25.0, StatisticsUtils.percentile(values, 50), 1e-9);
        assertEquals(40.0, StatisticsUtils.percentile(values, 100), 1e-9);
        assertEquals(17.5, StatisticsUtils.percentile(values, 25), 1e-9);
    }

    @Test
    void meanAndStandardDeviationAreCorrect() {
        List<Double> values = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);

        assertEquals(5.0, StatisticsUtils.mean(values), 1e-9);
        assertEquals(2.13809, StatisticsUtils.standardDeviation(values), 1e-4);
        assertEquals(4.5, StatisticsUtils.median(values), 1e-9);
    }

    @Test
    void emptyListGivesZeros() {
        assertEquals(0.0, StatisticsUtils.mean(List.of()), 1e-9);
        assertEquals(0.0, StatisticsUtils.percentile(List.of(), 50), 1e-9);
        assertEquals(0.0, StatisticsUtils.standardDeviation(List.of()), 1e-9);
    }

    @Test
    void singleValueIsItsOwnPercentile() {
        assertEquals(7.0, StatisticsUtils.percentile(List.of(7.0), 90), 1e-9);
    }
}
