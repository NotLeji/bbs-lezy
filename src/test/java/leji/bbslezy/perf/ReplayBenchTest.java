package leji.bbslezy.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayBenchTest
{
    @Test
    public void parseValidSpec()
    {
        ReplayBench.Config config = ReplayBench.parse("myFilm,100,600,true");

        assertEquals("myFilm", config.filmId());
        assertEquals(100, config.warmupTicks());
        assertEquals(600, config.sampleTicks());
        assertTrue(config.withCamera());
    }

    @Test
    public void parseWithoutCamera()
    {
        assertFalse(ReplayBench.parse("myFilm,100,600,false").withCamera());
    }

    @Test
    public void parseInvalidSpecs()
    {
        assertNull(ReplayBench.parse("myFilm"));
        assertNull(ReplayBench.parse("myFilm,x,y,z"));
        assertNull(ReplayBench.parse("myFilm,-1,600,true"));
        assertNull(ReplayBench.parse(null));
    }
}
